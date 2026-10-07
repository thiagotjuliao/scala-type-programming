package typeprog.ch22typedbuildersandprotocols

/** Exercise 01 — a builder typed by its schema.
  *
  * `Schema` lists a record's fields as pairs of a key and the type of its
  * value. `Builder[S]` collects values for them; `S` is the tuple of keys set
  * so far, newest first, and every `set` adds one.
  *
  *   - `Lookup[S, K]` is the type of `K`'s value in the schema `S`, and
  *     `set(key)(value)` takes exactly that type: `set("age")(36)` compiles,
  *     `set("age")("old")` does not, and neither does a key the schema lacks.
  *   - `Contains[T, K]` is `true` when `K` is one of the elements of `T`, and
  *     `false` otherwise — `false` for `EmptyTuple`.
  *   - A key can be set once: setting `"name"` a second time does not compile.
  *     Keys can be set in any order.
  *
  * The runtime side is already here and is not the exercise: values go into a
  * `Map`, which `values` exposes.
  *
  * `Lookup` is `Any` and `Contains` is `Boolean` in the stub only so that it
  * compiles: they are the wrong answers that let everything through.
  *
  * Hint: chapters 07 and 09 for walking a tuple in a match type — a case can
  * match a pattern that mentions `K`, the key being looked for. Chapter 11 for
  * a method that compiles only when a fact about its types holds.
  */
object Exercise01:

  type Schema = (("name", String), ("email", String), ("age", Int))

  type Lookup[S <: Tuple, K] = S match
    case EmptyTuple => Nothing
    case ((n *: v *: EmptyTuple) *: t) =>
      n match
        case K => v
        case _ => Lookup[t, K]

  type Contains[T <: Tuple, K] = T match
    case EmptyTuple => false
    case K *: _ => true
    case _ *: t => Contains[t, K]

  final class Builder[S <: Tuple] private (val values: Map[String, Any]):

    def set[K <: String & Singleton](key: K)(value: Lookup[Schema, K])(using
        Contains[S, K] =:= false
    ): Builder[K *: S] =
      new Builder(values.updated(key, value))

  object Builder:
    def apply(): Builder[EmptyTuple] = new Builder(Map.empty)
end Exercise01
