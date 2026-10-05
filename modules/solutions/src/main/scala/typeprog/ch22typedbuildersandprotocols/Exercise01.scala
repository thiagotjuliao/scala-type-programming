package typeprog.ch22typedbuildersandprotocols

/** Exercise 01 — a builder typed by its schema. *Solved.*
  *
  * `Lookup` walks the schema one pair at a time. Its first case is a pattern
  * that mentions `K` itself: `(K, v) *: _` matches only a pair whose key is the
  * very type being looked for, and binds the value's type to `v`. Otherwise
  * the second case drops the head and recurses on the rest — chapter 09's walk
  * down a tuple, with chapter 07's reduction doing the work.
  *
  * A key the schema lacks runs out of pairs. `Lookup` has no case for
  * `EmptyTuple`, so it does not reduce, and the value — a `String`, say — is
  * then checked against a type nothing conforms to: the call is rejected, at
  * the value. The compiler also *warns* that the match type could not be
  * reduced; the error is what stops the build.
  *
  * `Contains` is the same walk with a `Boolean` answer, and it does need its
  * `EmptyTuple => false` case: `set` asks for `Contains[S, K] =:= false`, and
  * on a builder with nothing set yet `S` is `EmptyTuple`. Without that case,
  * `Contains[EmptyTuple, K]` would be stuck, would not be `false`, and no first
  * `set` would ever compile. The `<: Boolean` bound says what its cases
  * produce.
  *
  * `K <: String & Singleton` is what keeps the key's literal type. With
  * `K <: String` alone, `"age"` is inferred as `String`, and `Lookup[Schema,
  * String]` matches the *first* pair — `"name"` is a `String` — so
  * `set("age")(36)` is rejected with *"Found: (36 : Int) Required: String"*.
  * A pattern in a match type matches by subtyping, not by equality.
  */
object Exercise01:

  type Schema = (("name", String), ("email", String), ("age", Int))

  type Lookup[S <: Tuple, K] = S match
    case (K, v) *: _ => v
    case _ *: rest => Lookup[rest, K]

  type Contains[T <: Tuple, K] <: Boolean = T match
    case EmptyTuple => false
    case K *: _ => true
    case _ *: rest => Contains[rest, K]

  final class Builder[S <: Tuple] private (val values: Map[String, Any]):

    def set[K <: String & Singleton](key: K)(value: Lookup[Schema, K])(using
        Contains[S, K] =:= false
    ): Builder[K *: S] =
      new Builder(values.updated(key, value))

  object Builder:
    def apply(): Builder[EmptyTuple] = new Builder(Map.empty)
