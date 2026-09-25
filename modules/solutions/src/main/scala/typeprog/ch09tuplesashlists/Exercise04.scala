package typeprog.ch09tuplesashlists

import scala.compiletime.ops.int.{+, -, >}

/** Exercise 04 — a vector with its length in its type. *Solved.*
  *
  * `N + 1`, `N + M` and `N - 1` are types from `compiletime.ops.int`. They stay
  * symbolic inside `Vec`, where `N` is a type parameter, and are evaluated at
  * each use, where `N` is a literal: `1 :: 2 :: Vec.empty[Int]` is a `Vec[0 +
  * 1 + 1, Int]`, which the compiler reduces to `Vec[2, Int]`.
  *
  * `head` and `tail` ask for `N > 0 =:= true`. `N > 0` is a `Boolean` literal
  * type once `N` is known — `true` for `2`, `false` for `0` — and `=:=`
  * evidence exists only when the two sides are the same type. For an empty
  * vector the evidence is missing, and the call does not compile:
  *
  * {{{
  * Cannot prove that (0 : Int) > (0 : Int) =:= (true : Boolean).
  * }}}
  *
  * The same check could be a runtime `require`, and it would pass every test
  * but the ones that expect a compile error. The length in the type is what
  * turns "empty vector" from an exception into a program that does not
  * compile.
  */
object Exercise04:

  final class Vec[N <: Int, +A] private (val items: Vector[A]):

    def ::[B >: A](b: B): Vec[N + 1, B] = Vec(b +: items)

    def ++[M <: Int, B >: A](other: Vec[M, B]): Vec[N + M, B] = Vec(items ++ other.items)

    def head(using N > 0 =:= true): A = items.head

    def tail(using N > 0 =:= true): Vec[N - 1, A] = Vec(items.tail)

  object Vec:
    def empty[A]: Vec[0, A] = Vec(Vector.empty)
