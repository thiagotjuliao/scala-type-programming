package typeprog.ch21unitsofmeasure

import scala.compiletime.ops.int.{+, -, /, %}

/** Exercise 03 — formulas the compiler checks.
  *
  * The library is complete, from exercise 02, in `units`. Use it:
  *
  *   - name three units — `Velocity`, `Acceleration` and `Energy` — by building
  *     them from units that already exist rather than writing their exponents
  *     out;
  *   - write three formulas against them: the speed of covering a distance in
  *     a time; kinetic energy, ½·m·v²; and the time an object takes to fall
  *     from a height, √(2h / g).
  *
  * Each formula has its unit in its signature, so a wrong one does not
  * compile. Get one wrong on purpose — `√(h · g)`, say — and read what the
  * compiler says.
  *
  * The units are `Nothing` in the stub so that the signatures compile.
  *
  * Hint: an acceleration is a speed per second, and an energy has the unit of
  * a mass times a speed squared.
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

  type Acceleration = Div[Metre, Mul[Second, Second]]

  type Energy = Mul[Kilogram, Mul[Metre, Acceleration]]

  def metresPerSecond(d: Double): Quantity[Velocity] = quantity(d)
  def metresPerSecondSquared(d: Double): Quantity[Acceleration] = quantity(d)

  def speed(distance: Quantity[Metre], time: Quantity[Second]): Quantity[Velocity] =
    distance / time

  def kineticEnergy(mass: Quantity[Kilogram], speed: Quantity[Velocity]): Quantity[Energy] =
    (mass * speed * speed).scale(0.5)

  def fallTime(height: Quantity[Metre], gravity: Quantity[Acceleration]): Quantity[Second] =
    sqrt((height / gravity).scale(2.0))
end Exercise03
