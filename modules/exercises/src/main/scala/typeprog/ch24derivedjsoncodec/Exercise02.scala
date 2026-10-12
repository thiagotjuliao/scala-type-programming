package typeprog.ch24derivedjsoncodec

import scala.deriving.*
import scala.compiletime.*

/** Exercise 02 — derive a `Decoder`, the way back.
  *
  * A `Decoder` reads a `Json` and answers either the value or a `DecodeError`:
  * a message, and the path of field names that leads to the value that failed.
  * `Decoder` already has instances for `Int`, `String`, `Boolean`, and for
  * `Option[A]` given one for `A` — `JNull` is `None`.
  *
  *   - A case class is read from a `JObject`, each field by its name, in any
  *     order. Anything that is not an object fails with `"expected an
  *     object"`.
  *   - A field that is absent is decoded from `JNull`: an `Option` field is
  *     then `None`, and any other fails with `"missing"`.
  *   - A field that fails fails the whole value, with the field's name added
  *     in front of the path: `DecodeError(List("address", "city"),
  *     "missing")` for a `User` whose `address` has no `city`.
  *   - An enum is read from a `JObject` whose `"type"` names the case; the
  *     case's decoder reads the object. With no `"type"`, it fails as
  *     `DecodeError(List("type"), "missing")`; with an unknown one, as
  *     `DecodeError(List("type"), "unknown case Triangle")`.
  *   - Fields of a case class or enum type are derived on the way, and a
  *     recursive type works.
  *
  * As shipped, `derived` is `???`.
  *
  * Hint: the same walk as exercise 01, with `Decoder`s. Chapter 10's
  * `fromProduct` builds a case class from a tuple of its fields — and
  * `Tuple.fromArray` builds that tuple.
  */
object Exercise02:
  extension [E, A](either: Either[E, A])
    def leftMap[E2](f: E => E2): Either[E2, A] =
      either match
        case Left(e) => Left(f(e))
        case Right(v) => Right(v)

  enum Json:
    case JNull
    case JBool(value: Boolean)
    case JNumber(value: Double)
    case JString(value: String)
    case JArray(items: List[Json])
    case JObject(fields: List[(String, Json)])

  final case class DecodeError(path: List[String], message: String)

  trait Decoder[A]:
    def decode(json: Json): Either[DecodeError, A]

  object Decoder:

    private def fail(message: String) = Left(DecodeError(Nil, message))

    given Decoder[Int] =
      case Json.JNumber(n) if n.isWhole => Right(n.toInt)
      case _ => fail("expected a whole number")

    given Decoder[String] =
      case Json.JString(s) => Right(s)
      case _ => fail("expected a string")

    given Decoder[Boolean] =
      case Json.JBool(b) => Right(b)
      case _ => fail("expected a boolean")

    given [A] => (d: Decoder[A]) => Decoder[Option[A]] =
      case Json.JNull => Right(None)
      case json => d.decode(json).map(Some(_))

    inline def instance[A]: Decoder[A] = summonFrom {
      case d: Decoder[A] => d
      case m: Mirror.Of[A] => derived(using m)
    }

    inline def instances[Ts <: Tuple]: List[Decoder[Any]] =
      inline erasedValue[Ts] match
        case EmptyTuple => Nil
        case _: (h *: t) => instance[h].asInstanceOf[Decoder[Any]] :: instances[t]

    def field(fields: Map[String, Json], label: String, d: Decoder[Any]): Either[DecodeError, Any] =
      fields.get(label) match
        case Some(j) => d.decode(j).leftMap(e => DecodeError(label :: e.path, e.message))
        case None => d.decode(Json.JNull).leftMap(_ => DecodeError(List(label), "missing"))

    def decodeProduct[A](
        m: Mirror.ProductOf[A],
        names: List[String],
        decoders: => List[Decoder[Any]]
    ): Decoder[A] =
      lazy val decs = decoders

      json =>
        json match
          case Json.JObject(fields) =>
            names
              .zip(decs)
              .foldRight[Either[DecodeError, List[Any]]](Right(Nil)):
                case ((label, d), rest) =>
                  for
                    value <- field(fields.toMap, label, d)
                    values <- rest
                  yield value :: values
              .map(as => m.fromProduct(Tuple.fromArray(as.toArray)))

          case _ => Left(DecodeError(Nil, "expected an object"))

    def decodeSum[A](
        m: Mirror.SumOf[A],
        names: List[String],
        decoders: => List[Decoder[Any]]
    ): Decoder[A] =
      lazy val decs = decoders

      json =>
        json match
          case Json.JObject(fields) =>
            fields.filter(_._1 == "type") match
              case (tpe -> Json.JString(name)) :: _ =>
                val i = names.indexOf(name)

                if i >= 0 then decs(i).decode(json).map(_.asInstanceOf[A])
                else Left(DecodeError(List("type"), s"unknown case $name"))

              case _ => Left(DecodeError(List("type"), "missing"))
          case _ => Left(DecodeError(Nil, "expected an object"))

    inline def derived[A](using m: Mirror.Of[A]): Decoder[A] =
      val names = constValueTuple[m.MirroredElemLabels].toList.map(_.toString)
      lazy val decoders = instances[m.MirroredElemTypes]

      inline m match
        case p: Mirror.ProductOf[A] => decodeProduct(p, names, decoders)
        case s: Mirror.SumOf[A] => decodeSum(s, names, decoders)
  end Decoder
end Exercise02
