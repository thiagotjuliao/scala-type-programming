package typeprog.ch22typedbuildersandprotocols

import scala.annotation.implicitNotFound

/** Exercise 02 — `build` only when every required field is set.
  *
  * `User` has two required fields, `name` and `email`, listed in `Required`,
  * and an optional one, `age`.
  *
  *   - `AllIn[R, S]` is `true` when every element of `R` is an element of `S`,
  *     whatever the order, and `false` otherwise. It is `true` for an empty
  *     `R`.
  *   - `build` compiles only when `AllIn[Required, S]` is `true`. When it does
  *     not, the compile error starts with `cannot build a User` — not with the
  *     compiler's *"Cannot prove that ..."*.
  *   - It builds the `User` from the values set: `age` is `Some` when it was
  *     set and `None` when it was not.
  *
  * Exercise 01's builder is given, solved. `AllIn` is `Boolean` in the stub
  * only so that it compiles.
  *
  * Hint: `AllIn` can be written with `Contains`, one required key at a time.
  * For the message, chapter 11: evidence of your own, with an annotation that
  * says what to print when it is missing.
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

  type AllIn[R <: Tuple, S <: Tuple] = R match
    case EmptyTuple => true
    case h *: t =>
      Contains[S, h] match
        case true => AllIn[t, S]
        case _ => false

  final case class User(name: String, email: String, age: Option[Int])

  final class Builder[S <: Tuple] private (val values: Map[String, Any]):

    def set[K <: String & Singleton](key: K)(value: Lookup[Schema, K])(using
        Contains[S, K] =:= false
    ): Builder[K *: S] =
      new Builder(values.updated(key, value))

    def build(using Builder.Completed[S]): User =
      User(
        values("name").asInstanceOf[Lookup[Schema, "name"]],
        values("email").asInstanceOf[Lookup[Schema, "email"]],
        values.get("age").map(_.asInstanceOf[Lookup[Schema, "age"]])
      )

  object Builder:
    @implicitNotFound("cannot build a User - missing required fields")
    sealed trait Completed[S <: Tuple]

    given [S <: Tuple] => (AllIn[Required, S] =:= true) => Completed[S] = new Completed[S] {}

    def apply(): Builder[EmptyTuple] = new Builder(Map.empty)
end Exercise02
