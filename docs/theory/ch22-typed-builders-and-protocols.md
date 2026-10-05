# Chapter 22 — Typed builders and protocols

> Status: **done** — `git show ch22`

The third case study. Chapter 11 built a request builder with two flags, one
per field, and a door whose states were listed by hand. This chapter takes the
same idea past what can be listed: a builder for any number of fields, driven
by a schema, and a connection that follows a protocol of any length. The state
that was a pair of booleans becomes a **tuple of types** (chapter 09), read
with match types (chapter 07) and enforced with evidence (chapter 11); the
connection belongs to one server through a path-dependent type (chapter 03).

## Why this exists

A builder for a record with three fields, two of them required:

```scala
final case class User(name: String, email: String, age: Option[Int])

UserBuilder().name("Ada").build() // compiles; fails at runtime, no email
```

Chapter 11's answer was one phantom flag per field — `RequestBuilder[HasUrl,
HasMethod]` — and one setter per field. It does not grow. Ten fields are ten
type parameters, every setter repeats all ten, and the record's shape is
written once in the case class and again in the builder's signature.

A connection has the same problem in time instead of space. A client must say
`Hello`, then `Auth`, then send its `Data`; chapter 11's door listed each state
as a type and each transition as a method. A protocol of five messages is five
states and five methods, and a second protocol is all of it again.

What both need is state that is **data at the type level**: a list of the
fields set so far, a list of the messages still due — and operations that read
and update that list for any length.

## The idea

### A schema is a tuple of pairs

```scala
type Schema = (("name", String), ("email", String), ("age", Int))
```

Literal types (chapter 04) for the keys, ordinary types for the values. One
type says which fields exist and what each one holds.

### The value's type is looked up

`set("age")(36)` should take an `Int` because the schema says so. That is a
function from a key to a type — a recursive match type over the schema
(chapters 07 and 09):

```scala
type Lookup[S <: Tuple, K] = S match
  case (K, v) *: _ => v
  case _ *: rest => Lookup[rest, K]
```

`set` declares its value as `Lookup[Schema, K]`, with `K` the literal type of
the key it was given, and the compiler works out `Int` while type-checking the
call.

### The fields set so far are a tuple

The builder is `Builder[S <: Tuple]`, starting at `EmptyTuple`; each `set`
returns a `Builder[K *: S]`. Whether a key is already there is another match
type, `Contains[S, K]`, which reduces to `true` or `false` — and a setter that
asks for `Contains[S, K] =:= false` (chapter 11) cannot be called twice for the
same key.

### `build` asks for every required field

`build` needs each key of `Required = ("name", "email")` to be in `S`. That is
`AllIn[Required, S]`, a match type folding `Contains` over the required keys,
demanded as evidence. Raw `=:=` evidence fails with *"Cannot prove that ...
=:= (true : Boolean)"*, so the demand goes through a type class of its own
with an `@implicitNotFound` message, as in chapter 11.

### A protocol is a tuple of messages

A connection is `Conn[Sent <: Tuple, Left <: Tuple]`. `send` takes exactly
`Tuple.Head[Left]` and returns a connection with that message moved from
`Left` to the end of `Sent`. A message out of order is a type mismatch; a
message after the last one has no head to match. Closing takes a `Conn[P,
EmptyTuple]` and returns the transcript as a `P` — the protocol's own type,
built one append at a time.

### A connection belongs to its server

`Conn` is a class inside `Server`, so each server has its own
`server.Conn` (chapter 03). `other.close(conn)` with a connection of `server`
is a type mismatch, even when both servers speak the same protocol.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch22typedbuildersandprotocols/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

Measured on 2026-10-05 with Scala 3.9.0; package prefixes trimmed from the
messages.

**A key the schema lacks is reported at the value.** `set("nmae")("Ada")`:

```
Found:    ("Ada" : String)
Required: Lookup[EmptyTuple.type, ("nmae" : String)]
```

`Lookup` walked off the end of the schema and stopped there, unreduced — with
the E184 warning *"Match type reduction failed since selector EmptyTuple.type
matches none of the cases"* beside it. The error is right; it just names the
value, not the key.

**A key that widens finds the wrong field.** With `K <: String` instead of
`K <: String & Singleton`, `set("age")(36)` is rejected:

```
Found:    (36 : Int)
Required: String
```

`"age"` was inferred as `String`, and a match type pattern matches by
subtyping: `("name", String)` is a `(String, v)`, so `Lookup[Schema, String]`
stops at the first pair. `Singleton` keeps the literal type.

**A walk with no case for the end.** `Contains` without `case EmptyTuple =>
false` reduces for every non-empty tuple and is stuck on the empty one — which
is what every builder starts as, so no first `set` compiles:

```
Cannot prove that Contains[EmptyTuple,
  ("name" : String)] =:= (false : Boolean).
Note: a match type could not be fully reduced:
  ...
  failed since selector EmptyTuple
  matches none of the cases
```

**Raw evidence makes a poor message.** `build` asking for `AllIn[Required, S]
=:= true` directly is correct, and fails with *"Cannot prove that AllIn[...]
=:= (true : Boolean)"*. Behind a type class with `@implicitNotFound`, the same
failure reads as the class says:

```
cannot build a User: it needs name and email, and has only ("age" : String) *: EmptyTuple.
```

followed by the search the compiler tried, which is still worth reading.

**A message too many.** After the last message, `send(Data("x"))`:

```
Found:    Data
Required: Tuple.Head[EmptyTuple.type & NonEmptyTuple]
Note: a match type could not be fully reduced:
  trying to reduce  Tuple.Head[EmptyTuple.type & NonEmptyTuple]
  failed since selector EmptyTuple.type & NonEmptyTuple
  matches none of the cases
```

Again an error at the value, again with an E184 warning beside it: nothing is
due, so no type is the next message.

**Another server's connection.** Closing on `other` a conversation opened on
`server`:

```
Found:    server.Conn[
  (Hello,
    Auth,
    Data),
EmptyTuple.type]
Required: other.Conn[
  (Hello,
    Auth,
    Data),
EmptyTuple]
```

The type arguments agree; the prefix does not. That one difference, `server.`
against `other.`, is the whole check.

## Exercises

| # | asks for |
| --- | --- |
| 01 | a builder whose `set` takes the type the schema gives its key, and takes each key once |
| 02 | a `build` that compiles only once every required field is set, in any order, and says so when not |
| 03 | a connection that sends a protocol's messages in order, closes only at the end on its own server, and returns a transcript of the protocol's type |

```bash
sbt "exercises/testOnly typeprog.ch22typedbuildersandprotocols.*"
```

## Further reading

- Kohei Honda,
  [*Types for dyadic interaction*](https://doi.org/10.1007/3-540-57208-2_35),
  CONCUR '93, 1993 — where session types begin: a protocol as the type of a
  channel.
- [Session type](https://en.wikipedia.org/wiki/Session_type), on Wikipedia —
  an overview, and pointers to the multiparty generalisation.
