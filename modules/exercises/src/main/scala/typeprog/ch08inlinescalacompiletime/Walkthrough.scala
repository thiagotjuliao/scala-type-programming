package typeprog.ch08inlinescalacompiletime

import scala.compiletime.{constValue, erasedValue, error, summonFrom, summonInline}

/** Chapter 08 — `inline` & `scala.compiletime`.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The chapter covers seven things, in order:
  *
  *   1. `inline` methods, `inline` parameters and `inline if`;
  *   2. `inline match` — a branch chosen by static type;
  *   3. `transparent` — the expansion decides the type;
  *   4. `erasedValue` — matching on a type with no value;
  *   5. `constValue` and `error` — a literal type read while compiling;
  *   6. `summonInline` and `summonFrom` — instances looked up at expansion;
  *   7. why `inline` has to go all the way up.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch08-inline-scala-compiletime.md`.
  *
  * (Messages quoted below drop the `typeprog.ch08inlinescalacompiletime.`
  * prefix the compiler prints in front of every name.)
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. Inline methods
  //
  // The body is copied into each call site while that call site is compiled.
  // An `inline` parameter is the caller's expression itself, and an
  // `inline if` keeps only the branch its constant condition selects.
  // ---------------------------------------------------------------------------

  inline def answer(inline flag: Boolean): String =
    inline if flag then "yes" else "no"

  val yes: String = answer(true) // compiles to "yes"

  // The condition has to be known while compiling:
  //
  //   def ask(b: Boolean): String = answer(b)
  //   // Cannot reduce `inline if` because its condition is not a constant value: b

  // ---------------------------------------------------------------------------
  // 2. `inline match`
  //
  // Reduced at the call site, against the static type of the scrutinee. No
  // runtime test is made, so nothing erased can get in the way.
  // ---------------------------------------------------------------------------

  inline def kind[A](a: A): String = inline a match
    case _: Int => "an Int"
    case _: String => "a String"

  val kindOfOne: String = kind(1) // "an Int"
  val kindOfS: String = kind("s") // "a String"

  //   kind(true)
  //   // cannot reduce inline match with
  //   //  scrutinee:  true : (true : Boolean)
  //   //  patterns :  case _:Int
  //   //              case _:String

  // ---------------------------------------------------------------------------
  // 3. `transparent`
  //
  // A transparent call has the type of what it expands to; the declared type
  // is an upper bound.
  // ---------------------------------------------------------------------------

  transparent inline def next[A](a: A): Any = inline a match
    case i: Int => i + 1
    case c: Char => (c + 1).toChar
    case l: Long => l + 1L

  val nextInt: Int = next(41) // 42 — an Int, statically
  val nextChar: Char = next('a') // 'b' — a Char
  def nextOf(i: Int): Int = next(i) // not a constant: still an Int

  // Without `transparent`, the same expansion is typed as declared:
  inline def opaqueNext[A](a: A): Any = inline a match
    case i: Int => i + 1

  //   val n: Int = opaqueNext(41)
  //   // Found:    Any
  //   // Required: Int

  // A precise declared type is the other option, and it works only as far as
  // each branch tells the compiler what the type parameter is. A typed pattern
  // on the parameter does: in `case i: Int`, `A` is an `Int`, so the match
  // type reduces there.
  type Next[A] = A match
    case Int => Int
    case Char => Char

  inline def nextDeclared[A](a: A): Next[A] = inline a match
    case i: Int => i + 1
    case c: Char => (c + 1).toChar

  val declaredNext: Int = nextDeclared(41)

  // A pattern on a pair of parameters does not, and the body is rejected where
  // it is written:
  //
  //   type Sum[A, B] = (A, B) match
  //     case (Int, Int) => Int
  //   inline def sum[A, B](a: A, b: B): Sum[A, B] = inline (a, b) match
  //     case (x: Int, y: Int) => x + y
  //   // Found:    Int
  //   // Required: Sum[A, B]
  //   //
  //   // Note: a match type could not be fully reduced: …

  // ---------------------------------------------------------------------------
  // 4. `erasedValue`
  //
  // A stand-in value of type `T`, never evaluated, for an `inline match` to
  // match on when there is only a type. Patterns can bind type variables.
  // ---------------------------------------------------------------------------

  inline def typeName[T]: String = inline erasedValue[T] match
    case _: Int => "Int"
    case _: String => "String"
    case _: (a, b) => "(" + typeName[a] + ", " + typeName[b] + ")"
    case _ => error("typeName does not know this type")

  val nameOfPair: String = typeName[(Int, (String, Int))] // "(Int, (String, Int))"

  //   typeName[Boolean]
  //   // typeName does not know this type

  // Outside an `inline match` it stands for nothing:
  //
  //   val e = erasedValue[Int]
  //   // method erasedValue is declared as `erased`, but is in fact used

  // ---------------------------------------------------------------------------
  // 5. `constValue` and `error`
  //
  // The value of a literal type, as a constant — so `inline if` can decide on
  // it, and `error` can refuse the call with a message of our own.
  // ---------------------------------------------------------------------------

  inline def percent[N <: Int]: Double =
    inline if constValue[N] > 100 then error("more than 100 percent")
    else constValue[N] / 100.0

  val quarter: Double = percent[25] // 0.25

  //   percent[150]
  //   // more than 100 percent
  //
  //   percent[Int]
  //   // Cannot reduce `inline if` because its condition is not a constant value: ??? > 100
  //   // Int is not a constant type; cannot take constValue

  // A type bound by a pattern can be read too.
  final class Tagged[Name <: String]

  inline def tagOf[T]: String = inline erasedValue[T] match
    case _: Tagged[name] => constValue[name]

  val userTag: String = tagOf[Tagged["user"]] // "user"

  // ---------------------------------------------------------------------------
  // 6. `summonInline` and `summonFrom`
  //
  // A plain `summon` in an inline body is resolved where the body is written.
  // These two wait for the expansion, where the types are known.
  // ---------------------------------------------------------------------------

  //   inline def orderingOf[A]: Ordering[A] = summon[Ordering[A]]
  //   // No given instance of type Ordering[A] was found for parameter x of
  //   // method summon in object Predef.
  //   // …

  inline def orderingOf[A]: Ordering[A] = summonInline[Ordering[A]]

  val intOrdering: Ordering[Int] = orderingOf[Int]

  final class Widget

  //   orderingOf[Widget]
  //   // No given instance of type Ordering[Walkthrough.Widget] was found.
  //   // …

  // `summonFrom` branches on whether a search succeeds, in order, and can end
  // on something that is not an instance.
  inline def tidy[A](xs: List[A]): List[A] = summonFrom {
    case o: Ordering[A] => xs.sorted(using o)
    case _ => xs
  }

  val sorted: List[Int] = tidy(List(3, 1, 2)) // List(1, 2, 3)
  val unsorted: List[Widget] = tidy(List(Widget(), Widget())) // as given

  // ---------------------------------------------------------------------------
  // 7. Inline all the way up
  //
  // The reduction happens at the call site, with the types the call site
  // knows. A generic method knows a type parameter, and nothing reduces.
  // ---------------------------------------------------------------------------

  //   def kindOf[A](a: A): String = kind(a)
  //   // cannot reduce inline match with
  //   //  scrutinee:  a : (a : A)
  //   //  patterns :  case _:Int
  //   //              case _:String

  // Made `inline` itself, it moves the reduction one call site further up —
  // where, here, the type is known.
  inline def kindOf[A](a: A): String = kind(a)

  val kindOfTwo: String = kindOf(2) // "an Int"

end Walkthrough
