package typeprog.ch07matchtypes

/** Exercise 04 — units that cancel.
  *
  * Chapter 05 ended with quantities whose unit is a phantom type: a
  * `Quantity[Meters]` and a `Quantity[Seconds]` are both a bare `Double` at
  * runtime, cannot be added to each other, and divide into a
  * `Quantity[Meters / Seconds]`. What it could not do is multiply back: speed
  * times time should be a distance, and there was no way to say that the
  * `Seconds` cancel.
  *
  * Give `*` its unit, `Times[U, V]`:
  *
  *   - a ratio times the unit it divides by is the numerator:
  *     `(Meters / Seconds) * Seconds` is `Meters`;
  *   - and from the other side: `Seconds * (Meters / Seconds)` is `Meters`;
  *   - anything else is a product: `Meters * Seconds` is `Meters * Seconds`,
  *     and `(Meters / Seconds) * Meters` stays as it is — only the unit
  *     divided by cancels.
  *
  * This is not unit algebra in general, and it does not need to be: a ratio
  * against a ratio, or a product against anything, is simply a product.
  *
  * The amount of the result is the product of the amounts.
  *
  * Hint: comparing two types takes a match inside a match — the inner
  * pattern can be a type the outer one bound. The units are sealed traits
  * with no subtypes, which is what lets the compiler tell them apart.
  */
object Exercise04:

  sealed trait Meters
  sealed trait Seconds
  sealed trait Kilograms

  /** The unit of a ratio. Never instantiated: written infix, `Meters / Seconds`. */
  sealed trait /[A, B]

  /** The unit of a product. Never instantiated: written infix, `Meters * Seconds`. */
  sealed trait *[A, B]

  opaque type Quantity[U] = Double

  object Quantity:
    def of[U](amount: Double): Quantity[U] = amount

  extension [U](q: Quantity[U])
    def amount: Double = q
    def +(other: Quantity[U]): Quantity[U] = q + other
    def per[V](other: Quantity[V]): Quantity[U / V] = q / other

    /** TODO: the body. */
    def *[V](other: Quantity[V]): Quantity[Times[U, V]] =
      q * other

  /** TODO: replace `Unsolved` with the unit of a product. */
  type Times[U, V] = (U, V) match
    case (a / b, c) =>
      c match
        case b => a
        case _ => U * V
    case (a, b / c) =>
      c match
        case a => b
        case _ => U * V
    case _ => U * V
end Exercise04
