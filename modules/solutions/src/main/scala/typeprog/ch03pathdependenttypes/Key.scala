package typeprog.ch03pathdependenttypes

/** A named setting that knows the type of its own value — the abstraction
  * exercises 02 to 04 are stated against.
  *
  * `Value` is a type *member*, not a parameter, on purpose: a `Key` can be
  * passed around, stored in a `List[Key]`, and still be asked for its value
  * type through a path, `k.Value`. `parse` is there so a key can turn raw text
  * into that type without anyone else knowing what it is.
  */
trait Key:
  type Value
  def name: String
  def parse(raw: String): Option[Value]

object Key:

  /** A key whose value type is known: the refinement from chapter 01, named. */
  type Aux[V] = Key { type Value = V }

  def apply[V](keyName: String)(parser: String => Option[V]): Key.Aux[V] = new Key:
    type Value = V
    def name: String = keyName
    def parse(raw: String): Option[V] = parser(raw)
