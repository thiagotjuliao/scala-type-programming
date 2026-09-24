package typeprog.ch03pathdependenttypes

/** Exercise 02 — type a settings store by the key it is handed. *Solved.*
  *
  * `k` is a parameter, so `k.Value` is a type the rest of the signature can
  * use: `set`'s second parameter list asks for one, and `get` returns an
  * `Option` of one. Nothing about the *store* changed — it is still a
  * `Map[String, Any]` — only what its signatures promise. At the call site the
  * compiler replaces `k` with the actual key and reads `Value` off that key's
  * type: for `port: Key.Aux[Int]` it is `Int`, so `get(port)` is an
  * `Option[Int]` and `set(port)("8080")` is rejected.
  *
  * For a key known only as `Key`, `k.Value` is abstract, and that is still
  * enough. `from.get(k)` is an `Option[k.Value]` and `to.set(k)` wants a
  * `k.Value`: the same type, because it is the same path, although nobody
  * knows what it is. A store typed `Any` in and `Any` out loses exactly this —
  * it would have to be told the type to hand it back.
  *
  * The cast in `get` is the one place the type system is taken on trust, and
  * it is only as sound as the keys: two keys with the same name and different
  * value types would read each other's values, and the `ClassCastException`
  * would surface far from here, at the first use. Keeping the cast inside, once,
  * is what makes that the only place to look.
  *
  * The generic signature `def get[V](k: Key.Aux[V]): Option[V]` works here too
  * — for an abstract key the compiler infers `V = k.Value` — which is the point
  * of the chapter's last section: a type parameter and a type member are two
  * spellings of the same idea. The member one is simply the one a `Key` can
  * carry around without anybody writing `V`.
  */
object Exercise02:

  final class Settings(values: Map[String, Any]):

    def set(k: Key)(v: k.Value): Settings = Settings(values + (k.name -> v))

    def get(k: Key): Option[k.Value] = values.get(k.name).map(_.asInstanceOf[k.Value])

  object Settings:
    val empty: Settings = Settings(Map.empty)
