package typeprog.ch06typeclassesgivens

import typeprog.core.evidence

/** Chapter 06 — type classes, givens & implicit search.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The chapter covers six things, in order:
  *
  *   1. a type class — behaviour for a type, declared outside it;
  *   2. conditional instances — rules the search applies recursively;
  *   3. where the compiler looks — the lexical scope, then the implicit scope;
  *   4. when two match — the tie-breakers, and the ambiguity when none apply;
  *   5. a given that computes a type;
  *   6. chapter 01's evidence, seen again as ordinary givens.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch06-type-classes-givens.md`.
  *
  * (Messages quoted below drop the `typeprog.ch06typeclassesgivens.` prefix the
  * compiler prints in front of every name.)
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. A type class
  //
  // `Pretty[A]` is what a type must be able to do. Nothing extends it: `Int`
  // gets an instance without `Int` knowing, which a bound `A <: Pretty` could
  // never have given it.
  // ---------------------------------------------------------------------------

  trait Pretty[A]:
    def pretty(a: A): String

    /** Syntax. Found through a *visible* instance — see section 3. */
    extension (a: A) def prettied: String = pretty(a)

  object Pretty:
    // A single-method trait can be given as a lambda.
    given Pretty[Int] = i => s"#$i"
    given Pretty[Boolean] = b => if b then "yes" else "no"

  // A `using` parameter is one the compiler passes, found by its type.
  def render[A](a: A)(using p: Pretty[A]): String = p.pretty(a)

  val renderedInt: String = render(42) // "#42": A = Int, then a Pretty[Int]

  // The same demand as a context bound, named with `as`. Inside, the instance
  // is a parameter, so it is visible — and the extension in the trait works.
  def renderTwice[A: Pretty as p](a: A): String = p.pretty(a) + " " + a.prettied

  // `summon` is the search with nothing around it.
  val intInstance: Pretty[Int] = summon[Pretty[Int]]

  // Outside such a method the companion's instances are *not* visible — they
  // are only in the implicit scope — so the syntax is not found:
  //
  //   42.prettied
  //   // value prettied is not a member of Int, but could be made available as
  //   // an extension method.
  //   //
  //   // The following import might fix the problem:
  //   //
  //   //   import Walkthrough.Pretty.given_Pretty_Int

  final class Widget

  //   render(Widget())
  //   // No given instance of type Walkthrough.Pretty[Walkthrough.Widget] was
  //   // found for parameter p of method render in object Walkthrough

  // ---------------------------------------------------------------------------
  // 2. Conditional instances
  //
  // An instance with a premise is a rule. The search applies rules until it
  // reaches instances that need nothing, and builds the result at compile time.
  // ---------------------------------------------------------------------------

  trait Size[A]:
    def size(a: A): Int

  object Size:
    given Size[String] = _.length
    given Size[Int] = _ => 1

    /** If an `A` has a size, a `Vector[A]` has one: the sum. */
    given vector: [A: Size as element] => Size[Vector[A]] = _.map(element.size).sum

    /** Two premises: both halves need a size. */
    given pair: [A: Size as first, B: Size as second] => Size[(A, B)] =
      (a, b) => first.size(a) + second.size(b)

  // Nobody wrote an instance for this type. The search assembles it from four:
  // `vector`, `pair`, and the ones for `String` and `Int`.
  val nested: Int = summon[Size[Vector[(String, Int)]]].size(Vector(("ab", 7), ("c", 8))) // 5

  // When a premise fails, the message shows how far the search got:
  //
  //   summon[Size[Vector[Widget]]]
  //   // No given instance of type Walkthrough.Size[Vector[Walkthrough.Widget]]
  //   // was found for parameter x of method summon in object Predef.
  //   // I found:
  //   //
  //   //     Walkthrough.Size.vector[Walkthrough.Widget](
  //   //       /* missing */
  //   //         summon[Walkthrough.Size[Walkthrough.Widget]]
  //   //     )
  //   //
  //   // But no implicit values were found that match type
  //   // Walkthrough.Size[Walkthrough.Widget].

  // ---------------------------------------------------------------------------
  // 3. Where the compiler looks
  //
  // First the lexical scope: what is defined in enclosing scopes, and what is
  // imported. Then — only if that found nothing — the implicit scope of the
  // type: the companions of the type and of each of its parts.
  // ---------------------------------------------------------------------------

  final case class Celsius(degrees: Double)

  object Celsius:
    // `Pretty` is not ours to edit, `Celsius` is: its companion is in the
    // implicit scope of `Pretty[Celsius]`, so this is found with no import.
    given Pretty[Celsius] = c => s"${c.degrees}°C"

  val temperature: String = render(Celsius(21.5)) // "21.5°C"

  // An instance for a type class and a type the author owns neither of — an
  // orphan — has no companion to live in. It lives in an object, and has to be
  // imported, with `given`: a wildcard does not bring givens in.
  object Orphans:
    given Pretty[String] = s => s"'$s'"

  //   import Orphans.*
  //   render("hi")
  //   // No given instance of type Walkthrough.Pretty[String] was found for
  //   // parameter p of method render in object Walkthrough
  //   //
  //   // Note: given instance given_Pretty_String in object Orphans was not
  //   // considered because it was not imported with `import given`.

  val orphanRendered: String =
    import Orphans.given
    render("hi") // "'hi'"

  // Because the lexical scope is searched first, an import also *overrides*:
  // here the companion's instance is never consulted.
  object Fahrenheit:
    given Pretty[Celsius] = c => s"${c.degrees * 9 / 5 + 32}°F"

  val inFahrenheit: String =
    import Fahrenheit.given
    render(Celsius(100)) // "212.0°F"

  // Of two lexical candidates, the more deeply nested one wins.
  def nestedWins: String =
    given outer: Pretty[Widget] = _ => "outer"
    def inner =
      given innermost: Pretty[Widget] = _ => "inner"
      render(Widget())
    inner // "inner"

  // ---------------------------------------------------------------------------
  // 4. When two match
  //
  // The types rank candidates first: a rule for a particular type beats a
  // rule for every type.
  // ---------------------------------------------------------------------------

  trait Label[A]:
    def label(a: A): String

  object Label:
    given anything: [A] => Label[A] = a => s"something: $a"
    given Label[Int] = i => s"the number $i"

  val specificWins: String = summon[Label[Int]].label(3) // "the number 3"
  val fallbackUsed: String = summon[Label[Widget]].label(Widget()) // "something: …"

  // Between two plain givens whose types are subtypes of each other, Scala 3.5
  // and later pick the *most general*. Both of these match `Seq[Int]`; the
  // `Seq` one is chosen, where Scala 3.4 chose the `List` one.
  object Defaults:
    given Seq[Int] = Seq(1)
    given List[Int] = List(2)

  val general: Seq[Int] =
    import Defaults.given
    summon[Seq[Int]] // Seq(1)

  // Two rules of the same shape, both matching, with nothing to rank them:
  // `Int` has a `Pretty` and an `Ordering`, so both premises hold.
  trait Describe[A]:
    def describe(a: A): String

  object Tied:
    given byPretty: [A: Pretty] => Describe[A] = a => summon[Pretty[A]].pretty(a)
    given byOrdering: [A: Ordering] => Describe[A] = a => s"<$a>"

  //   import Tied.given
  //   summon[Describe[Int]]
  //   // Ambiguous given instances: both given instance byPretty in object Tied
  //   // and given instance byOrdering in object Tied match type
  //   // Walkthrough.Describe[Int] of parameter x of method summon in object Predef

  // Where a given is *defined* breaks the tie: one in an object wins over one
  // in a trait that object extends. The loser goes into the parent — the
  // low-priority trait.
  trait LowPriorityDescribe:
    given byOrdering: [A: Ordering] => Describe[A] = a => s"<$a>"

  object Ranked extends LowPriorityDescribe:
    given byPretty: [A: Pretty] => Describe[A] = a => summon[Pretty[A]].pretty(a)

  val ranked: (String, String) =
    import Ranked.given
    (summon[Describe[Int]].describe(1), summon[Describe[String]].describe("s")) // ("#1", "<s>")

  // ---------------------------------------------------------------------------
  // 5. A given that computes a type
  //
  // A type member in the type class, fixed by each instance. The result type
  // of `firstOf` is `u.Elem` — chapter 03's dependent method type — so it is
  // whatever the instance found says.
  // ---------------------------------------------------------------------------

  trait Elements[C]:
    type Elem
    def first(c: C): Option[Elem]

  object Elements:
    /** The instance's type must say what `Elem` is; the refinement (chapter 01)
      * does, and `Aux` is the usual name for the alias that writes it.
      */
    type Aux[C, E] = Elements[C] { type Elem = E }

    given listElements: [A] => Aux[List[A], A] = new Elements[List[A]]:
      type Elem = A
      def first(c: List[A]): Option[A] = c.headOption

    given Aux[String, Char] = new Elements[String]:
      type Elem = Char
      def first(c: String): Option[Char] = c.headOption

  def firstOf[C](c: C)(using u: Elements[C]): Option[u.Elem] = u.first(c)

  val firstChar: Option[Char] = firstOf("abc") // a Char, statically
  val firstInt: Option[Int] = firstOf(List(1, 2))

  // The type is printed as the instance's member, and that member is known to
  // be `Char` — which is why the line above compiles and this one does not:
  //
  //   val wrong: Option[Int] = firstOf("abc")
  //   // Found:    Option[Walkthrough.Elements.given_Aux_String_Char.Elem]
  //   // Required: Option[Int]
  //
  // Declared as a plain `Elements[String]`, the instance would still be found,
  // and `Elem` would be known as nothing but itself: not even `firstChar` above
  // would compile.

  // ---------------------------------------------------------------------------
  // 6. Evidence is a type class
  //
  // Chapter 01's `=:=` and `<:<` are type classes whose instances the standard
  // library provides only when the relation holds. Asking for one is an
  // ordinary given search.
  // ---------------------------------------------------------------------------

  val intIsAnyVal: Int <:< AnyVal = summon[Int <:< AnyVal]
  val stringIsString: String =:= String = summon[String =:= String]

  evidence.subtypeOf[Celsius, Product] // a case class is a Product — found by search

  //   summon[String <:< AnyVal]
  //   // Cannot prove that String <:< AnyVal.

end Walkthrough
