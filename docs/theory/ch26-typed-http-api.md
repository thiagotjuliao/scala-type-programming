# Chapter 26 — Typed HTTP API

> Status: **done** — `git show ch26`

The last case study. Every chapter so far built one kind of type-level
guarantee; this one puts them on a single value. An HTTP endpoint is declared
once — its method, its path with the values it captures, the type it returns
— and that declaration decides three things that are usually written three
times: what the handler receives, how the router reads a request, and what a
client sends and gets back. The path's type is a tuple computed as the path
is written (chapters 04, 07, 09); the endpoint carries its types as members
(chapters 03, 06); the client's signature is typed through the endpoint it is
given (chapter 03); and the path can be written as a string the compiler
checks and types (chapters 08, 12).

## Why this exists

An endpoint, written the usual way:

```scala
// server
routes.get("/users/:id/name") { req =>
  val id = req.param("id").toInt
  users(id).name
}

// client
def userName(id: Int): String =
  http.get(s"/users/$id/name").body
```

The path is written twice, as two strings that nothing compares. The server
reads `id` with a name and a conversion that fails at runtime; the client
formats it with an interpolation that accepts anything. The response is a
`String` on both sides because nobody said it was anything else. Rename the
segment, change `id` to a `Long`, return a list — each side compiles, and the
two stop agreeing the first time they talk.

What is wanted is one value:

```scala
val userName = Endpoint.get(path"/users/$id/name").returning[String]
```

from which the server gets a handler of type `Int => String`, the router a
parser for `/users/42/name`, and the client a `call(userName)(42)` that is an
`Either[String, String]` — and where a change to the value changes all three,
or stops one of them compiling.

## The idea

### A path's type is the tuple of what it captures

A path is built with `/`, a literal segment or a capture at a time:

```scala
val userPosts = Path.root / "users" / capture[Int]("id") / "posts"
// Path[Int *: EmptyTuple]
```

The type parameter is a tuple: a literal segment leaves it as it is, and a
capture appends the type it reads, `Tuple.Append[In, A]` (chapter 09). The
type is computed one call at a time while the path is written, so nobody
writes it down. `render` and `parse` then take and give exactly that tuple,
and a path with an `Int` capture cannot be rendered with a `String`.

How a captured segment is read and written is a type class, `Segment[A]`
(chapter 06): `capture[java.time.Instant]("at")` does not compile, because
nothing says how to read one from a path.

### An endpoint carries its types as members

```scala
trait Endpoint:
  type In <: Tuple
  type Out
  def path: Path[In]
  def codec: Codec[Out]
```

`Endpoint.get(path).returning[String]` is an `Endpoint.Aux[Int *:
EmptyTuple, String]` — chapter 06's `Aux`, a refinement that keeps both
members known (chapter 01). Type members rather than parameters, so that
code taking *any* endpoint — a router, a client — can still talk about its
`In` and `Out`, through the value (chapter 03).

### The handler's argument is computed from the path

A handler for one capture should be `(id: Int) => ...`, not `(in:
Tuple1[Int]) => ...`. `Args` is a match type (chapter 07) that turns the
tuple into what a handler wants: `Unit` for none, the value for one, the
tuple for more. `handle(f: Args[In] => Out)` reduces at the call, where `In`
is concrete:

```scala
userName.handle((id: Int) => names(id))           // compiles
userName.handle((id: String) => id)               // does not
```

Inside `handle`, `In` is abstract and `Args[In]` is stuck; the value is
converted by a function that mirrors the match type case for case, and a
cast states what the compiler cannot see.

### The client is typed through the endpoint

```scala
def call(e: Endpoint)(args: Args[e.In]): Either[String, e.Out]
```

is a dependent method type: at `client.call(userName)(42)`, `e` is
`userName`, `e.In` is `Int *: EmptyTuple`, `Args` reduces to `Int`, and
`e.Out` is `String`. The client renders the path with the arguments, sends
it, and reads the body with the endpoint's codec — the one the server wrote
it with.

### The path, written as a string

`path"/users/$id/posts"` is the same path, written as it is read. A macro
(chapter 12) takes the interpolation apart, as chapter 25 did: it checks the
text — a leading `/`, no empty segment, only the characters a path allows, a
capture that is a whole segment — and writes out the calls to `/`, binding
each capture's type with `'{ $c: Capture[a] }`. The macro's result type can
only say `Path[?]`; the call is declared `transparent` (chapter 08), so its
type is the type of the code it expands to: `Path[Int *: EmptyTuple]`.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch26typedhttpapi/Walkthrough.scala),
with the macro it calls in `WalkthroughMacros.scala` beside it. Its example
is a typed log line rather than an HTTP path, so that the exercises are left
to do. Read it alongside this document; every claim it makes is one the
compiler is checking.

## Pitfalls

Measured on 2026-10-13 with Scala 3.9.0.

**A one-capture handler is not a function of a `Tuple1`.** Without `Args`,
`handle` takes an `In => Out`, and the natural handler,
`userName.handle((id: Int) => names(id))`, is rejected — `In` is
`Int *: EmptyTuple`, a `Tuple1[Int]`, and not an `Int`:

```text
Found:    Int => typeprog.ch26typedhttpapi.Exercise02Fixtures.userName.Out
Required: typeprog.ch26typedhttpapi.Exercise02Fixtures.userName.In =>

  typeprog.ch26typedhttpapi.Exercise02Fixtures.userName.Out
```

**Two parameters for a tuple, through a member.** Scala 3 adapts a
two-parameter lambda to a function of a pair, and it still does with the
match type written out: `val g: Args[(Int, String)] => String = (id, slug) =>
...` compiles. Handed to `postTitle.handle`, whose parameter is typed
`Args[In]` through the endpoint's member `In`, the same lambda is rejected:

```text
Wrong number of parameters, expected: 1
```

A pattern-matching function, `{ case (id, slug) => ... }`, is accepted, and
so is a lambda of one tuple parameter.

**`Args[In]` inside `handle`.** `In` is abstract there, so `Args[In]` does
not reduce, and handing the parsed tuple straight to the handler is
rejected:

```text
Found:    (in : Endpoint.this.In)
Required: typeprog.ch26typedhttpapi.Exercise02.Args[Endpoint.this.In]

Note: a match type could not be fully reduced:

  trying to reduce  typeprog.ch26typedhttpapi.Exercise02.Args[Endpoint.this.In]
  failed since selector Endpoint.this.In
  does not match  case EmptyTuple => Unit
  and cannot be shown to be disjoint from it either.
```

**A handler parameter of `Nothing => Any` accepts everything.** `Function1`
is contravariant in its argument and covariant in its result (chapter 01), so
every function is a `Nothing => Any`. A signature like that compiles with any
handler — and cannot call one, since there is no `Nothing` to pass it.

**A macro's result type is not the call's.** Without `transparent`, the call
has the type the macro declares, `Path[?]`, whose `In` nobody knows —
`path"/users/$id/posts".render(Tuple1(42))` is rejected:

```text
Found:    Tuple1[Int]
Required: ?1.In
```

**`'{ ??? }` in a transparent macro is a `Nothing`.** With `transparent`, a
placeholder expansion gives the call the type of `???`, and nothing can be
selected on it:

```text
Found:    (??? : => Nothing)
Required: ?{ render: ? }
```

A stub for such a macro returns a value of a loose type instead —
`Path.root.asInstanceOf[Path[Tuple]]` here — so that calls still compile.

## Exercises

| # | asks for |
| --- | --- |
| 01 | `Path[In]`: a capture appends its type to the path's tuple, and `render` / `parse` take and give exactly that tuple |
| 02 | `Args` and `handle`: the endpoint decides the handler's type, and a handler of any other shape does not compile |
| 03 | `Client.call`: arguments and result typed through the endpoint, with failures as values |
| 04 | `path"..."`: a path written as a string, checked while compiling, typed as the calls it stands for |

```bash
sbt "exercises/testOnly typeprog.ch26typedhttpapi.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [tapir](https://tapir.softwaremill.com/en/latest/) — endpoints as values,
  interpreted as a server, a client and documentation; the design this
  chapter is a small version of
- [Servant, *A web API as a type*](https://docs.servant.dev/en/latest/tutorial/ApiType.html) —
  the same idea in Haskell, where the API is a type rather than a value
- [Match types](https://docs.scala-lang.org/scala3/reference/new-types/match-types.html) —
  the reference, including when a match type reduces
- [Dependent function types](https://docs.scala-lang.org/scala3/reference/new-types/dependent-function-types.html) —
  and the dependent method types they generalise
