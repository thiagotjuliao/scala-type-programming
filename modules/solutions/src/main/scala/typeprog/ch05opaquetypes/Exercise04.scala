package typeprog.ch05opaquetypes

/** Exercise 04 — units that exist only at compile time. *Solved.*
  *
  * `opaque type Quantity[U] = Double` combines the chapter's two ideas. The
  * `opaque` keeps a `Double` from being a quantity, and a quantity from being a
  * `Double`, outside this object; the parameter `U` appears nowhere in the
  * representation, so it is a phantom — two units are two types, and at
  * runtime both are a bare `double`.
  *
  * With the alias opaque, `+` means the extension, and the extension takes a
  * `Quantity[U]` with the *same* `U`: metres plus seconds finds no method.
  * With the stub's transparent alias, `+` meant `Double.+`, which a member
  * method always wins over an extension for — so the unit check never ran at
  * all, and nothing warned about it.
  *
  * `per` returns `Quantity[U / V]`, building the unit of the result from the
  * two inputs with a type constructor that is never instantiated: `/` exists
  * only to be written in types, infix. `Quantity[Meters / Seconds]` and
  * `Quantity[Seconds / Meters]` are different types for the same reason
  * `Map[Int, String]` and `Map[String, Int]` are.
  *
  * What it does not do is algebra: `(Meters / Seconds) * Seconds` is not
  * `Meters` here. Reducing units needs computation on types — match types, in
  * chapter 07.
  */
object Exercise04:

  sealed trait Meters
  sealed trait Seconds

  /** The unit of a ratio. Never instantiated: written infix, `Meters / Seconds`. */
  sealed trait /[A, B]

  opaque type Quantity[U] = Double

  object Quantity:
    def of[U](amount: Double): Quantity[U] = amount

  extension [U](q: Quantity[U])
    def +(other: Quantity[U]): Quantity[U] = q + other
    def per[V](other: Quantity[V]): Quantity[U / V] = q / other
    def times(factor: Double): Quantity[U] = q * factor
    def value: Double = q
