package typeprog.ch24derivedjsoncodec

import scala.compiletime.{constValueTuple, erasedValue, summonFrom}
import scala.deriving.Mirror

/** Exercise 01 — derive an `Encoder` for case classes and enums. *Solved.*
  *
  * `derived` is chapter 10's derivation with a JSON result. `inline m match`
  * tells a product from a sum while expanding, where the mirror's members are
  * known. In both cases the same two lists are built from the mirror: the
  * labels, `constValueTuple` of `MirroredElemLabels` — field names for a case
  * class, case names for an enum — and one encoder per element type.
  *
  * `instances` walks the element types with `erasedValue` and asks `instance`
  * for each. `instance` is its own `inline def` with a type parameter rather
  * than a `summonFrom` written inside the walk's `case`: there it would search
  * for the pattern's type variable, not the type it matched, and find nothing
  * (chapter 10's pitfalls). It takes an existing `Encoder` first — `Int`,
  * `Option[String]`, a type with its own `derives` — and falls back to
  * deriving one from the element's mirror, which is how `Address` needs no
  * `derives`, and how an enum's cases, which nobody derived anything for, get
  * theirs.
  *
  * `product` zips the labels, the encoders and the value's fields —
  * `productIterator` walks them in the same order the mirror lists them. `sum`
  * lets `ordinal` say which case the value is, encodes it with that case's
  * encoder, and puts the case's label in front under `"type"`.
  *
  * The encoders reach `product` and `sum` **by name**. For `Tree`, deriving
  * the encoder of `Node` finds `Encoder[Tree]` — the instance being defined,
  * already in the companion. Evaluated eagerly, it would be read before it
  * exists; by name, it is evaluated on the first `encode`, when it does. The
  * instances themselves are anonymous classes built in ordinary methods, not
  * in the `inline` body, so that each derivation does not define a new class.
  *
  * The casts to `Encoder[Any]` are what a list of encoders of different types
  * costs; they are safe because the i-th encoder is paired with the i-th field.
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

    inline def derived[A](using m: Mirror.Of[A]): Encoder[A] = inline m match
      case p: Mirror.ProductOf[A] =>
        product(labels[p.MirroredElemLabels], instances[p.MirroredElemTypes])
      case s: Mirror.SumOf[A] =>
        sum(s, labels[s.MirroredElemLabels], instances[s.MirroredElemTypes])

    inline def labels[Ls <: Tuple]: List[String] =
      constValueTuple[Ls].toList.asInstanceOf[List[String]]

    inline def instances[Ts <: Tuple]: List[Encoder[Any]] = inline erasedValue[Ts] match
      case _: EmptyTuple => Nil
      case _: (h *: ts) => instance[h] :: instances[ts]

    inline def instance[H]: Encoder[Any] = summonFrom {
      case e: Encoder[H] => e.asInstanceOf[Encoder[Any]]
      case m: Mirror.Of[H] => derived[H](using m).asInstanceOf[Encoder[Any]]
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
end Exercise01
