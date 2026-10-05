package typeprog.ch21unitsofmeasure

import scala.compiletime.ops.int.{%, +, -, /}

/** Exercise 03 — formulas the compiler checks. *Solved.*
  *
  * The units are built from units, `Div[Metre, Second]` rather than
  * `(1, -1, 0)`. Both are the same type — the spec checks that they are — but
  * the first says what a velocity *is*, and an error in it is an error in
  * physics rather than in arithmetic. `Energy` is spelled the way kinetic
  * energy computes it, a mass times a velocity squared, so the formula's
  * result has exactly the declared unit with no conversion in between.
  *
  * Each body is the formula as written on paper. What makes it trustworthy is
  * the signature: `fallTime` returns a `Quantity[Second]`, and
  * `(height.scale(2) / gravity).sqrt` reduces to `Half[(0, 2, 0)]`, which is
  * `(0, 1, 0)`. The wrong formula, `(height * gravity).sqrt`, reduces to
  * `(1, -1, 0)` — a velocity — and the declared `Quantity[Second]` rejects it.
  * The `2` goes through `scale`, not `*`: it has no unit, and a plain
  * `Double` is not a `Quantity`.
  *
  * All of it lives outside `units`, and has to. Inside, `Quantity` is `Double`:
  * the same three bodies compile with any unit at all, including the wrong
  * formula, and the signatures stop checking anything.
  *
  * `kineticEnergy` multiplies `speed * speed` first and the mass after, to
  * match the order in `Energy`; with the mass first, the unit is
  * `Mul[Mul[Kilogram, Velocity], Velocity]`, which reduces to the same
  * `(2, -2, 1)` — the order only matters to a reader.
  */
object Exercise03:

  /** Exercise 02's library, solved. In an object of its own, because inside
    * the scope that defines an opaque type the type is its representation: a
    * formula written in here would be `Double` arithmetic, and no unit in it
    * would be checked.
    */
  object units:

    type Dim = (Int, Int, Int)

    type Metre = (1, 0, 0)
    type Second = (0, 1, 0)
    type Kilogram = (0, 0, 1)
    type Dimensionless = (0, 0, 0)

    opaque type Quantity[U <: Dim] = Double

    def quantity[U <: Dim](d: Double): Quantity[U] = d
    def metres(d: Double): Quantity[Metre] = d
    def seconds(d: Double): Quantity[Second] = d
    def kilograms(d: Double): Quantity[Kilogram] = d

    type Mul[A <: Dim, B <: Dim] <: Dim = (A, B) match
      case ((m1, s1, k1), (m2, s2, k2)) => (m1 + m2, s1 + s2, k1 + k2)

    type Div[A <: Dim, B <: Dim] <: Dim = (A, B) match
      case ((m1, s1, k1), (m2, s2, k2)) => (m1 - m2, s1 - s2, k1 - k2)

    type HalfOf[N <: Int] <: Int = N % 2 match
      case 0 => N / 2

    type Half[U <: Dim] <: Dim = U match
      case (m, s, k) => (HalfOf[m], HalfOf[s], HalfOf[k])

    extension [U <: Dim](q: Quantity[U])
      def value: Double = q
      def scale(k: Double): Quantity[U] = q * k
      def +(r: Quantity[U]): Quantity[U] = q + r
      def -(r: Quantity[U]): Quantity[U] = q - r
      def *[V <: Dim](r: Quantity[V]): Quantity[Mul[U, V]] = q * r
      def /[V <: Dim](r: Quantity[V]): Quantity[Div[U, V]] = q / r
      def sqrt(using Mul[Half[U], Half[U]] =:= U): Quantity[Half[U]] = math.sqrt(q)
  end units

  export units.*

  type Velocity = Div[Metre, Second]

  type Acceleration = Div[Velocity, Second]

  type Energy = Mul[Kilogram, Mul[Velocity, Velocity]]

  def metresPerSecond(d: Double): Quantity[Velocity] = quantity(d)
  def metresPerSecondSquared(d: Double): Quantity[Acceleration] = quantity(d)

  def speed(distance: Quantity[Metre], time: Quantity[Second]): Quantity[Velocity] =
    distance / time

  def kineticEnergy(mass: Quantity[Kilogram], speed: Quantity[Velocity]): Quantity[Energy] =
    (mass * (speed * speed)).scale(0.5)

  def fallTime(height: Quantity[Metre], gravity: Quantity[Acceleration]): Quantity[Second] =
    (height.scale(2) / gravity).sqrt
end Exercise03
