package typeprog.ch09tuplesashlists

import scala.compiletime.{erasedValue, error}
import scala.compiletime.ops.int.{+, >}

import typeprog.core.evidence

/** Chapter 09 — tuples as HLists.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The chapter covers six things, in order:
  *
  *   1. a tuple is a list of types — `*:` and `EmptyTuple`;
  *   2. the standard library's tuple operations, which are match types;
  *   3. writing one;
  *   4. values over any tuple — by instances, and by inline expansion;
  *   5. counting in types with Peano numbers;
  *   6. counting in types with `scala.compiletime.ops`.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch09-tuples-as-hlists.md`.
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. A tuple is a list of types
  // ---------------------------------------------------------------------------

  evidence.sameType[(Int, String, Boolean), Int *: String *: Boolean *: EmptyTuple]

  // The same for values: `*:` builds a tuple from a head and a tail.
  val built: (Int, String) = 1 *: "a" *: EmptyTuple

  // A one-element tuple is `Tuple1`; parentheses around one value are not one.
  evidence.sameType[Tuple1[Int], Int *: EmptyTuple]
  val justOne: Int = 1

  // A match on any tuple takes it apart one element at a time.
  def describe(t: Tuple): String = t match
    case EmptyTuple => "end"
    case h *: rest => s"$h, " + describe(rest)

  val described: String = describe((1, "a")) // "1, a, end"

  // ---------------------------------------------------------------------------
  // 2. The standard library's operations are match types
  // ---------------------------------------------------------------------------

  evidence.sameType[Tuple.Head[(Int, String)], Int]
  evidence.sameType[Tuple.Tail[(Int, String)], String *: EmptyTuple]
  evidence.sameType[Tuple.Concat[(Int, String), (Boolean, Char)], (Int, String, Boolean, Char)]
  evidence.sameType[Tuple.Map[(Int, String), Option], (Option[Int], Option[String])]
  evidence.sameType[Tuple.Size[(Int, String)], 2]

  // The value-level operations are typed by them.
  val appended: (Int, String, Boolean) = (1, "a") ++ Tuple1(true)

  // ---------------------------------------------------------------------------
  // 3. Writing one
  //
  // A case for the end, a case for a head and a tail. `X` is upper-case, so it
  // is the type parameter used as a pattern: the case matches when the head
  // conforms to it.
  // ---------------------------------------------------------------------------

  type Replace[T <: Tuple, X, Y] <: Tuple = T match
    case EmptyTuple => EmptyTuple
    case X *: t => Y *: Replace[t, X, Y]
    case h *: t => h *: Replace[t, X, Y]

  evidence.sameType[Replace[(Int, String, Int), Int, Long], (Long, String, Long)]

  // ---------------------------------------------------------------------------
  // 4. Values over any tuple
  //
  // By instances: one rule for the end, one for a head and a tail. The search
  // applies the second once per element.
  // ---------------------------------------------------------------------------

  trait Show[A]:
    def show(a: A): String

  object Show:
    given Show[Int] = _.toString
    given Show[String] = s => s"'$s'"
    given Show[Boolean] = b => if b then "yes" else "no"

    given Show[EmptyTuple] = _ => "()"

    given [H, T <: Tuple] => (head: Show[H], tail: Show[T]) => Show[H *: T] =
      case h *: EmptyTuple => head.show(h)
      case h *: t => head.show(h) + ", " + tail.show(t)

  val shown: String = summon[Show[(Int, String, Boolean)]].show((1, "a", true)) // "1, 'a', yes"

  // By inline expansion (chapter 08): the same recursion, closed, with no
  // instance at all.
  inline def nameOf[A]: String = inline erasedValue[A] match
    case _: Int => "Int"
    case _: String => "String"
    case _: Boolean => "Boolean"
    case _ => error("nameOf does not know this type")

  inline def typeNames[T <: Tuple]: List[String] = inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (h *: t) => nameOf[h] :: typeNames[t]

  val names: List[String] = typeNames[(Int, String, Boolean)] // List(Int, String, Boolean)

  // ---------------------------------------------------------------------------
  // 5. Counting in types: Peano numbers
  //
  // Zero, or the successor of a number. Arithmetic is a case for `Zero` and a
  // case for `Succ[n]`, whose binder is one less.
  // ---------------------------------------------------------------------------

  sealed trait Nat
  sealed trait Zero extends Nat
  sealed trait Succ[N <: Nat] extends Nat

  type One = Succ[Zero]
  type Three = Succ[Succ[One]]

  type Twice[N <: Nat] <: Nat = N match
    case Zero => Zero
    case Succ[n] => Succ[Succ[Twice[n]]]

  evidence.sameType[Twice[Three], Succ[Succ[Succ[Succ[Succ[Succ[Zero]]]]]]]

  // A Peano number can be counted into a literal type with `ops.int.+`.
  type Count[N <: Nat] <: Int = N match
    case Zero => 0
    case Succ[n] => Count[n] + 1

  evidence.sameType[Count[Twice[Three]], 6]

  // ---------------------------------------------------------------------------
  // 6. Counting in types: `scala.compiletime.ops`
  //
  // Arithmetic and comparisons on literal types, evaluated when the arguments
  // are literals. A comparison is a `Boolean` literal type, usable as evidence.
  // ---------------------------------------------------------------------------

  evidence.sameType[1 + 2, 3]
  evidence.sameType[3 > 2, true]

  /** Only for a positive `N`. */
  def positive[N <: Int](using N > 0 =:= true): String = "positive"

  val isPositive: String = positive[3]

  //   positive[0]
  //   // Cannot prove that (0 : Int) > (0 : Int) =:= (true : Boolean).

  // Nothing is known about an abstract number — not even that adding zero
  // changes nothing:
  //
  //   def same[N <: Int] = summon[N + 0 =:= N]
  //   // Cannot prove that N + (0 : Int) =:= N.

end Walkthrough
