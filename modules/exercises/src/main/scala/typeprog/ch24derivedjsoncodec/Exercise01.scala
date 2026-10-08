package typeprog.ch24derivedjsoncodec

import scala.deriving.Mirror

/** Exercise 01 — derive an `Encoder` for case classes and enums.
  *
  * `Json` is the value an `Encoder` produces; turning it into text is not part
  * of the exercise. `Encoder` already has instances for `Int`, `Double`,
  * `String`, `Boolean`, and for `Option[A]` and `List[A]` given one for `A`.
  * `derives Encoder` puts `Encoder.derived` in a type's companion.
  *
  *   - A case class is a `JObject` with one entry per field, in the order of
  *     the fields, keyed by the field's name: `User("Ada", 36)` is
  *     `JObject(List("name" -> JString("Ada"), "age" -> JNumber(36)))`.
  *   - A field whose type has an `Encoder` uses it. One whose type has none,
  *     but is itself a case class or an enum, is derived on the way — it does
  *     not need a `derives` of its own.
  *   - An enum case is its own encoding, with an entry `"type" ->
  *     JString(caseName)` in front: `Circle(2)` is `JObject(List("type" ->
  *     JString("Circle"), "radius" -> JNumber(2)))`, and a case with no
  *     parameters is just its `"type"`.
  *   - A recursive type, such as a tree whose nodes hold trees, works.
  *
  * As shipped, `derived` is `???`: every `derives Encoder` compiles, and every
  * encoding throws.
  *
  * Hint: chapter 10, all of it — the mirror's labels and element types, read
  * with chapter 08's `constValueTuple` and `erasedValue`; an instance found or
  * derived with `summonFrom`; the instances passed by name, for the recursive
  * type; `ordinal` for an enum's case.
  */
object Exercise01:

  enum Json:
    case JNull
    case JBool(value: Boolean)
    case JNumber(value: Double)
    case JString(value: String)
    case JArray(items: List[Json])
    case JObject(fields: List[(String, Json)])

  trait Encoder[A]:
    def encode(a: A): Json

  object Encoder:

    given Encoder[Int] = n => Json.JNumber(n)
    given Encoder[Double] = Json.JNumber(_)
    given Encoder[String] = Json.JString(_)
    given Encoder[Boolean] = Json.JBool(_)
    given [A] => (e: Encoder[A]) => Encoder[Option[A]] = _.fold(Json.JNull)(e.encode)
    given [A] => (e: Encoder[A]) => Encoder[List[A]] = as => Json.JArray(as.map(e.encode))

    /** TODO: an encoder for any case class or enum. */
    inline def derived[A](using m: Mirror.Of[A]): Encoder[A] = ???
  end Encoder
end Exercise01
