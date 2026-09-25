package typeprog.ch08inlinescalacompiletime

/** Exercise 03 — chapter 07's `add`, with a value.
  *
  * Chapter 07 could compute the *type* of a sum — `Sum[Int, Double]` is
  * `Double` — and could not produce the value: a runtime `match` cannot tell
  * `(Int, Double)` from `(Int, Int)` once the types are erased.
  *
  * Write `add` so that:
  *
  *   - `Int` and `Int` make an `Int`; `Int` and `Double`, either way round, and
  *     `Double` and `Double`, make a `Double`; `String` and `String` make a
  *     `String` — the concatenation;
  *   - the result type is exact, statically: `val d: Double = add(1, 2.5)`
  *     compiles, `val i: Int = add(1, 2.5)` does not;
  *   - the arguments need not be constants: `add(x, y)` for an `x: Int` and a
  *     `y: Double` is a `Double`;
  *   - anything else — `add(1, "a")`, `add(true, false)` — does not compile,
  *     and neither does a call from a generic method that cannot tell which
  *     case applies.
  *
  * No match type is needed. As shipped, `add` returns `Any`.
  *
  * Hint: the choice can be made on the arguments' static types, at each call.
  * And a call's type can be the type of what it expands to.
  */
object Exercise03:

  /** TODO: the sum, with the type of the sum. */
  inline def add[A, B](a: A, b: B): Any = ???
