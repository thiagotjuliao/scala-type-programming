package typeprog.ch21unitsofmeasure

import scala.compiletime.ops.int.{+, -}

/** Exercise 02 — a square root, only where the unit has one.
  *
  * The square root of m² is m: it halves every exponent. The square root of m
  * has no unit at all, and should not compile.
  *
  *   - `Half[U]` halves each exponent of `U`, when it is even.
  *   - `sqrt` returns a quantity of the halved unit, and compiles only for a
  *     unit whose exponents are all even: `(metres(3) * metres(3)).sqrt` is a
  *     `Quantity[Metre]`, and `metres(1).sqrt` is a compile error.
  *
  * A compile *error*, not a warning. Try the obvious version first, and read
  * what the compiler says about `metres(1).sqrt`: it is the point of the
  * exercise.
  *
  * `Half` is `Nothing` in the stub only because `Quantity` wants a `Dim`, which
  * `Unsolved` is not. Exercise 01 is given, solved.
  *
  * Hint: chapter 07 for halving, with `/` and `%` from `compiletime.ops.int`.
  * Then chapter 01 — what does a correct square root promise about its result,
  * squared? The compiler can be asked for proof of a fact like that.
  */
object Exercise02:

  type Dim = (Int, Int, Int)

  type Metre = (1, 0, 0)
  type Second = (0, 1, 0)
  type Kilogram = (0, 0, 1)
  type Dimensionless = (0, 0, 0)

  opaque type Quantity[U <: Dim] = Double

  def metres(d: Double): Quantity[Metre] = d
  def seconds(d: Double): Quantity[Second] = d
  def kilograms(d: Double): Quantity[Kilogram] = d

  type Mul[A <: Dim, B <: Dim] <: Dim = (A, B) match
    case ((m1, s1, k1), (m2, s2, k2)) => (m1 + m2, s1 + s2, k1 + k2)

  type Div[A <: Dim, B <: Dim] <: Dim = (A, B) match
    case ((m1, s1, k1), (m2, s2, k2)) => (m1 - m2, s1 - s2, k1 - k2)

  /** TODO: every exponent halved. */
  type Half[U <: Dim] = Nothing

  extension [U <: Dim](q: Quantity[U])
    def value: Double = q
    def scale(k: Double): Quantity[U] = q * k
    def +(r: Quantity[U]): Quantity[U] = q + r
    def -(r: Quantity[U]): Quantity[U] = q - r
    def *[V <: Dim](r: Quantity[V]): Quantity[Mul[U, V]] = q * r
    def /[V <: Dim](r: Quantity[V]): Quantity[Div[U, V]] = q / r

    /** TODO: implement, for units with every exponent even. */
    def sqrt: Quantity[Half[U]] = ???
end Exercise02
