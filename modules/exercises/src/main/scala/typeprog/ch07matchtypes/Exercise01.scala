package typeprog.ch07matchtypes

/** Exercise 01 — take one layer off a type.
  *
  * Define `Unwrap[X]`, the type inside `X` when `X` is one of three
  * containers, and `X` itself otherwise:
  *
  *   - an `Option[a]` unwraps to `a` — and so does any subtype of it:
  *     `Unwrap[Some[String]]` is `String`;
  *   - an `Either[e, a]` unwraps to `a`, its right side;
  *   - a `List[a]` unwraps to `a`;
  *   - only one layer comes off: `Unwrap[Option[List[Int]]]` is `List[Int]`;
  *   - any other type is left as it is: `Unwrap[Int]` is `Int`, and
  *     `Unwrap[Vector[Int]]` is `Vector[Int]`.
  *
  * Hint: a case's pattern can name the parts of the type it matches, and the
  * result can use them. A catch-all case exists, and where it goes matters.
  */
object Exercise01:

  /** TODO: replace `Unsolved` with a match type. */
  type Unwrap[X] = X match
    case Option[a] => a
    case Either[?, a] => a
    case List[a] => a
    case _ => X
