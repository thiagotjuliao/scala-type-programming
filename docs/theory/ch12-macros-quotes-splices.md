# Chapter 12 — Macros: quotes & splices

> Status: **done** — `git show ch12`

## Why this exists

A logging helper that prints an expression next to its value:

```scala
def debug(value: Int): String = s"? = $value"

debug(x * 2 + 2) // "? = 42" — which expression was it?
```

By the time `debug` runs, `x * 2 + 2` is `42`; the *code* that produced it is
gone. An `inline` parameter (chapter 08) keeps the expression around during
expansion, but nothing in `scala.compiletime` can turn it into its source text.
The same wall stands in front of every tool that needs to *look at code*: a
string literal that should be a valid regular expression, checked while
compiling; the name of a field, taken from `person.age` so that a rename
cannot leave a string behind; the fields of a case class and their types, as
the compiler sees them.

Chapters 07 to 10 computed with types, and then with values the compiler
could fold. What is left is arbitrary code — parsing, validating, rewriting —
run *by* the compiler, on the program being compiled. That is a **macro**: a
method whose body runs at compile time, receives the call's arguments as
syntax trees, and returns a tree to put in the call's place.

## The idea

### Quotes and splices

```scala
import scala.quoted.*

inline def showCode(inline a: Any): String = ${ showCodeImpl('a) }

def showCodeImpl(a: Expr[Any])(using Quotes): Expr[String] =
  Expr(a.show)
```

Two operators move between code and values:

- a **quote**, `'{ … }` or `'a`, is code as a value: `'a` is the argument
  expression, as an `Expr[Any]`, rather than what it evaluates to;
- a **splice**, `${ … }`, is the other way: it runs its body now, at compile
  time, and puts the `Expr` it produces into the program.

A macro is an `inline` method whose body is a single splice calling an
ordinary method — the *implementation* — with its arguments quoted. The
implementation takes `Expr`s and a `Quotes` context, and returns an `Expr`.
Inside a quote, a splice inserts an `Expr` into code being built:

```scala
def twiceImpl(a: Expr[Int])(using Quotes): Expr[Int] = '{ $a + $a }
```

`Expr[T]` is typed: `'{ $a + $a }` only compiles if `$a` is an `Int`, so a
macro cannot produce ill-typed code by accident. `Type[T]` is the same for
types, available in a quote for every `T: Type`.

### Values in, values out

`Expr(value)` lifts a value the macro computed into code — a `String`, a
`List`, anything with a `ToExpr`. `expr.value` goes the other way when the
expression is a literal, and `expr.valueOrAbort` aborts the expansion if it is
not:

```scala
inline def positive(inline n: Int): Int = ${ positiveImpl('n) }

def positiveImpl(n: Expr[Int])(using Quotes): Expr[Int] =
  if n.valueOrAbort > 0 then n
  else quotes.reflect.report.errorAndAbort("expected a positive literal", n)
```

`report.errorAndAbort` is the macro's `compiletime.error` (chapter 08), with a
message built at compile time and a position: the error is reported on the
user's code, where `n` was written.

### Matching on code

A quote can also be a pattern:

```scala
def simplifyImpl(e: Expr[Int])(using Quotes): Expr[Int] = e match
  case '{ ($x: Int) * 1 } => x
  case _ => e
```

`'{ ($x: Int) * 1 }` matches an expression of that shape and binds the
sub-expression `$x`. The rewrite happens while compiling: `simplify(y * 1)`
compiles to `y`.

### Reflection: `TypeRepr` and `Symbol`

Quotes and `Expr` cover code that can be written as Scala. For the compiler's
own view of a program there is `quotes.reflect`: `Term` for any expression,
`TypeRepr` for any type, `Symbol` for any definition.

```scala
def isCaseClassImpl[T: Type](using Quotes): Expr[Boolean] =
  import quotes.reflect.*
  Expr(TypeRepr.of[T].typeSymbol.flags.is(Flags.Case))
```

`TypeRepr.of[T].typeSymbol.caseFields` lists a case class's fields,
`memberType` gives each one's type, and `a.asTerm.pos.sourceCode` is the text
an argument was written with. The API is large, close to the compiler, and
less stable than the rest of the language; the quotes above are the part to
prefer.

### When to stop

Every earlier chapter is a way *not* to write a macro:

- a relation between types is a bound or evidence (chapters 01, 06, 11);
- a computation on types is a match type (07);
- a computation on constants, or a choice by type, is `inline` (08);
- an instance for any case class is derivation from a `Mirror` (10).

A macro costs more than any of these. It must be compiled before its callers
— in another file, and a stub's implementation cannot even be `???`, since it
runs inside the compiler. It slows compilation, it is harder for an IDE to
follow, its failures surface as expansion errors in someone else's code, and
the reflection API ties it to compiler internals. It is the right tool when
the program has to *read code*: source text, the shape of an expression, a
literal that must be parsed. When the question is about types, the answer is
almost always earlier in this course.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch12macrosquotessplices/Walkthrough.scala),
with the macros it calls in `WalkthroughMacros.scala` beside it — a macro
cannot be called in the file that defines it. Read it alongside this document;
every claim it makes is one the compiler is checking.

## Pitfalls

**Calling a macro where it is defined.**

```text
Cannot call macro method debugImpl defined in the same source file
```

Callers go in another file. When the compiler cannot order the files so that
every macro is compiled before its callers, it stops altogether — *"Cyclic
macro dependencies among …"* — with the advice to *"place macros in one set of
files and their callers in another"*.

**A stub that throws.** An implementation of `???` does not compile to a
failing test; it fails the compilation of every caller:

```text
Exception occurred while executing macro expansion.
scala.NotImplementedError: an implementation is missing
```

A stub returns `'{ ??? }` — code that throws when *it* runs.

**A value that is not a literal.** `valueOrAbort` on a variable:

```text
Expected a known value.

The value of: s
could not be extracted using scala.quoted.FromExpr$PrimitiveFromExpr@…
```

Replace it with a `report.errorAndAbort` that says what the caller should
write instead.

**A selection that is secretly a call.** A Java method declared with
parentheses, like `String.length`, is called with them even when written
without: `person.name.length` reaches the macro as `Apply(Select(…, length),
Nil)`, and code that only expects a `Select` rejects it:

```text
nameOf needs a name or a selection like a.b, not Exercise03Spec.this.person.name.length()
```

**Source text is the tree's span.** `pos.sourceCode` is the text between the
start and the end of the tree, and parentheses are not in the tree: for
`x * (1 + 1)` it is `x * (1 + 1`, without the closing parenthesis.

**Types printed in full.** `TypeRepr.show` prints `scala.Predef.String` and
`scala.Int`; `show(using Printer.TypeReprShortCode)` prints `String` and `Int`.

**Constant folding before the macro sees it.** The macro receives the typed
tree, and `1 + 2` written with literals has already become `3` in it: `show`
prints `3`, and a quoted pattern for a sum does not match. The source text
keeps what was written — `pos.sourceCode` is `1 + 2`. With a variable, `y + 2`
stays a sum, and `show` prints it as `y.+(2)`.

## Exercises

| # | asks for |
| --- | --- |
| 01 | `debug(expr)`, the expression's source text next to its value |
| 02 | `regex("…")`, checked while compiling: an invalid pattern, or a string that is not a literal, does not compile |
| 03 | `nameOf(person.age)`, the name of a field or variable, so that a rename cannot leave a string behind |
| 04 | a case class described as the compiler sees it — `Person(name: String, age: Int)` — from its type alone |

```bash
sbt "exercises/testOnly typeprog.ch12macrosquotessplices.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [Macros](https://docs.scala-lang.org/scala3/guides/macros/macros.html) and
  [quoted code](https://docs.scala-lang.org/scala3/guides/macros/quotes.html)
  in the macros guide — the tutorial path
- [Reflection](https://docs.scala-lang.org/scala3/guides/macros/reflection.html) —
  `quotes.reflect`, `TypeRepr` and `Symbol`
- [Best practices](https://docs.scala-lang.org/scala3/guides/macros/best-practices.html) —
  the guide's own advice on when not to write one
