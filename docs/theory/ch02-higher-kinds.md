# Chapter 02 — Higher-kinded types & type lambdas

> Status: **done** — `git show ch02`

## Why this exists

Try to write, once, the method that adds one to every `Int` inside a container:

```scala
def incList(xs: List[Int]): List[Int]       = xs.map(_ + 1)
def incOption(xs: Option[Int]): Option[Int] = xs.map(_ + 1)
def incEither(xs: Either[String, Int]): Either[String, Int] = xs.map(_ + 1)
```

Three bodies, one idea. Chapter 01's tools cannot merge them. A type parameter
`C` stands for a *finished* type — `List[Int]` — and there is no way to say
"`C` but with `String` inside instead of `Int`", because `C` has no inside. What
the three signatures share is not a type but a *type constructor*: something
that is still waiting for its element type. Abstracting over it takes a
parameter that is itself a type constructor:

```scala
def inc[F[_]](fa: F[Int])(F: Functor[F]): F[Int] = F.map(fa)(_ + 1)
```

That `F[_]` is a higher-kinded type, and nearly every abstraction worth naming
in functional Scala — `Functor`, `Monad`, `Traverse`, every effect type — is
stated over one. The third version, `Either[String, Int]`, is also where the
chapter's second tool comes from: `Either` takes *two* parameters, `F` wants
one, and closing the gap needs a type constructor that has no name of its own.
That is a type lambda.

## The idea

### Kinds are the types of types

Values have types; types have **kinds**. The kind says how many type arguments
a type still needs before a value can have it:

| type | kind | reads as |
| --- | --- | --- |
| `Int`, `String`, `List[Int]` | `*` | a proper type — values live here |
| `List`, `Option` | `* -> *` | give it one type, get a proper type |
| `Either`, `Map`, `Tuple2` | `* -> * -> *` | give it two |
| `Functor` | `(* -> *) -> *` | give it a *type constructor*, get a proper type |

`List` on its own is not a type anything can have — `val xs: List = ...` is
rejected with *"Missing type parameter for List"*. It is a function at the type
level, and `List[Int]` is that function applied.

The last row is the new one. `Functor`'s parameter is not a type but a type
constructor, so `Functor` is a function from functions to types — a
**higher-kinded** type, in exactly the sense that `map` is a higher-order
function.

### `F[_]` declares the shape, not a wildcard

In a type parameter list, `F[_]` says "`F` takes one type argument". The
underscore is a placeholder for a parameter name nobody will use, not a
wildcard; `F[X]` means the same, and `G[_, _]` declares something of kind
`* -> * -> *`. Inside the body `F` must then always be applied — `F[A]`,
`F[B]` — and outside, whatever is supplied for `F` must have that kind:

```scala
trait Functor[F[_]]:
  def map[A, B](fa: F[A])(f: A => B): F[B]

Functor[List]     // * -> *, as declared
Functor[Int]      // rejected: Int is *
Functor[Either]   // rejected: Either is * -> * -> *
```

The compiler calls this a kind mismatch, and checks it before anything else —
no instance search, no member lookup, just arity.

### Type lambdas give a type constructor without naming it

`Either[String, Int]` has the right number of holes for nothing: it is `*`, and
`Either` is `* -> * -> *`. To get a `* -> *` out of `Either` one parameter has
to be fixed, and in Scala 3 that is written as a function at the type level:

```scala
[X] =>> Either[String, X]
```

Read it exactly like `x => f("s", x)`: a parameter list, an arrow, a body.
It is a type constructor of kind `* -> *`, anonymous, so it can be written
directly where one is expected:

```scala
def eitherFunctor[E]: Functor[[X] =>> Either[E, X]] = ...
```

Naming one is an ordinary alias, and the alias can itself take parameters —
which gives `ErrorOr` the kind `* -> (* -> *)`, a type constructor that
*returns* a type constructor:

```scala
type ErrorOr[E] = [X] =>> Either[E, X]

Functor[ErrorOr[String]]  // fine: ErrorOr[String] is * -> *
```

That last property is what a two-parameter alias does not have.
`type Result[E, X] = Either[E, X]` can only be applied to both arguments at once, and
`Functor[Result[String]]` is rejected for *"Not enough type arguments"*. The
curried form is the one that can be partially applied.

Type lambdas also compose. `[X] =>> F[G[X]]` stacks two type constructors into
one, and naming it takes parameters that are themselves higher-kinded:

```scala
type Compose[F[_], G[_]] = [X] =>> F[G[X]]

Compose[List, Option][Int]  // = List[Option[Int]]
```

### Inference fills the hole from the right

Call `inc(Right(1): Either[String, Int])` and the compiler has to solve
`F[Int] = Either[String, Int]` for `F`. That equation has more than one
reading in principle; Scala 3 picks one rule and sticks to it — the argument
lines up with the **last** type parameter, and everything before it is fixed:

```text
F[Int]  ~  Either[String, Int]   ⇒   F = [X] =>> Either[String, X]
```

The same rule makes a function `String => Int` an `F[Int]` with
`F = [X] =>> String => X`. It is why effect types, `Either` and friends put the
"varying" parameter last, and it is also the rule's limit: an `F` whose hole
sits anywhere else is never inferred, and the call has to say which one it
means — `inc[[X] =>> Either[X, String]](Left(1))(...)`.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch02higherkinds/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

**Supplying a type where a type constructor is wanted, or the reverse.** The
message names the argument and the shape it was measured against:

```text
Type argument Int does not have the same kind as its bound [_$1]
```

`[_$1]` is the compiler's rendering of the declared `F[_]` — one parameter,
unnamed. The same message appears for `Functor[Either]`, which has one
parameter too many. Declaring a parameter as a plain `F` and then applying it
fails from the other side: *"F does not take type parameters"*.

**Writing `Either[String, _]` and expecting a type lambda.** In Scala 2 with the
kind-projector plugin that was partial application. In Scala 3 an underscore in
argument position is a *wildcard*, so `Either[String, _]` is the proper type
"an `Either` of `String` and something", kind `*`, and is rejected like any
other:

```text
Type argument Either[String, ?] does not have the same kind as its bound [_$1]
```

The `?` in the message is the giveaway. Write `[X] =>> Either[String, X]`.
The underscore is meant to become a type-lambda placeholder in some future
version, with `?` as the only wildcard; until then, and without the opt-in
compiler flag, it is a wildcard.

**Partially applying a two-parameter alias.** `type Result[E, X] = ...` cannot
be given one argument:

```text
Not enough type arguments for Result[E, X]
expected: [E, X]
actual:   [String]
```

Curry it: `type Result[E] = [X] =>> Either[E, X]`.

**Expecting inference to find a hole that is not last.** Passing an
`Either[Int, String]` where `F[Int]` is wanted does not infer
`F = [X] =>> Either[X, String]`; it fails outright, and the message is
unhelpful:

```text
Found:    Either[Int, String]
Required: F[Int]
where:    F is a type variable with constraint <: [_] =>> Any
```

Nothing is wrong with the value. The compiler matched the `Int` against
`String` — the last parameter — and gave up. Supply `F` explicitly.

## Exercises

| # | asks for |
| --- | --- |
| 01 | declare `Mappable`, `BiMappable` and `Instances` at the kinds their members need |
| 02 | define `ErrorOr` as a type lambda, and a `Functor` for it that inference can find |
| 03 | a `Functor` over the *left* side of `Either`, and the call that inference cannot make |
| 04 | `Compose` two type constructors into one, and map through both layers at once |

```bash
sbt "exercises/testOnly typeprog.ch02higherkinds.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [Type lambdas](https://docs.scala-lang.org/scala3/reference/new-types/type-lambdas.html) —
  the reference page, short, and the
  [detailed spec](https://docs.scala-lang.org/scala3/reference/new-types/type-lambdas-spec.html)
  behind it for the variance and bounds rules
- [Higher-kinded types](https://docs.scala-lang.org/scala3/book/types-others.html) in the Scala 3
  book, for a second phrasing of the kind table
- [Kind projector migration](https://docs.scala-lang.org/scala3/guides/migration/plugin-kind-projector.html) —
  why `_` changed meaning, where it is heading, and the compiler flag that opts in early
- Moors, Piessens, Odersky, [*Generics of a Higher Kind*](https://adriaanm.github.io/files/higher.pdf)
  (OOPSLA 2008) — the paper that put type constructor polymorphism into Scala
- [cats `Functor`](https://github.com/typelevel/cats/blob/main/core/src/main/scala/cats/Functor.scala) —
  the same trait as this chapter's, with twenty more years of consequences
