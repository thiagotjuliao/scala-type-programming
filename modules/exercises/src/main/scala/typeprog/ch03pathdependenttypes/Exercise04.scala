package typeprog.ch03pathdependenttypes

/** Exercise 04 — repair three keys that forgot their type.
  *
  * Each declaration below builds a perfectly good key and then loses track of
  * what its value is, each in a different way. None of them fails where it is
  * written; they fail at the first use that needs the type:
  *
  *   - `port.Value` should be `Int`;
  *   - `host.Value` should be `String` — and before that, `host.Value` should
  *     be something the compiler accepts at all;
  *   - a key made by `keyOf[V]` should have `V` as its value type, including
  *     when `V` is inferred from the parser.
  *
  * Change the declarations, not what the keys do: every key still parses as
  * it does now.
  *
  * Hint: a type is only as precise as the one written down for it; and only a
  * stable path can carry a type member.
  */
object Exercise04:

  /** TODO: its value type is `Int`. */
  val port: Key = Key[Int]("port")(_.toIntOption)

  /** TODO: its value type is `String`. */
  def host: Key.Aux[String] = Key[String]("host")(Some(_))

  /** TODO: a key made here should keep `V`. */
  def keyOf[V](name: String)(parse: String => Option[V]): Key = Key[V](name)(parse)
