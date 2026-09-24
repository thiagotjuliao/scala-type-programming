# Chapter 04 — Literal, singleton, union & intersection types

> Status: **done** — `git show ch04`

## Why this exists

Three signatures that say less than their authors know:

```scala
def describe(status: Int): String            // only 200, 404 and 500 mean anything
def port(env: Map[String, String]): Any      // an Int, or one of three errors
def stamp(env: Logger)(msg: String): Unit    // also needs a Clock
```

Each one is repairable with the tools of chapter 01, and each repair costs a
declaration that exists only to satisfy the type checker. An `enum Status`
with three cases and a conversion to and from `Int`. A `sealed trait PortError`
that three unrelated error classes must be edited to extend — and a result type
`Either[PortError, Int]` that nests the moment a second kind of failure joins
in. A `trait LoggerWithClock extends Logger, Clock`, and every value that has
both capabilities but was declared before this trait existed no longer fits.

The types this chapter adds need none of that. `200 | 404 | 500` is the set of
three values, written as a type. `Int | Missing | Malformed | OutOfRange` is
"one of these", with no common parent declared anywhere. `Logger & Clock` is
"both", satisfied by anything that happens to be both. They are types built
from other types — or from values — at the place they are used.

## The idea

### A value can be a type

Every literal has a type of its own, inhabited by that value alone:

```scala
val answer: 42 = 42
val id: "user-id" = "user-id"

val wrong: 42 = 43     // Found: (43 : Int)  Required: (42 : Int)
```

`42` as a type is a subtype of `Int`, and the compiler writes it `(42 : Int)`
in messages. Literal types exist for `Int`, `Long`, `Float`, `Double`,
`Char`, `String` and `Boolean` literals.

The catch is that an ordinary `val` does not keep one. Inference **widens** a
literal to its underlying type unless something asks for the precise one:

```scala
val plain = 200              // Int
val typed: 200 = 200         // 200 — asked for by the ascription
final val constant = 200     // 200 — a final val keeps its literal type
inline val inlined = 200     // 200 — and so does an inline val
```

`final val` is only allowed as a member; inside a method body the compiler
refuses it (*"The final modifier is not allowed on local definitions"*), and an
ascription is the way to keep the literal there.

### A value that is a path has a singleton type

Chapter 03 selected types *through* a stable path. The path itself also has a
type: `x.type`, the type whose only value is `x`. For an `object O`, `O.type`
is how the object's own type is written; for a `val`, `x.type` is narrower than
its declared type, and it is where literal types come from — `(42 : Int)` is
the singleton type of the literal `42`.

The singleton type anyone meets first is `this.type`. A method that returns
`this` and says so keeps the *caller's* type, subclass included:

```scala
class Request:
  def header(k: String, v: String): this.type = ...

final class JsonRequest extends Request:
  def body(json: String): this.type = ...

JsonRequest().header("Accept", "json").body("{}")   // header kept JsonRequest
```

Declared as `: Request`, the same `header` hands back a `Request`, and the
chain stops: *"value body is not a member of Request"*.

`Singleton` is the upper bound of all these types. A type parameter bounded by
it, `[T <: Singleton]`, is inferred as the literal instead of being widened —
which is what `ValueOf[T]`, the evidence that turns a singleton type back into
its value, needs:

```scala
def name[T <: Singleton](t: T)(using v: ValueOf[T]): T = v.value
name("a")   // T = "a"; without the bound, T = String and there is no ValueOf
```

### A union is "one of these"

`A | B` is the type of values that are an `A` or a `B`. Nothing has to be
declared for it: the members need no common parent, and the union is written
where it is needed.

```scala
final case class Missing(key: String)
final case class Malformed(key: String, raw: String)

def lookup(env: Map[String, String], key: String): String | Missing
```

A union is used by asking which member it is, and a `match` over one is
checked for exhaustiveness like a `match` over a sealed hierarchy:

```scala
lookup(env, "port") match
  case s: String  => ...
  case Missing(k) => ...
```

Leave a member out and the compiler warns *"match may not be exhaustive"*,
naming the case — for unions of literals too: a `match` over `200 | 404 | 500`
that forgets `500` is reported.

Unions are commutative and associative — `Int | String` and `String | Int` are
the same type — and they compose without nesting. Two functions that fail in
different ways combine into a result that lists every way, which is the thing
`Either` does not do without a shared error type or a nest of `Either`s.

### Inference keeps some unions and widens others

When the branches of an `if` or a `match` have different types, the result is
their union — Scala 3 does not reach for the common supertype as Scala 2 did:

```scala
val mixed = if ok then 1 else "one"       // Int | String
val list  = List(1, "one")                // List[Int | String]
```

But a union of *literals* is widened, like any literal:

```scala
val code = if ok then 200 else 404        // Int, not 200 | 404
```

and a union returned from a method without a declared result type can come out
in an unexpected shape — `IoError | (Int | ParseError)` — the same type as the
flat one, written the way inference assembled it. Declaring the result type is
both the documentation and the check.

### An intersection is "both"

`A & B` is the type of values that are an `A` and a `B`. It replaces Scala 2's
`A with B` in types, and unlike it, it is commutative: `A & B` and `B & A` are
the same type.

```scala
trait Logger:
  def log(line: String): Unit
trait Clock:
  def now(): Long

def stamp(env: Logger & Clock)(msg: String): Unit =
  env.log(s"[${env.now()}] $msg")
```

Anything that is both fits — an `object` that extends the two traits, an
anonymous `new Logger with Clock`, a value built by combining two others — and
nothing had to be declared as "a logger with a clock" in advance. A value that
is only a `Logger` is rejected:

```text
Found:    (l : Logger)
Required: Logger & Clock
```

When both sides have a member of the same name, the intersection has it at the
intersection of the two types: if `A` has `def id: Int` and `B` has
`def id: Any`, then `(A & B).id` is an `Int & Any`, which is `Int`.

### `Matchable` is what may be pattern-matched

`Any` has a subtype, `Matchable`, that every class type extends. A `match` on a
value of type `T` — an unconstrained type parameter — cannot know whether `T`
is something that may be inspected at runtime, and under `-source:future` the
compiler warns:

```text
pattern selector should be an instance of Matchable,
but it has unmatchable type T instead
```

The fix is to say what is expected: `[T <: Matchable]`, or a union of the
cases being handled. It matters because of chapter 05: an opaque type is not
supposed to be looked through at runtime, and `Matchable` is how the compiler
keeps a generic `match` from doing so.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch04literaluniontypes/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

**A `val` that was meant to be a literal.** `val Ok = 200` is an `Int`, and
handing it to a method that takes `200 | 404 | 500` fails, although the value is
right there:

```text
Found:    (Ok : Int)
Required: (200 : Int) | (404 : Int) | (500 : Int)
```

The `(Ok : Int)` is the tell: the name, and the widened type behind it. Write
`val Ok: 200 = 200`, or `final val Ok = 200` as a member.

**Copying a literal loses it again.** `val typed: 200 = 200` is fine;
`val copy = typed` is an `Int`. The widening happens at every unannotated `val`,
not once.

**`[T <: Singleton]` does not stop a `val` from widening.** The bound makes `T`
the literal inside the call; `val y = lit("a")` still widens the result to
`String`. It helps where the type parameter itself matters — `ValueOf[T]` —
not in the value handed back.

**A union of literals from an `if`.** `if ok then 200 else 404` is an `Int`.
Where `200 | 404` is wanted, the expected type has to be there when the `if` is
typed — an ascription on the `val`, or the parameter it is passed to.

**A missing case in a match on a union** is a warning, not an error, and it is
easy to scroll past:

```text
match may not be exhaustive.

It would fail on pattern case: ParseError(_)
```

**Writing `with` in a type.** It still compiles in Scala 3, as a deprecated
spelling of `&`:

```text
with as a type operator has been deprecated; use & instead
```

Types are written `A & B`; `with` remains only among the parents of a class,
where `new Logger with Clock` is still the way to write an anonymous class with
two of them.

## Exercises

| # | asks for |
| --- | --- |
| 01 | a `Status` type that is exactly `200`, `404` and `500`, and named constants that are usable as one |
| 02 | a request builder whose methods keep the subclass they are called on, and a helper that returns its argument's own type |
| 03 | a port lookup whose result lists every way it can fail, and a function that explains each |
| 04 | a function that needs a `Logger` *and* a `Clock`, and a way to make one out of the two |

```bash
sbt "exercises/testOnly typeprog.ch04literaluniontypes.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [Union types](https://docs.scala-lang.org/scala3/reference/new-types/union-types.html) and
  [intersection types](https://docs.scala-lang.org/scala3/reference/new-types/intersection-types.html) —
  the reference pages, each with a *more details* page for the subtyping and
  inference rules
- [Union types](https://docs.scala-lang.org/scala3/book/types-union.html) and
  [intersection types](https://docs.scala-lang.org/scala3/book/types-intersection.html)
  in the Scala 3 book
- [SIP-23: literal-based singleton types](https://docs.scala-lang.org/sips/42.type.html) —
  where `42` as a type, `Singleton` and `ValueOf` come from
- [The `Matchable` trait](https://docs.scala-lang.org/scala3/reference/other-new-features/matchable.html) —
  why it exists, and what `-source:future` turns on
