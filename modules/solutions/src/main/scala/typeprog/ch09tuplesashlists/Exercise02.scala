package typeprog.ch09tuplesashlists

import scala.compiletime.ops.int.+

/** Exercise 02 — the position of a type in a tuple. *Solved.*
  *
  * Three cases, as for any operation on a tuple. `EmptyTuple`: not found,
  * `-1`. `X *: _`: the head is `X`, so the index is `0` — `X` is upper-case,
  * so it is the type parameter used as a pattern, not a new binder. Otherwise
  * the answer is the tail's, one further along.
  *
  * "One further along" is not simply `IndexOf[t, X] + 1`: that turns the
  * tail's `-1` into `0`, and every absent element would be found at the head.
  * `Shift` keeps `-1` as it is and adds one to anything else. Its `case -1` is
  * a literal pattern, and it can be moved past for `2` because two different
  * literal types are disjoint (chapter 07); `+` then evaluates, both sides
  * being literals.
  *
  * Moving past `case X *: _` for a head that is not `X` needs the same proof of
  * disjointness: `Int` and `String` are classes, so it holds. For two unsealed
  * traits it would not, and `IndexOf` would stop there, unreduced.
  */
object Exercise02:

  type IndexOf[T <: Tuple, X] <: Int = T match
    case EmptyTuple => -1
    case X *: _ => 0
    case _ *: t => Shift[IndexOf[t, X]]

  /** `-1` stays `-1`; any other index moves one along. */
  type Shift[N <: Int] <: Int = N match
    case -1 => -1
    case _ => N + 1
