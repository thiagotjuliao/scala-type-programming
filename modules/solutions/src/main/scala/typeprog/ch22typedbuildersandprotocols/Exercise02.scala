package typeprog.ch22typedbuildersandprotocols

import scala.annotation.implicitNotFound

/** Exercise 02 — `build` only when every required field is set. *Solved.*
  *
  * `AllIn` takes the required keys one at a time and asks `Contains` about
  * each; a nested match on its answer either recurses or stops at `false`.
  * Order does not matter because `Contains` searches the whole of `S`.
  *
  * `build` could ask for `AllIn[Required, S] =:= true` directly, and it would
  * be correct — and the error would read *"Cannot prove that AllIn[...] =:=
  * (true : Boolean)"*. `Complete` is that same evidence behind a name of its
  * own, so that it can carry an `@implicitNotFound` message: its only given
  * asks for the `=:=`, and when that fails, the search for a `Complete` fails
  * with the sentence written here. `${S}` in it prints the keys set so far.
  *
  * The values come out of the map with a cast. It is safe, and contained in
  * this one method: `set` only accepts a value of the type `Lookup` gives its
  * key, and `build` only runs once `name` and `email` are known to be in the
  * map — which is what the evidence proves. `age` is read with `get`, because
  * nothing promises it.
  */
object Exercise02:

  type Schema = (("name", String), ("email", String), ("age", Int))

  type Lookup[S <: Tuple, K] = S match
    case (K, v) *: _ => v
    case _ *: rest => Lookup[rest, K]

  type Contains[T <: Tuple, K] <: Boolean = T match
    case EmptyTuple => false
    case K *: _ => true
    case _ *: rest => Contains[rest, K]

  type Required = ("name", "email")

  type AllIn[R <: Tuple, S <: Tuple] <: Boolean = R match
    case EmptyTuple => true
    case k *: rest =>
      Contains[S, k] match
        case true => AllIn[rest, S]
        case false => false

  @implicitNotFound("cannot build a User: it needs name and email, and has only ${S}")
  final class Complete[S <: Tuple] private ()

  object Complete:
    given [S <: Tuple] => (AllIn[Required, S] =:= true) => Complete[S] = new Complete

  final case class User(name: String, email: String, age: Option[Int])

  final class Builder[S <: Tuple] private (val values: Map[String, Any]):

    def set[K <: String & Singleton](key: K)(value: Lookup[Schema, K])(using
        Contains[S, K] =:= false
    ): Builder[K *: S] =
      new Builder(values.updated(key, value))

    def build(using Complete[S]): User =
      User(
        values("name").asInstanceOf[String],
        values("email").asInstanceOf[String],
        values.get("age").map(_.asInstanceOf[Int])
      )

  object Builder:
    def apply(): Builder[EmptyTuple] = new Builder(Map.empty)
end Exercise02
