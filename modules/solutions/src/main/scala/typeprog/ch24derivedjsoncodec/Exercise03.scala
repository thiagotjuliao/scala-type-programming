package typeprog.ch24derivedjsoncodec

import scala.annotation.implicitNotFound
import scala.compiletime.{constValue, constValueTuple, erasedValue, error, summonFrom}
import scala.deriving.Mirror

/** Exercise 03 — say what cannot be encoded, and where. *Solved.*
  *
  * `instance` gets a last case. When neither an `Encoder` nor a mirror is
  * found for a field's type, `case _` matches, and `error` fails the
  * compilation where the expansion reaches it — with a message, instead of
  * *"cannot reduce summonFrom"*.
  *
  * To name the type and the field, `instance` has to know them, so the walk
  * carries them: `derived` passes the mirror's `MirroredLabel`, and
  * `instances` walks the labels beside the types, matching the pair `(Ts,
  * Ls)` and handing each `instance` its field's label. Both are literal types,
  * and `error` takes the message only as a constant: `constValue` turns each
  * literal type into a constant, and the compiler folds `"..." +
  * constValue[T] + "..."` into one before reporting it. A `val` holding part
  * of the message would not fold — it is a runtime value — and is refused.
  *
  * The error names `Event` even when `Meeting` is being derived: `Event` is
  * reached as a field, has a mirror, and is derived on the way; its `at` is
  * where the expansion stops. That is the useful place to point at.
  *
  * A plain `summon[Encoder[Instant]]` never reaches `derived`: it is an
  * ordinary search, and its message comes from the type class. That is what
  * `@implicitNotFound` on `Encoder` is for (chapter 06). Inside the
  * derivation the annotation is never printed — a `summonFrom` case that
  * finds nothing simply does not match — so the two messages do not replace
  * each other.
  */
object Exercise03:

  enum Json:
    case JNull
    case JBool(value: Boolean)
    case JNumber(value: Double)
    case JString(value: String)
    case JArray(items: List[Json])
    case JObject(fields: List[(String, Json)])

  @implicitNotFound(
    "no Encoder for ${A}: give it one, or make it a case class or an enum and derive it"
  )
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

    inline def instance[T <: String, H, L]: Encoder[Any] = summonFrom {
      case e: Encoder[H] => e.asInstanceOf[Encoder[Any]]
      case m: Mirror.Of[H] => derived[H](using m).asInstanceOf[Encoder[Any]]
      case _ =>
        error(
          "cannot derive Encoder for " + constValue[T] + ": field " + constValue[
            L
          ] + " has no Encoder"
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
