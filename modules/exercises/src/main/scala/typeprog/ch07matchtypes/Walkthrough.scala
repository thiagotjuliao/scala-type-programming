package typeprog.ch07matchtypes

import typeprog.core.evidence

/** Chapter 07 — match types.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The chapter covers six things, in order:
  *
  *   1. a match type — a table from types to types, reduced by the compiler;
  *   2. when the compiler cannot decide — disjointness, and stuck types;
  *   3. recursion;
  *   4. values of a match type — the `match` that mirrors one;
  *   5. bounds;
  *   6. match types next to chapter 06's type classes, and what erasure does
  *      to the values.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch07-match-types.md`.
  *
  * (Messages quoted below drop the `typeprog.ch07matchtypes.Walkthrough.`
  * prefix the compiler prints in front of every name.)
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. A match type
  //
  // A `match` whose scrutinee and results are types. The compiler reduces it
  // by taking the cases in order and picking the first whose pattern the
  // argument conforms to.
  // ---------------------------------------------------------------------------

  type Doubled[X] = X match
    case Int => Long
    case String => String
    case Option[a] => Option[(a, a)] // `a` is a binder: lower-case, as in a value pattern

  evidence.sameType[Doubled[Int], Long]
  evidence.sameType[Doubled[String], String]
  evidence.sameType[Doubled[Option[Char]], Option[(Char, Char)]]

  // The test is subtyping, not equality: `Some[Char] <: Option[a]`, `a = Char`.
  evidence.sameType[Doubled[Some[Char]], Option[(Char, Char)]]

  // An upper-case name in a pattern is a *reference*, not a binder:
  //
  //   type Bad[X] = X match
  //     case List[T] => T
  //   // Not found: type T

  // ---------------------------------------------------------------------------
  // 2. When the compiler cannot decide
  //
  // A case that does not match is skipped only if the compiler can prove the
  // argument and the pattern *disjoint* — that no type is both. Otherwise the
  // match type stays as it is: stuck.
  // ---------------------------------------------------------------------------

  // `Int` does not match `case Option[a]`, and two classes neither of which
  // extends the other are disjoint: nothing is both an `Int` and an `Option`.
  // So `Doubled[Int]` could get past the `Option` case, had it come first.

  // A type parameter is not known to be anything, and may be anything. Inside
  // a generic method, `Doubled[A]` does not reduce:
  //
  //   def stuck[A](a: A): Long = (??? : Doubled[A])
  //   // Found:    Doubled[A]
  //   // Required: Long
  //   //
  //   // Note: a match type could not be fully reduced:
  //   //
  //   //   trying to reduce  Doubled[A]
  //   //   failed since selector A
  //   //   does not match  case Int => Long
  //   //   and cannot be shown to be disjoint from it either.
  //   //   Therefore, reduction cannot advance to the remaining cases
  //   //
  //   //     case String => String
  //   //     case Option[a] => Option[(a, a)]

  // Two unsealed traits are never disjoint — a class could extend both:
  trait Loud
  trait Quiet

  type Volume[X] = X match
    case Loud => "loud"
    case Quiet => "quiet"

  evidence.sameType[Volume[Loud], "loud"]

  //   evidence.sameType[Volume[Quiet], "quiet"]
  //   // Cannot prove that Volume[Quiet] =:= ("quiet" : String).
  //   //
  //   // Note: a match type could not be fully reduced:
  //   //
  //   //   trying to reduce  Volume[Quiet]
  //   //   failed since selector Quiet
  //   //   does not match  case Loud => ("loud" : String)
  //   //   and cannot be shown to be disjoint from it either.
  //   //   Therefore, reduction cannot advance to the remaining case
  //   //
  //   //     case Quiet => ("quiet" : String)

  // Sealed traits with no subtypes are disjoint — every subtype is known, and
  // there are none — and so are two different literal types (chapter 04).
  sealed trait Low
  sealed trait High

  type Level[X] = X match
    case Low => 0
    case High => 1

  evidence.sameType[Level[High], 1]

  type IsZero[N <: Int] = N match
    case 0 => true
    case _ => false

  evidence.sameType[IsZero[0], true]
  evidence.sameType[IsZero[3], false]

  // …but `Int` is not disjoint from `0`: `0` *is* an `Int`.
  //
  //   evidence.sameType[IsZero[Int], false]
  //   // Cannot prove that IsZero[Int] =:= (false : Boolean).
  //   // … failed since selector Int
  //   // does not match  case (0 : Int) => (true : Boolean)
  //   // and cannot be shown to be disjoint from it either.

  // A match type with no case for its argument is stuck too. There is no
  // implicit `Nothing` at the end:
  //
  //   evidence.sameType[Doubled[Boolean], Nothing]
  //   // … trying to reduce  Doubled[Boolean]
  //   // failed since selector Boolean
  //   // matches none of the cases

  // ---------------------------------------------------------------------------
  // 3. Recursion
  //
  // A case can apply the match type being defined — to something smaller.
  // ---------------------------------------------------------------------------

  /** The last element type of a tuple. The standard library's `Tuple.Last` is
    * this, and `Tuple.Head`, `Tuple.Concat` and the rest are match types too.
    */
  type Last[T <: Tuple] = T match
    case x *: EmptyTuple => x
    case _ *: rest => Last[rest]

  evidence.sameType[Last[(Int, String, Boolean)], Boolean]
  evidence.sameType[Tuple.Last[(Int, String, Boolean)], Boolean]
  evidence.sameType[Tuple.Head[(Int, String)], Int]

  // An empty tuple has no first element, and no case says otherwise:
  //
  //   evidence.sameType[Tuple.Head[EmptyTuple], Nothing]
  //   // … trying to reduce  Tuple.Head[EmptyTuple]
  //   // failed since selector EmptyTuple
  //   // matches none of the cases
  //   //
  //   //   case x *: _ => x

  // A recursion that never gets smaller is stopped:
  //
  //   type Grow[X] = X match
  //     case Int => Grow[List[X]]
  //     case List[a] => Grow[List[X]]
  //   def grow: Grow[Int] = ???
  //   // Recursion limit exceeded.
  //   // Maybe there is an illegal cyclic reference?
  //   // …

  // ---------------------------------------------------------------------------
  // 4. Values of a match type
  //
  // Inside `double`, `Doubled[X]` is stuck — `X` is a type parameter — and no
  // value conforms to a stuck type. A `match` that mirrors the match type is
  // the exception: each branch is checked against the *reduced* case.
  // ---------------------------------------------------------------------------

  def double[X <: Int | String | Option[?]](x: X): Doubled[X] = x match
    case i: Int => i.toLong * 2 // checked against Long
    case s: String => s + s // against String
    case o: Option[a] => o.map(a => (a, a)) // against Option[(a, a)]

  val doubledInt: Long = double(21) // 42L — a Long, statically
  val doubledText: String = double("ab") // "abab"
  val doubledSome: Option[(Char, Char)] = double(Option('x')) // Some(('x', 'x'))

  // Typed patterns, the same types, the same order, the same number, no
  // guards. One extra case, added to be safe, and every branch is rejected:
  //
  //   def doubleSafe[X](x: X): Doubled[X] = x match
  //     case i: Int => i.toLong * 2
  //     case s: String => s + s
  //     case o: Option[a] => o.map(a => (a, a))
  //     case _ => ???
  //   // Found:    Long
  //   // Required: Doubled[X]
  //   //
  //   // Note: a match type could not be fully reduced: …

  // The bound on `X` is what keeps `double(true)` out. Without it the call
  // compiles — `Doubled[Boolean]` is a stuck type, and a stuck type is a type —
  // and throws a MatchError when it runs. With it:
  //
  //   double(true)
  //   // Found:    (true : Boolean)
  //   // Required: Int | String | Option[?]

  // ---------------------------------------------------------------------------
  // 5. Bounds
  //
  // Generic code holding a stuck match type can do with it what it could do
  // with the bound — `Any` unless one is declared. A declared bound is checked
  // against every case.
  // ---------------------------------------------------------------------------

  type Name[X] <: CharSequence = X match
    case String => String
    case Int => StringBuilder

  def name[X <: String | Int](x: X): Name[X] = x match
    case s: String => s
    case i: Int => StringBuilder("#").append(i)

  // `Name[X]` is stuck here, and still a `CharSequence`:
  def nameLength[X <: String | Int](x: X): Int = name(x).length

  //   type Name[X] <: CharSequence = X match
  //     case String => String
  //     case Int => Int
  //   // Found:    Int
  //   // Required: CharSequence

  // ---------------------------------------------------------------------------
  // 6. Match types and type classes
  //
  // Chapter 06's `Add` computed a result type by searching for an instance.
  // The same table, written directly:
  // ---------------------------------------------------------------------------

  type Sum[A, B] = (A, B) match
    case (Int, Int) => Int
    case (Int, Double) => Double
    case (Double, Int) => Double
    case (Double, Double) => Double
    case (String, String) => String
    case ((a1, a2), (b1, b2)) => (Sum[a1, b1], Sum[a2, b2])

  evidence.sameType[Sum[Int, Double], Double]
  evidence.sameType[Sum[(Int, String), (Double, String)], (Double, String)]

  // No instance, no `Aux`. What it does not give is the value. `Int` and
  // `Double` inside a pair are type *arguments*, and erased: at runtime
  // `(1, 2.5)` is a `Tuple2` like any other. A mirroring `match` compiles —
  //
  //   def add[A, B](a: A, b: B): Sum[A, B] = (a, b) match
  //     case p: (Int, Int) => p._1 + p._2
  //     case p: (Int, Double) => p._1 + p._2
  //     …
  //   // warning: the type test for (Int, Int) cannot be checked at runtime
  //   //          because its type arguments can't be determined from (A, B)
  //   // warning: Unreachable case  (for every case after the first)
  //
  // — and `add(1, 2.5)` takes the first case and throws:
  //
  //   java.lang.ClassCastException: class java.lang.Double cannot be cast to
  //   class java.lang.Integer
  //
  // `double` works because `Int`, `String` and `Option` are classes a runtime
  // test can see. Where only type arguments tell the cases apart, the value
  // needs a dispatch that happens at compile time: a type class (chapter 06),
  // or an `inline match` (chapter 08).

end Walkthrough
