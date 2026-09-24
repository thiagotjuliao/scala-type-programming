package typeprog.ch03pathdependenttypes

/** Exercise 04 — repair three keys that forgot their type. *Solved.*
  *
  * Three ways to lose a type member, and each has a different repair.
  *
  * `port` was ascribed `Key`. `Key[Int](...)` returns a `Key.Aux[Int]` —
  * `Key { type Value = Int }` — and the ascription widened it to the trait,
  * which knows only that there *is* a `Value`. The path was fine; the type
  * behind it was gone, and the first use reported `port.Value` where `Int` was
  * expected. Ascribe the refinement, as here, or write no type at all: the
  * inferred one is already the refinement.
  *
  * `host` had the right type and the wrong kind of definition. A `def` is
  * evaluated on every call and may return a different key each time, so it is
  * not a stable path and `host.Value` is rejected outright — *"not a valid type
  * prefix, since it is not an immutable path"*. Making it a `val` fixes that
  * and nothing else; its type was never the problem.
  *
  * `keyOf` is the same widening as `port`, in a result type. Every key it made
  * was a `Key.Aux[V]` inside and a `Key` to the caller. Declaring the result as
  * `Key.Aux[V]` lets the refinement through, and `V` itself still comes from
  * inference when the caller leaves it out: `keyOf("retries")(_.toIntOption)`
  * reads `V = Int` off the parser. A `def` is fine *here*: nobody selects a
  * type through `keyOf` itself, only through the `val` its result is bound to.
  */
object Exercise04:

  val port: Key.Aux[Int] = Key[Int]("port")(_.toIntOption)

  val host: Key.Aux[String] = Key[String]("host")(Some(_))

  def keyOf[V](name: String)(parse: String => Option[V]): Key.Aux[V] = Key[V](name)(parse)
