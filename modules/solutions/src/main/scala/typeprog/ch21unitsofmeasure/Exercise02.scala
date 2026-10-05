package typeprog.ch21unitsofmeasure

import scala.compiletime.ops.int.{%, +, -, /}

/** Exercise 02 — a square root, only where the unit has one. *Solved.*
  *
  * `Half` halves each exponent through `HalfOf`, whose single case is `0`: it
  * matches when `N % 2` reduces to `0`, and then the result is `N / 2`. For an
  * odd `N` no case matches.
  *
  * That alone is the obvious version, and it is not enough: a match type that
  * no case matches is not an error. The compiler reports *"Match type
  * reduction failed since selector (1 : Int) matches none of the cases"* as a
  * warning and leaves `Half[Metre]` unreduced, so `metres(1).sqrt` compiles
  * and returns a `Quantity` of a unit nobody can name. The mistake only shows
  * up later, where that quantity meets one of a real unit.
  *
  * The `using` parameter turns it into an error at the call. It is the promise
  * a square root makes — its result, squared, is what it started from — asked
  * of the compiler as `=:=` evidence (chapter 01). For `(2, 0, 0)` both sides
  * reduce to `(2, 0, 0)` and the compiler supplies the proof; for `(1, 0, 0)`
  * the left side is stuck, the two are not the same type, and the call is
  * rejected with *"Cannot prove that ..."*.
  *
  * Generic code meets the same wall from the other side: in a method over an
  * abstract `U`, `Half[U]` cannot reduce at all, so `q.sqrt` does not compile
  * there unless the method asks its own caller for the same evidence and
  * passes it on.
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
end Exercise02
