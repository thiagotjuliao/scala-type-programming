package typeprog.ch08inlinescalacompiletime

/** Exercise 04 — use an instance if there is one.
  *
  * `describe(a)` renders a value in the best way available for its type:
  *
  *   - with its `Show`, if it has one: `describe(true)` is `"yes"`;
  *   - otherwise, if it has a `Numeric`, as `"number "` followed by the value:
  *     `describe(2.5)` is `"number 2.5"`;
  *   - a `Show` wins over a `Numeric`: an `Int` has both, and `describe(42)` is
  *     `"#42"`;
  *   - an instance in scope at the call counts — a `given Show[Double]` defined
  *     next to a call to `describe(2.5)` is the one used;
  *   - a type with neither does not compile, and the error says `neither a
  *     Show nor a Numeric`.
  *
  * Chapter 06 would have ranked two rules with a low-priority trait. Here the
  * order is written in one place, and the last resort is not an instance.
  *
  * Hint: an instance search can be one case of a match, tried in order, at
  * the call site.
  */
object Exercise04:

  trait Show[A]:
    def show(a: A): String

  object Show:
    given Show[Int] = i => s"#$i"
    given Show[Boolean] = b => if b then "yes" else "no"

  /** TODO: a `Show` if there is one, a `Numeric` if not, a compile error if neither. */
  inline def describe[A](a: A): String = ???
