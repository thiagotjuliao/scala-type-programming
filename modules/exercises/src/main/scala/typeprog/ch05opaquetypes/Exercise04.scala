package typeprog.ch05opaquetypes

/** Exercise 04 — units that exist only at compile time.
  *
  * A `Quantity[U]` is a number of `U`s: metres, seconds, metres per second. As
  * shipped it is a plain alias for `Double`, so the unit is decoration —
  * `metres + seconds` compiles, through `Double`'s own `+`, and a bare `3.0` is
  * a `Quantity` of anything. Make the unit mean something:
  *
  *   - outside `Exercise04` a `Quantity[U]` is neither a `Double` nor made from
  *     one, except through `Quantity.of`, and two units are two types;
  *   - `+` adds two quantities of the *same* unit, and keeps it;
  *   - `per` divides a `Quantity[U]` by a `Quantity[V]` and gives a
  *     `Quantity[U / V]` — metres per second is `Meters / Seconds`;
  *   - `times` scales by a plain `Double` and keeps the unit;
  *   - `value` reads the number back.
  *
  * At runtime every quantity must still be a bare `Double`: no wrapper, and
  * no unit anywhere but in the types. `Meters`, `Seconds` and `/` are never
  * instantiated.
  *
  * Hint: an opaque type can take type parameters, and nothing obliges them to
  * appear in its representation.
  */
object Exercise04:

  sealed trait Meters
  sealed trait Seconds

  /** The unit of a ratio. Never instantiated: written infix, `Meters / Seconds`. */
  sealed trait /[A, B]

  /** TODO: make the unit part of the type outside this object. */
  opaque type Quantity[U] = Double

  object Quantity:
    def of[U](amount: Double): Quantity[U] = amount

  extension [U](q: Quantity[U])

    /** TODO: implement. */
    def +(other: Quantity[U]): Quantity[U] =
      q + other

    /** TODO: the result type, and the body. */
    def per[V](other: Quantity[V]): Quantity[U / V] =
      Quantity.of(q / other)

    /** TODO: implement. */
    def times(factor: Double): Quantity[U] =
      q * factor

    /** TODO: implement. */
    def value: Double = q
end Exercise04
