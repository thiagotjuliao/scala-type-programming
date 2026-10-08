# Chapter 24 — Derived JSON codec

> Status: **done** — `git show ch24`

The fifth case study. Chapter 10 derived a `Show` and an `Eq`: one direction,
and a result nobody parses back. A JSON codec has two directions that must
agree — what `Encoder` writes, `Decoder` has to read — and a decoder meets
data that is wrong in ways no type rules out: a missing field, a string where
a number was due, a case nobody declared. This chapter derives both from a
type's `Mirror` (chapter 10), with the instances found or derived during the
expansion (chapter 08), and makes the one thing derivation cannot do — a field
of a type with no codec — a compile error that says which field.

## Why this exists

A codec written by hand:

```scala
given Encoder[User] = u =>
  Json.JObject(List("name" -> Json.JString(u.name), "age" -> Json.JNumber(u.age)))

given Decoder[User] = json =>
  for
    name <- field[String](json, "name")
    age <- field[Int](json, "age")
  yield User(name, age)
```

It is the case class written out twice more, with the field names as strings.
Add a field to `User` and both compile — the encoder simply leaves it out, the
decoder never reads it. Rename one, and the JSON key keeps the old name in one
direction only. Nothing checks that the two agree with each other, or with
the class.

Reflection at runtime avoids the repetition and pays for it later: a field of
a type the library cannot handle is found when the first value is encoded, in
production. What is wanted is the codec *computed from the type*, by the
compiler — the same names, the same order, both directions from one
description — and a type it cannot handle rejected while compiling.

## The idea

### JSON is a value, and the codecs are type classes

```scala
enum Json:
  case JNull
  case JBool(value: Boolean)
  case JNumber(value: Double)
  case JString(value: String)
  case JArray(items: List[Json])
  case JObject(fields: List[(String, Json)])

trait Encoder[A]:
  def encode(a: A): Json

trait Decoder[A]:
  def decode(json: Json): Either[DecodeError, A]
```

`Encoder` and `Decoder` are chapter 06's type classes, with instances for
`Int`, `String`, `Boolean`, and for `Option[A]` and `List[A]` given one for
`A`. Text in and out — parsing and printing JSON — is not the interesting part,
and is left out.

### A product is an object, keyed by its labels

A case class's mirror gives its field names, `MirroredElemLabels`, and its
field types, `MirroredElemTypes` (chapter 10). The derived encoder walks both
together: for every field, an instance — found if one exists, derived from the
field type's own mirror if not — and the field's label as the key.

```scala
final case class User(name: String, age: Int) derives Encoder

summon[Encoder[User]].encode(User("Ada", 36))
// JObject(List(("name", JString("Ada")), ("age", JNumber(36.0))))
```

A field whose type is itself a case class needs no `derives` of its own: the
mirror is enough, and its encoder is derived on the way.

### A sum is an object with a discriminator

An enum's mirror lists its cases as `MirroredElemTypes`, and `ordinal` says
which case a value is. The case is encoded with its own (derived) encoder, and
the case's name is added under `"type"`:

```scala
enum Shape derives Encoder:
  case Circle(radius: Int)
  case Dot

// Circle(1): JObject(List(("type", JString("Circle")), ("radius", JNumber(1.0))))
// Dot:       JObject(List(("type", JString("Dot"))))
```

The decoder reads it back the other way: the `"type"` names the case, its
position among the labels picks the case's decoder, and the rest of the object
is that decoder's.

### Decoding can fail, and says where

A decoder answers `Either[DecodeError, A]`, where a `DecodeError` carries the
message and the **path** to the value that failed. A product decoder that sees
its field `age` fail adds `age` to the front of the path; a nested one adds its
own on the way out. The result points at the value:

```scala
DecodeError(List("address", "city"), "missing")
```

A missing field is decoded from `JNull`, so an `Option` field that is absent
is `None`, and a required one fails as `missing`. The values decoded are put
back together with `fromProduct` (chapter 10).

### What cannot be derived is a compile error, with a name

A field of a type with neither an instance nor a mirror — a
`java.time.Instant`, say — leaves the derivation with nothing to use. The
`summonFrom` that looks for the field's instance gets a last case, `case _ =>
error(...)` (chapter 08), and since the expansion knows the type's label and
the field's, the message can name them:

```scala
case _ => error("cannot derive Encoder for " + constValue[T] + ": field " + constValue[L] + " has no Encoder")
```

`compiletime.error` needs a constant string. `constValue` of a literal type is
a constant, and the compiler folds the concatenation before it reports it —
*"cannot derive Encoder for Event: field at has no Encoder"*.

A direct search, `summon[Encoder[Instant]]`, does not go through the
derivation. For that one the type class itself carries the message, with
`@implicitNotFound` (chapter 06).

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch24derivedjsoncodec/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

Measured on 2026-10-07 with Scala 3.9.0.

**No last case in the `summonFrom`.** A field nobody can encode stops the
expansion with a message about the machinery, not the field:

```
cannot reduce summonFrom with
 patterns :  case given e @ _:Encoder[java.time.Instant]
             case given m @ _:scala.deriving.Mirror.Of[java.time.Instant]
```

followed by an *"Inline stack trace"*. It does not say which field, nor which
type was being derived.

**A message that is not a constant.** `error(prefix + constValue[L])` with
`val prefix = "..."`, declared in an object `NotConstant`:

```
A literal string is expected as an argument to `compiletime.error`. Got NotConstant.prefix + "at"
```

The `val` is a value at runtime, and the message is needed while compiling.
Make it an `inline val`, or write the literal in place.

**An expansion error that waits its turn.** A `compiletime.error` is reported
when the expansion runs, after the code around it type-checks. With an
ordinary type error elsewhere in the same compilation, the derivation's error
does not appear at all — fix the first error, and the second shows up. A
build that reports one error is not saying there is only one.

**The two messages are not interchangeable.** `@implicitNotFound` on `Encoder`
is what a plain `summon[Encoder[Instant]]` reports: *"no Encoder for
java.time.Instant: ..."*. Inside a derivation the search is a `summonFrom`
case that simply does not match, and the annotation is never printed; the
derivation's own `error` is. A codec library wants both.

**A number is a `Double`.** `JNumber` holds a `Double`, so a `Long` beyond 2⁵³
or a `BigDecimal` would not survive the round trip. The decoder for `Int`
checks `isWhole`, and rejects `1.5` as *"expected a whole number"*; real
libraries keep the number's text instead.

## Exercises

| # | asks for |
| --- | --- |
| 01 | `Encoder.derived`: case classes as objects keyed by their labels, enums with a `"type"` discriminator |
| 02 | `Decoder.derived`, the way back, with an error that names the path to the value that failed |
| 03 | a compile error that names the type and the field that cannot be encoded, and one for a plain `summon` |

```bash
sbt "exercises/testOnly typeprog.ch24derivedjsoncodec.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [Type class derivation](https://docs.scala-lang.org/scala3/reference/contextual/derivation.html) —
  the reference: `derives`, the `Mirror` members, and a derived `Eq` written
  out in full
- [circe, *Semi-automatic derivation*](https://circe.github.io/circe/codecs/semiauto-derivation.html) —
  the JSON library most Scala code uses, deriving `Encoder` and `Decoder`
  from the same kind of description
- [jsoniter-scala](https://github.com/plokhotnyuk/jsoniter-scala) — the
  other end of the trade-off: codecs generated by macros (chapter 12), for
  speed
- [RFC 8259](https://www.rfc-editor.org/rfc/rfc8259), *The JavaScript Object
  Notation (JSON) Data Interchange Format* — section 6 on numbers, and why
  a `Double` is the usual, and lossy, reading of one
