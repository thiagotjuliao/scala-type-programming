package typeprog.ch09tuplesashlists

import scala.compiletime.ops.int.+

/** Exercise 01 — arithmetic on Peano numbers. *Solved.*
  *
  * Each operation recurses on the structure of its first argument. `Plus`:
  * zero plus `B` is `B`; the successor of `a`, plus `B`, is the successor of
  * `a + B`. The binder in `Succ[a]` is the predecessor — the only subtraction
  * Peano numbers need, and the reason a match type can do this arithmetic with
  * no help: the pattern takes the number apart.
  *
  * `Plus[_3, _0]` is `_3` without a case for it: three steps down the first
  * argument, and the `Zero` case returns `B`, which is `_0`, wrapped in three
  * `Succ`s.
  *
  * `Times` is repeated addition — `(a + 1) × B = B + a × B` — built on `Plus`.
  * `ToInt` counts the `Succ`s with `compiletime.ops.int.+`, which the compiler
  * evaluates as soon as both sides are literals: `ToInt[_3]` is `((0 + 1) + 1)
  * + 1`, reduced to `3`.
  *
  * The bounds `<: Nat` and `<: Int` on the results say what each operation
  * returns, and are checked against every case (chapter 07). Generic code
  * holding an unreduced `Plus[A, B]` can then use it as a `Nat`.
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

  type Plus[A <: Nat, B <: Nat] <: Nat = A match
    case Zero => B
    case Succ[a] => Succ[Plus[a, B]]

  type Times[A <: Nat, B <: Nat] <: Nat = A match
    case Zero => Zero
    case Succ[a] => Plus[B, Times[a, B]]

  type ToInt[N <: Nat] <: Int = N match
    case Zero => 0
    case Succ[n] => ToInt[n] + 1
end Exercise01
