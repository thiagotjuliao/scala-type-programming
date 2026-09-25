package typeprog.ch07matchtypes

/** Exercise 02 — take every layer off a type. *Solved.*
  *
  * The only change from `Unwrap` is in the results: where `Unwrap` returned
  * `a`, `Leaf` returns `Leaf[a]`. For `List[Option[Either[String, Int]]]` the
  * compiler reduces four times — `List`, `Option`, `Either`, then the
  * catch-all at `Int` — and every step is on a smaller type, which is why it
  * stops.
  *
  * The catch-all is the base case. Without it, `Leaf[Int]` would match none
  * of the cases and stay `Leaf[Int]`: a match type is a partial function, and
  * outside its domain it does not fail, it just never reduces.
  *
  * The near miss is `case Option[a] => Unwrap[a]`, reusing exercise 01: one
  * extra layer, not all of them. `Leaf[List[List[List[Int]]]]` would stop at
  * `List[Int]`. The recursion has to go through `Leaf` itself.
  */
object Exercise02:

  type Leaf[X] = X match
    case Option[a] => Leaf[a]
    case Either[?, a] => Leaf[a]
    case List[a] => Leaf[a]
    case _ => X
