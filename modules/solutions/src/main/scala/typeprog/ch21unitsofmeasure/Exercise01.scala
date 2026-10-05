package typeprog.ch21unitsofmeasure

import scala.compiletime.ops.int.{+, -}

/** Exercise 01 — multiply and divide, and let the compiler work out the unit.
  * *Solved.*
  *
  * `Mul` is a match type over the pair of units. Its one case takes both
  * tuples apart into six exponents and puts them back together summed; the
  * `+` in it is `compiletime.ops.int.+`, a type that reduces to a literal when
  * both its arguments are literals. So `Mul[Metre, Second]` becomes
  * `(1 + 0, 0 + 1, 0 + 0)` and then `(1, 1, 0)`, while the call is being
  * type-checked. `Div` is the same with `-`.
  *
  * The `<: Dim` after the parameter list is not decoration. `Quantity` takes
  * only a `Dim`, and in the signature of `*` the unit is `Mul[U, V]` for an
  * abstract `U` and `V`, which cannot be reduced: without a declared bound the
  * compiler has no way to know the result is a `Dim`, and the signature does
  * not compile. The bound is a promise, checked against the case's right-hand
  * side.
  *
  * Cancellation is not a rule anywhere. `Div[Metre, Second]` is `(1, -1, 0)`;
  * multiplying by `Second` gives `(1 + 0, -1 + 1, 0 + 0)`, which is `(1, 0, 0)`
  * — the same type as `Metre`, because two tuples of literal types are equal
  * when their elements are. That is what chapter 05's names could not do.
  *
  * The bodies are one `Double` operation each: inside this object the opaque
  * type is its representation, so `q * r` is `Double`'s own `*`, not a call
  * back into this extension.
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

  type Mul[A <: Dim, B <: Dim] <: Dim = (A, B) match
    case ((m1, s1, k1), (m2, s2, k2)) => (m1 + m2, s1 + s2, k1 + k2)

  type Div[A <: Dim, B <: Dim] <: Dim = (A, B) match
    case ((m1, s1, k1), (m2, s2, k2)) => (m1 - m2, s1 - s2, k1 - k2)

  extension [U <: Dim](q: Quantity[U])
    def value: Double = q
    def scale(k: Double): Quantity[U] = q * k
    def +(r: Quantity[U]): Quantity[U] = q + r
    def -(r: Quantity[U]): Quantity[U] = q - r

    def *[V <: Dim](r: Quantity[V]): Quantity[Mul[U, V]] = q * r
    def /[V <: Dim](r: Quantity[V]): Quantity[Div[U, V]] = q / r
end Exercise01
