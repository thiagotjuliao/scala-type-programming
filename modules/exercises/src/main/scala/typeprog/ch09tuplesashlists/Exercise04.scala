package typeprog.ch09tuplesashlists

import scala.compiletime.ops.int.*

/** Exercise 04 — a vector with its length in its type.
  *
  * `Vec[N, A]` holds `N` elements of type `A`, and `N` is a literal type:
  * `Vec.empty[Int]` is a `Vec[0, Int]`. Give the operations their types:
  *
  *   - `a :: v` is one longer than `v`: `1 :: 2 :: Vec.empty[Int]` is a
  *     `Vec[2, Int]`;
  *   - `v ++ w` is as long as both together: a `Vec[2, Int]` and a
  *     `Vec[3, Int]` make a `Vec[5, Int]`;
  *   - `tail` is one shorter;
  *   - `head` and `tail` compile only on a vector that is not empty —
  *     `Vec.empty[Int].head` does not compile, and neither does the `head` of a
  *     one-element vector's `tail`.
  *
  * The elements live in `items`, in order.
  *
  * Hint: `scala.compiletime.ops.int` does arithmetic and comparisons on
  * literal types, and a comparison is a `Boolean` literal type — which can be
  * asked for as evidence.
  */
object Exercise04:

  final class Vec[N <: Int, +A] private (val items: Vector[A]):

    /** TODO: one longer. */
    def ::[B >: A](b: B): Vec[N + 1, B] =
      Vec(b +: items)

    /** TODO: as long as both. */
    def ++[M <: Int, B >: A](other: Vec[M, B]): Vec[N + M, B] =
      Vec(items ++ other.items)

    /** TODO: only when not empty. */
    def head(using N > 0 =:= true): A =
      items.head

    /** TODO: only when not empty, and one shorter. */
    def tail(using N > 0 =:= true): Vec[N - 1, A] =
      Vec(items.tail)

  object Vec:
    def empty[A]: Vec[0, A] = Vec(Vector.empty)
end Exercise04
