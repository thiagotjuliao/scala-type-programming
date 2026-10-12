# Chapter 25 — Macro-checked interpolator

> Status: **done** — `git show ch25`

The sixth case study. Chapter 12 checked a single literal while compiling — a
regular expression — and stopped there. A string interpolator is a literal
with holes in it: the text around the holes is known while compiling, the
values in them are not, and *where* a hole sits decides what may go into it.
This chapter builds `json"..."`: the text parsed by the compiler, every
argument checked against its place in the JSON, and the value put together
from code the macro writes (chapter 12), with the instances it needs found
during the expansion (chapters 06 and 08).

## Why this exists

JSON written in a string:

```scala
def user(name: String, age: Int): Json =
  Json.parse(s"""{"name": "$name", "age": $age}""").toOption.get
```

Three things are wrong with it, and none of them shows while compiling. A
missing comma or brace is found by `parse` when the line runs. `name` is
pasted in as text, so a name with a quote in it produces broken JSON, or a
different document than the one intended. And nothing checks that `age` is
something JSON can hold: `$at` with a `java.time.Instant` compiles, and
becomes whatever its `toString` happens to print.

Building the value by hand fixes the last two and loses the reason for writing
a literal:

```scala
Json.JObject(List("name" -> Json.JString(name), "age" -> Json.JNumber(age)))
```

What is wanted is the literal, read by the compiler: malformed JSON rejected
where it is written, each argument turned into JSON by a type class rather
than by `toString`, and the whole thing compiled to the constructor calls
above, with no parsing left for runtime.

## The idea

### An interpolator is a method on `StringContext`

`json"[1, $n]"` is rewritten by the parser into a call:

```scala
StringContext("[1, ", "]").json(n)
```

The literal is split at the holes into **parts** — one more part than there
are arguments — and the arguments are passed in order. Any method on
`StringContext` can be called this way, so an interpolator is an extension
method, and a checked one is a macro:

```scala
extension (inline sc: StringContext)
  inline def json(inline args: Any*): Json = ${ jsonImpl('sc, 'args) }
```

Both parameters are `inline`, so the implementation receives the code of the
call rather than its values.

### The parts are literals, and can be read

In the implementation, the `StringContext` is the expression
`StringContext("[1, ", "]")`, and a quoted pattern takes it apart (chapter
12). `Varargs` matches a sequence written out as arguments, and gives one
`Expr` per element:

```scala
sc match
  case '{ StringContext(${ Varargs(parts) }*) } => parts.map(_.valueOrAbort)
```

The parts arrive as they were written: an interpolator gets them *raw*, with
Scala's escapes untouched — `\n` is a backslash and an `n`. For JSON that is
what is wanted: its escapes are JSON's to read.

With the parts as strings, the macro runs a parser on them, here, inside the
compiler. The chapter's parser, `JsonTemplate.parse`, is given: it returns the
JSON with every hole marked by its argument's index — as a value, `Tpl.Arg(i)`,
or as a key, `Key.Arg(i)` — or a `ParseError` with the part and the offset
where reading stopped.

### An error points into the literal

Every part is a literal with a position, and the position of a part starts at
its first character. The offset the parser reports is therefore a place in the
source:

```scala
val pos = parts(error.part).asTerm.pos
report.errorAndAbort(
  "invalid JSON: " + error.message,
  Position(pos.sourceFile, pos.start + error.offset, pos.start + error.offset)
)
```

and the compiler underlines the character where the JSON went wrong, inside
the string, as it would a mistake in Scala.

### The result is code that builds the value

The parsed JSON exists in the compiler; the program needs it at runtime. The
macro turns it into the constructor calls that build it — `Expr(value)` for
each number and string (chapter 12), `Expr.ofList` for a list of
expressions — so `json"[1, true]"` compiles to
`Json.JArray(List(Json.JNumber(1.0), Json.JBool(true)))`. Nothing is parsed
when the program runs.

### Each argument is checked against its place

`args` is matched with `Varargs` too, and each argument has a type. A quoted
pattern with a type variable binds it:

```scala
arg match
  case '{ $a: t } => // t is the argument's type
```

What the argument must be depends on where its hole is. As a value, it needs
an `Encoder` — the macro asks for one with `Expr.summon[Encoder[t]]`, which is
a `summonFrom` (chapter 08) answered with an `Option` — and the code it writes
for the hole is `encoder.encode(arg)`. As a key, it must be a `String`. A hole
inside a string is neither, and the parser rejects it.

The type that comes out of `'{ $a: t }` is the argument's *own* type, which is
often narrower than the one written in its declaration: `$n` for a `val n: Int`
binds `t` to `n.type`, and `${1}` binds it to the literal type `1`. An
`Encoder` is invariant, so there is no `Encoder[n.type]`; `TypeRepr.of[t].widen`
is the type a programmer would name, and the one to look the instance up for.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch25macrocheckedinterpolator/Walkthrough.scala),
with the macros it calls in `WalkthroughMacros.scala` beside it. Its examples
are two small interpolators of their own, `hex"..."` and `show"..."`, so that
the JSON one is left for the exercises. Read it alongside this document; every
claim it makes is one the compiler is checking.

## Pitfalls

Measured on 2026-10-12 with Scala 3.9.0.

**An argument's type is a singleton.** For `val n = 1`, `'{ $a: t }` on `$n`
binds `t` to `n.type`; on `${1}`, to `1`; on `${"s"}`, to `"s"`. Asked for
`Expr.summon[Encoder[t]]` without widening, the search fails for every one of
them — the `Int` instance included — and the macro reports its own error
against a perfectly encodable argument, with the singleton printed short:

```text
no Encoder for name
no Encoder for 1
```

Since a test that calls `json"..."` with arguments is code, not a snippet,
those errors stop the spec from compiling at all. Widen first:
`TypeRepr.of[t].widen`, then `.asType` to have it as a `Type` again.

**The parts are raw.** `json"""{"a": "x\ny"}"""` hands the macro a part with a
backslash followed by an `n`, not a newline: an interpolator receives the
source text, and `s"..."` is the one that calls `StringContext.processEscapes`.
A macro that wants Scala's escapes processed has to do it itself.

**A part's position covers its text only.** For `json"""[1, 2"""` the first
part starts at the `[`, not at the quotes — so `pos.start + offset` is the
character the parser stopped at. The same parts passed by hand,
`StringContext("[1, 2").json()`, are ordinary string literals whose position
starts *at* the quote, and the same arithmetic lands one character early.

**Encoding every argument before looking at where it goes.** Asking each
argument for an `Encoder` up front, then building the JSON, reports a key of
type `java.time.Instant` as *"no Encoder for Instant"* — true, and beside the
point: a key must be a `String`, and no `Encoder` would make it one. Check an
argument where its hole is, so that the error is about that place.

**Duplicate keys are not caught.** JSON allows them, and this chapter's
parser keeps both. A key passed as an argument is not known until runtime in
any case; a literal one could be checked, and is not.

## Exercises

| # | asks for |
| --- | --- |
| 01 | `json"..."` without arguments: parsed while compiling into the code that builds it, invalid JSON a compile error pointing into the literal |
| 02 | arguments as values: each one encoded by the `Encoder` found for its type, and a compile error for a type with none |
| 03 | arguments as keys: a hole's place decides its type — a key must be a `String` |

```bash
sbt "exercises/testOnly typeprog.ch25macrocheckedinterpolator.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [String interpolation](https://docs.scala-lang.org/scala3/book/string-interpolation.html) —
  the Scala 3 book: how `x"..."` becomes a call on `StringContext`, and how to
  write an interpolator of your own
- [Macros](https://docs.scala-lang.org/scala3/guides/macros/macros.html) — the
  macros guide, including `Varargs` and `Expr.ofList`
- [Quoted code](https://docs.scala-lang.org/scala3/guides/macros/quotes.html) —
  quoted patterns, and type variables bound by them
- [circe-literal](https://github.com/circe/circe/tree/series/0.14.x/modules/literal) —
  a `json"..."` interpolator in the library most Scala code uses, checked by
  a macro in the same way
