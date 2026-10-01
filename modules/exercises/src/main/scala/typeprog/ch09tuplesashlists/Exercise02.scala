package typeprog.ch09tuplesashlists

import scala.compiletime.ops.int.*

/** Exercise 02 — the position of a type in a tuple.
  *
  * `IndexOf[T, X]` is where `X` first occurs in the tuple `T`, counted from
  * `0`, as a literal `Int` type — or `-1` if it does not occur at all:
  *
  *   - `IndexOf[(Int, String, Boolean), Int]` is `0`, and `Boolean` is at `2`;
  *   - `IndexOf[(Char, Int, String, Int), Int]` is `1`: the first occurrence;
  *   - `IndexOf[(Int, String, Boolean), Char]` is `-1`, and so is anything in
  *     `EmptyTuple`.
  *
  * Being a literal type, the index can be read as a value with `constValue`.
  * The standard library has no `IndexOf`; its `Contains` and `Size` are close
  * relatives worth reading.
  *
  * Hint: a case for the end, a case for "the head is `X`", and a case for
  * everything else. The last one has to add one to what the tail gives —
  * unless the tail gave `-1`.
  */
object Exercise02:

  /** TODO: replace `Unsolved` with a match type. */
  type IndexOf[T <: Tuple, X] = T match
    case EmptyTuple => -1
    case X *: _ => 0
    case _ *: t =>
      IndexOf[t, X] match
        case -1 => -1
        case _ => 1 + IndexOf[t, X]
