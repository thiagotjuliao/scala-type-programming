package typeprog.ch21unitsofmeasure

/** Exercise 01 — multiply and divide, and let the compiler work out the unit.
  *
  * `Quantity[U]` is chapter 05's, from its exercise 04, given here solved,
  * with one change: a unit is no longer a name but three exponents, of metre,
  * second and kilogram, written as a tuple of literal types — `Metre` is
  * `(1, 0, 0)`. Names cannot be computed with; exponents can.
  *
  * Multiplying two quantities adds the exponents of their units, and dividing
  * subtracts them: m · m is `(2, 0, 0)`, m / s is `(1, -1, 0)`.
  *
  *   - `Mul[A, B]` and `Div[A, B]` compute those units, exponent by exponent.
  *   - `*` and `/` between two quantities return a quantity of the computed
  *     unit, so `metres(10) / seconds(2)` is a `Quantity[(1, -1, 0)]`, and
  *     multiplying it back by seconds is a `Quantity[Metre]` again — with no
  *     rule written for cancelling.
  *
  * `Mul` and `Div` are `Nothing` in the stub only because `Quantity` wants a
  * `Dim`, which `Unsolved` is not.
  *
  * Hint: chapter 07 for a type computed from two types, chapter 09 for adding
  * two literal `Int` types.
  */
object Exercise01:

  type Dim = (Int, Int, Int)

  type Metre = (1, 0, 0)
  type Second = (0, 1, 0)
  type Kilogram = (0, 0, 1)
  type Dimensionless = (0, 0, 0)

  opaque type Quantity[U <: Dim] = Double

  def metres(d: Double): Quantity[Metre] = d
  def seconds(d: Double): Quantity[Second] = d
  def kilograms(d: Double): Quantity[Kilogram] = d

  /** TODO: the unit of a product. */
  type Mul[A <: Dim, B <: Dim] = Nothing

  /** TODO: the unit of a quotient. */
  type Div[A <: Dim, B <: Dim] = Nothing

  extension [U <: Dim](q: Quantity[U])
    def value: Double = q
    def scale(k: Double): Quantity[U] = q * k
    def +(r: Quantity[U]): Quantity[U] = q + r
    def -(r: Quantity[U]): Quantity[U] = q - r

    /** TODO: implement. */
    def *[V <: Dim](r: Quantity[V]): Quantity[Mul[U, V]] = ???

    /** TODO: implement. */
    def /[V <: Dim](r: Quantity[V]): Quantity[Div[U, V]] = ???
end Exercise01
