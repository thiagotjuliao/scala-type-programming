package typeprog.ch03pathdependenttypes

/** Exercise 02 — type a settings store by the key it is handed.
  *
  * `Settings` keeps its values in a `Map[String, Any]`, keyed by the `Key`'s
  * name, and as shipped it says exactly that much: `set` takes `Any`, `get`
  * returns `Option[Any]`, and every caller has to cast. Give both signatures
  * the type the key already knows:
  *
  *   - `set(k)(v)` accepts only a `v` of `k`'s value type — a `String` for the
  *     port key is rejected;
  *   - `get(k)` returns an `Option` of `k`'s value type, so reading the port key
  *     gives an `Option[Int]` with no cast at the call site;
  *   - both work for a key known only as `Key`, whose value type is abstract: a
  *     value read with `k` can be written back with `k`.
  *
  * Then implement them. `set` replaces an existing value; `get` of a key that
  * was never set is `None`. The one cast the store needs goes inside `get`,
  * once, instead of at every call site.
  *
  * Hint: a method parameter is a stable path, and a later parameter list or the
  * result type can select a type member through it.
  */
object Exercise02:

  final class Settings(values: Map[String, Any]):

    /** TODO: the type of `v`, and the body. */
    def set(k: Key)(v: k.Value): Settings =
      Settings(values.updated(k.name, v))

    /** TODO: the result type, and the body. */
    def get(k: Key): Option[k.Value] =
      values.get(k.name).flatMap(v => k.parse(v.toString))

  object Settings:
    val empty: Settings = Settings(Map.empty)
