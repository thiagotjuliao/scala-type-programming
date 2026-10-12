package typeprog.ch24derivedjsoncodec

import scala.compiletime.*
import scala.deriving.Mirror
import scala.annotation.implicitNotFound

/** Exercise 03 — say what cannot be encoded, and where.
  *
  * Exercise 01's encoder is given, solved — for every type it can encode. A
  * type with a field it cannot encode, `Event(name: String, at:
  * java.time.Instant)`, fails to compile with a message about `summonFrom`
  * that names neither the type nor the field.
  *
  *   - `Encoder.derived[Event]` fails with a message that starts `cannot
  *     derive Encoder for Event: field at has no Encoder` — the type being
  *     derived, and the field.
  *   - The same, when `Event` is reached as a field of the type being derived:
  *     the message names `Event` and `at`, where the problem is.
  *   - A plain `summon[Encoder[java.time.Instant]]` fails with a message that
  *     starts `no Encoder for java.time.Instant`.
  *   - Everything that encoded before encodes the same.
  *
  * Hint: chapter 08 — the last case of a `summonFrom`, and `error`, whose
  * message must be a constant: `constValue` of a literal type is one, and a
  * concatenation of constants is folded. The labels are in the mirror; the
  * walk has to carry them to where the field's instance is looked for.
  * Chapter 06 for the plain `summon`.
  */
object Exercise03:

  enum Json:
    case JNull
    case JBool(value: Boolean)
    case JNumber(value: Double)
    case JString(value: String)
    case JArray(items: List[Json])
    case JObject(fields: List[(String, Json)])

  @implicitNotFound("no Encoder for ${A}")
  trait Encoder[A]:
    def encode(a: A): Json

  object Encoder:

    given Encoder[Int] = n => Json.JNumber(n)
    given Encoder[Double] = Json.JNumber(_)
    given Encoder[String] = Json.JString(_)
    given Encoder[Boolean] = Json.JBool(_)
    given [A] => (e: Encoder[A]) => Encoder[Option[A]] = _.fold(Json.JNull)(e.encode)
    given [A] => (e: Encoder[A]) => Encoder[List[A]] = as => Json.JArray(as.map(e.encode))

    inline def derived[A](using m: Mirror.Of[A]): Encoder[A] = inline m match
      case p: Mirror.ProductOf[A] =>
        product(
          labels[p.MirroredElemLabels],
          instances[p.MirroredLabel, p.MirroredElemTypes, p.MirroredElemLabels]
        )
      case s: Mirror.SumOf[A] =>
        sum(
          s,
          labels[s.MirroredElemLabels],
          instances[s.MirroredLabel, s.MirroredElemTypes, s.MirroredElemLabels]
        )

    inline def labels[Ls <: Tuple]: List[String] =
      constValueTuple[Ls].toList.asInstanceOf[List[String]]

    inline def instances[T <: String, Ts <: Tuple, Ls <: Tuple]: List[Encoder[Any]] =
      inline erasedValue[(Ts, Ls)] match
        case _: (EmptyTuple, EmptyTuple) => Nil
        case _: (h *: ts, l *: ls) => instance[T, h, l] :: instances[T, ts, ls]

    /** TODO: the error, when there is nothing to use. */
    inline def instance[T <: String, H, L]: Encoder[Any] = summonFrom {
      case e: Encoder[H] => e.asInstanceOf[Encoder[Any]]
      case m: Mirror.Of[H] => derived[H](using m).asInstanceOf[Encoder[Any]]
      case _ =>
        error(
          "cannot derive Encoder for " + constValue[T] + ": field " +
            constValue[L] + " has no Encoder"
        )
    }

    def product[A](labels: List[String], encoders: => List[Encoder[Any]]): Encoder[A] =
      a =>
        val values = a.asInstanceOf[Product].productIterator.toList
        Json.JObject(labels.lazyZip(encoders).lazyZip(values).map((l, e, v) => l -> e.encode(v)))

    def sum[A](
        s: Mirror.SumOf[A],
        labels: List[String],
        encoders: => List[Encoder[Any]]
    ): Encoder[A] =
      a =>
        val i = s.ordinal(a)
        encoders(i).encode(a) match
          case Json.JObject(fields) => Json.JObject(("type" -> Json.JString(labels(i))) :: fields)
          case other => other
  end Encoder
end Exercise03
