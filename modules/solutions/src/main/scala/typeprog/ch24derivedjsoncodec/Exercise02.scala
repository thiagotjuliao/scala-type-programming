package typeprog.ch24derivedjsoncodec

import scala.compiletime.{constValueTuple, erasedValue, summonFrom}
import scala.deriving.Mirror

/** Exercise 02 — derive a `Decoder`, the way back. *Solved.*
  *
  * The walk is exercise 01's: labels from the mirror, one decoder per element
  * type, found or derived, passed by name so that a recursive type can find
  * itself. What changes is the direction, and that every step can fail.
  *
  * `product` reads each field by its label from a map of the object's
  * entries, so the order in the JSON does not matter. A field that is absent
  * is decoded from `JNull` — which the `Option` decoder turns into `None`,
  * and every other decoder rejects, reported as `"missing"`. A field whose
  * decoder fails has its label put in front of the error's path; a nested
  * case class does the same on its way out, so the path is built from the
  * inside, one field at a time, and ends up reading from the outside in:
  * `List("address", "city")`.
  *
  * The `foldRight` over `Either` stops at the first failure — a decoder that
  * gathered every error would be chapter 20's `Validated`. The decoded values
  * come out as a `List[Any]` in the mirror's order; `Tuple.fromArray` makes
  * them a tuple, and `fromProduct` the case class (chapter 10). The cast in
  * between is the price of the list of different types, and holds because the
  * i-th value came from the i-th field's decoder.
  *
  * `sum` finds the case by its name — the `"type"` entry — among the mirror's
  * labels, and lets that case's decoder read the whole object; the `"type"`
  * entry is one field it simply does not ask for.
  */
object Exercise02:

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

    inline def derived[A](using m: Mirror.Of[A]): Decoder[A] = inline m match
      case p: Mirror.ProductOf[A] =>
        product(p, labels[p.MirroredElemLabels], instances[p.MirroredElemTypes])
      case s: Mirror.SumOf[A] =>
        sum(labels[s.MirroredElemLabels], instances[s.MirroredElemTypes])

    inline def labels[Ls <: Tuple]: List[String] =
      constValueTuple[Ls].toList.asInstanceOf[List[String]]

    inline def instances[Ts <: Tuple]: List[Decoder[Any]] = inline erasedValue[Ts] match
      case _: EmptyTuple => Nil
      case _: (h *: ts) => instance[h] :: instances[ts]

    inline def instance[H]: Decoder[Any] = summonFrom {
      case d: Decoder[H] => d.asInstanceOf[Decoder[Any]]
      case m: Mirror.Of[H] => derived[H](using m).asInstanceOf[Decoder[Any]]
    }

    private def field(
        fields: Map[String, Json],
        label: String,
        d: Decoder[Any]
    ): Either[DecodeError, Any] =
      fields.get(label) match
        case Some(json) => d.decode(json).left.map(e => e.copy(path = label :: e.path))
        case None => d.decode(Json.JNull).left.map(_ => DecodeError(List(label), "missing"))

    def product[A](
        p: Mirror.ProductOf[A],
        labels: List[String],
        decoders: => List[Decoder[Any]]
    ): Decoder[A] =
      case Json.JObject(entries) =>
        val fields = entries.toMap
        labels
          .zip(decoders)
          .foldRight[Either[DecodeError, List[Any]]](Right(Nil)) { case ((label, d), rest) =>
            for
              value <- field(fields, label, d)
              values <- rest
            yield value :: values
          }
          .map(values =>
            p.fromProduct(Tuple.fromArray(values.toArray[Any].asInstanceOf[Array[Object]]))
          )
      case _ => fail("expected an object")

    def sum[A](labels: List[String], decoders: => List[Decoder[Any]]): Decoder[A] =
      case json @ Json.JObject(entries) =>
        entries.collectFirst { case ("type", Json.JString(name)) => name } match
          case None => Left(DecodeError(List("type"), "missing"))
          case Some(name) =>
            labels.indexOf(name) match
              case -1 => Left(DecodeError(List("type"), s"unknown case $name"))
              case i => decoders(i).decode(json).map(_.asInstanceOf[A])
      case _ => fail("expected an object")
  end Decoder
end Exercise02
