package typeprog.ch09tuplesashlists

import typeprog.core.Unsolved

/** Exercise 01 — arithmetic on Peano numbers.
  *
  * A natural number as a type is `Zero`, or the successor `Succ[N]` of a
  * number `N`. The aliases `_0` to `_6` are the first seven.
  *
  * Define, as match types:
  *
  *   - `Plus[A, B]`: `Plus[_2, _3]` is `_5`, and zero is its unit on either
  *     side;
  *   - `Times[A, B]`: `Times[_2, _3]` is `_6`; zero times anything, and
  *     anything times zero, is `_0`; one is its unit on either side;
  *   - `ToInt[N]`, the same number as a literal `Int` type: `ToInt[_3]` is `3`.
  *
  * Hint: recurse on the first argument — a case for `Zero`, and a case for
  * `Succ[n]` whose binder is one less. `Times` can use `Plus`, and `ToInt` can
  * use `scala.compiletime.ops.int`.
  */
object Exercise01:

  sealed trait Nat
  sealed trait Zero extends Nat
  sealed trait Succ[N <: Nat] extends Nat

  type _0 = Zero
  type _1 = Succ[_0]
  type _2 = Succ[_1]
  type _3 = Succ[_2]
  type _4 = Succ[_3]
  type _5 = Succ[_4]
  type _6 = Succ[_5]

  /** TODO: replace `Unsolved` with a match type. */
  type Plus[A <: Nat, B <: Nat] = Unsolved

  /** TODO: replace `Unsolved` with a match type. */
  type Times[A <: Nat, B <: Nat] = Unsolved

  /** TODO: replace `Unsolved` with a match type. */
  type ToInt[N <: Nat] = Unsolved
