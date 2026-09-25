package typeprog.ch07matchtypes

import typeprog.core.Unsolved

/** Exercise 03 — a result type computed from the argument's.
  *
  * `first` returns the first element of what it is given, if there is one —
  * and the type of what it returns depends on the type of what it is given:
  *
  *   - `first("abc")` is `Some('a')`, an `Option[Char]`;
  *   - `first(List(1, 2))` is `Some(1)`, an `Option[Int]`;
  *   - `first(Option(3))` is `Some(3)`: an `Option` is its own first element;
  *   - an empty `String`, `List` or `Option` gives `None`;
  *   - anything else — `first(42)`, `first(Vector(1))` — does not compile.
  *
  * `First[X]` names the result type: `First[String]` is `Option[Char]`.
  *
  * The body must not need a cast. `asInstanceOf` makes any of this compile,
  * and takes away the reason to write it.
  *
  * Hint: a `match` on the value can be checked against the match type, case
  * by case — if it has the same cases, in the same order. Keeping `42` out is
  * a job for the parameter's type (chapter 04).
  */
object Exercise03:

  /** TODO: replace `Unsolved` with a match type. */
  type First[X] = Unsolved

  /** TODO: the signature keeps anything else out; the body is a `match`. */
  def first[X](x: X): First[X] = ???
