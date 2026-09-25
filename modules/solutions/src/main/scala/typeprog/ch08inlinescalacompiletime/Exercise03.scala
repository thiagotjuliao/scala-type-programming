package typeprog.ch08inlinescalacompiletime

/** Exercise 03 — chapter 07's `add`, with a value. *Solved.*
  *
  * An `inline match` on `(a, b)` is reduced at each call site against the
  * arguments' *static* types: `add(1, 2.5)` keeps the `(Int, Double)` case and
  * nothing else. No runtime test is made, so erasure never gets a say —
  * which is what sank chapter 07's attempt with a runtime `match`.
  *
  * `transparent` gives the call the type of its expansion: `add(1, 2.5)` is a
  * `Double` — in fact `(3.5d : Double)`, the addition of two literals being
  * folded while compiling. The declared `Any` is only an upper bound. Without
  * `transparent` the expansion is just as precise and the call is an `Any`, so
  * `val d: Double = add(1, 2.5)` fails with *"Found: Any"*.
  *
  * The tempting signature, `: Sum[A, B]` with chapter 07's match type, does not
  * compile: the body is checked at the definition, where `Sum[A, B]` is stuck,
  * and every branch is rejected. `transparent` makes the match type
  * unnecessary.
  *
  * A pair of values that matches no case — `add(true, false)` — cannot be
  * reduced, and neither can a call from `def f[A](a: A)`, whose `(A, A)` might
  * be any case or none: *"cannot reduce inline match"*.
  */
object Exercise03:

  transparent inline def add[A, B](a: A, b: B): Any = inline (a, b) match
    case (x: Int, y: Int) => x + y
    case (x: Int, y: Double) => x + y
    case (x: Double, y: Int) => x + y
    case (x: Double, y: Double) => x + y
    case (x: String, y: String) => x + y
