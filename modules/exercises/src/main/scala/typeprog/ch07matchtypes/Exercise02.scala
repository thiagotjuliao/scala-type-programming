package typeprog.ch07matchtypes

/** Exercise 02 — take every layer off a type.
  *
  * Define `Leaf[X]`: like exercise 01's `Unwrap`, with the same three
  * containers — `Option`, `Either` (its right side) and `List` — except that
  * it does not stop after one layer. What is inside is unwrapped again, until
  * what is left is not one of the three:
  *
  *   - `Leaf[List[Option[Either[String, Int]]]]` is `Int`;
  *   - `Leaf[Int]` is `Int`;
  *   - `Leaf[List[Vector[Int]]]` is `Vector[Int]`: a `Vector` is not one of
  *     the three, so the unwrapping stops there.
  *
  * Hint: a case's result can be the match type being defined, applied to
  * something smaller.
  */
object Exercise02:

  /** TODO: replace `Unsolved` with a recursive match type. */
  type Leaf[X] = X match
    case Option[a] => Leaf[a]
    case Either[?, a] => Leaf[a]
    case List[a] => Leaf[a]
    case _ => X
