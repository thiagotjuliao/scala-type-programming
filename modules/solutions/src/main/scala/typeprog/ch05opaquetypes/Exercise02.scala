package typeprog.ch05opaquetypes

/** Exercise 02 — let a Port be read as an Int, and not made from one. *Solved.*
  *
  * `opaque type Port <: Int = Int` publishes one fact about the representation
  * and keeps the rest. The bound is visible everywhere, so outside `Exercise02`
  * a `Port` conforms to `Int`: it can be passed as one, and `Port.Http + 1`
  * resolves `+` on `Int` and gives an `Int`. The *equation* `Port = Int` stays
  * private, so the converse does not hold: `val p: Port = 8080` is rejected,
  * and the only ways to get a `Port` are the ones this object offers.
  *
  * The arithmetic result being an `Int` rather than a `Port` is correct, not a
  * limitation: `Port.Http + 70000` is no port. A result that should stay a
  * port has to come back through `from`.
  *
  * Compared with `opaque type Port = Int` plus an extension `def toInt: Int`,
  * the bound saves the boilerplate for the direction that is always safe, and
  * the subtyping is checked by the compiler rather than written by hand.
  */
object Exercise02:

  opaque type Port <: Int = Int

  object Port:
    val Http: Port = 80
    val Https: Port = 443

    def from(n: Int): Option[Port] = Option.when(n >= 1 && n <= 65535)(n)
