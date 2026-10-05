# Chapter 20 — Validation

> Status: **done** — `git show ch20`

The first case study. It introduces nothing new: it builds one small library —
validation that reports every error at once — out of chapters 01, 02 and 06,
and the point is to see them working together on a problem that is not about
types.

## Why this exists

A sign-up form with three fields, each checked on its own:

```scala
def name(s: String): Either[String, String] =
  if s.isBlank then Left("name is empty") else Right(s)
def age(n: Int): Either[String, Int] =
  if n < 18 then Left("must be 18 or older") else Right(n)
def email(s: String): Either[String, String] =
  if s.contains('@') then Right(s) else Left("email has no @")

for n <- name(""); a <- age(12); e <- email("x") yield User(n, a, e)
// Left(name is empty)
```

Three fields are wrong and the user is told about one. They fix it, submit
again, and learn about the second. Nothing about the form asked for that: the
three checks do not depend on each other, and all three could have run.

`Either` cannot do otherwise, and the reason is in its `flatMap`. The `for`
above is `name("").flatMap(n => age(12).flatMap(a => ...))`: the check of the
age is *inside a function of the name*, so when there is no name there is
nothing to call it with. Stopping at the first error is not a choice
`Either` makes — it is what sequencing means when each step may need the value
of the one before.

These steps do not need it. What they need is a combination that takes two
results that were computed *independently* and keeps the errors of both:

```scala
map2(name(""), age(12))(_ -> _)   // both errors, not the first
```

That operation, with a way to lift a plain value, is an **applicative
functor**. It is weaker than a monad — it cannot let the second step depend on
the first — and that weakness is exactly what makes accumulating errors
possible.

## The idea

### The type: a result that is a value or errors

```scala
enum Validated[+E, +A]:
  case Valid(value: A)
  case Invalid(errors: E)
```

It is `Either` with a different promise. Both parameters are covariant, for the
reason chapter 01 gave for `List`: `Valid(42)` has no errors at all, so its
error type is `Nothing`, and a `Validated[Nothing, Int]` has to be usable
wherever a `Validated[List[String], Int]` is expected. Invariant, it is not, and
every valid value needs an ascription.

### Combining errors is a type class

Two `Invalid`s are combined by combining their errors, and how depends on the
error type: lists concatenate, strings could be joined, a count would add. That
is behaviour chosen by a type, which chapter 06 calls a type class:

```scala
trait Semigroup[A]:
  extension (x: A) def combine(y: A): A
```

`map2` asks for a `Semigroup[E]` and nothing else. An error type without one is
not an error type `map2` can accumulate, and the call does not compile — which
is the right moment to find out.

### The abstraction: `Applicative[F[_]]`

`map2` and `pure` are not specific to `Validated`. `Option` has them, `List`
has them, `Either` has them (and stops at the first `Left`). Code written
against the operations instead of the type works for all of them:

```scala
trait Applicative[F[_]]:
  def pure[A](a: A): F[A]
  def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C]
```

`F[_]` is chapter 02's higher-kinded parameter, and `Validated` does not fit it
as it stands: it has two parameters, and `F` takes one. The error type is fixed
first, with a type lambda — `[A] =>> Validated[E, A]` — and the instance exists
only for error types that can be combined:

```scala
given [E: Semigroup] => Applicative[[A] =>> Validated[E, A]] = ...
```

A conditional instance, as in chapter 06: *if* `E` has a `Semigroup`, *then*
`Validated[E, _]` is applicative.

### `traverse`: written once

With the abstraction in place, the loop every validation library needs —
check every element, keep every error — is written once, for any applicative:

```scala
def traverse[F[_]: Applicative, A, B](as: List[A])(f: A => F[B]): F[List[B]]
```

With `Validated` it reports every element that failed. With `Option` it is
`None` as soon as one is. Same code, different `F`: the behaviour comes from
the instance the compiler found.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch20validation/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

Measured on 2026-10-04 with Scala 3.9.0; package prefixes trimmed from the
messages.

**`Validated` itself where `F[_]` is wanted.** `Applicative[Validated]` is a
kind error, not a missing instance:

```
Type argument Validated does not have the same kind as its bound [_$2]
```

(`_$2` is a fresh name the compiler makes up; the number varies.)

The bound is `F[_]`, one parameter; `Validated` takes two. Fix one with a
type lambda, `[A] =>> Validated[E, A]`, or with an alias such as
`type Checked[A] = Validated[List[String], A]`.

**An error type with no `Semigroup`.** `map2` on two `Invalid(true)`s says
which premise is missing:

```
No given instance of type Semigroup[Boolean] was found for a context parameter of method map2
```

and asking for the instance directly, `Applicative[[A] =>> Validated[Boolean,
A]]`, shows the search that almost succeeded:

```
I found:

    Applicative.given_Applicative_Validated[
      Boolean](
      /* missing */summon[Semigroup[Boolean]])

But no implicit values were found that match type Semigroup[Boolean].
```

That second message is chapter 06's conditional instance seen from the
outside: the instance matched, its condition did not.

**An `Applicative` is not found as a `Functor`.** Every `Applicative[Option]`
is a `Functor[Option]`, and still `summon[Functor[Option]]` fails:

```
No given instance of type Functor[Option] was found for parameter x of method summon in object Predef

The following import might fix the problem:

  import Applicative.given_Applicative_Option
```

The instance lives in `Applicative`'s companion, and the implicit scope of
`Functor[Option]` is the companions of `Functor` and `Option` — not of every
subtype of `Functor`. Where a `Functor` is needed, the instance must also be
reachable from there; the exercises ask only for `Applicative`.

**An invariant `Validated`.** Stored first and used later, a valid result no
longer fits:

```scala
val valid = Valid(42)
val v: Validated[List[String], Int] = valid
```
```
Found:    (valid : Validated[Nothing, Int])
Required: Validated[List[String], Int]
```

Written in one line, `val v: Validated[List[String], Int] = Valid(42)`, it
compiles invariant too — the expected type reaches `Valid.apply` and picks
`E` — which is why the mistake survives the first test of it.

**A `flatMap` on `Validated`.** Nothing stops one from being written, and it
type-checks: it is `Either`'s. A `for` over `Validated` with it then stops at
the first error, silently, because the second check is again inside a function
of the first value. No compiler message is involved; it is the reason the
exercises give `Validated` a `map2` and no `flatMap`.

## Exercises

| # | asks for |
| --- | --- |
| 01 | `Validated` itself — covariant, with `map` and `fold` — so that a valid value fits any error type |
| 02 | a `Semigroup` type class with instances, and a `map2` that keeps the errors of both sides |
| 03 | `Functor` and `Applicative` over `F[_]`, with instances for `Option` and, through a type lambda, for `Validated` |
| 04 | `traverse`, written once against `Applicative`, and a sign-up form validated with it that reports every error |

```bash
sbt "exercises/testOnly typeprog.ch20validation.*"
```

## Further reading

- Conor McBride and Ross Paterson,
  [*Applicative programming with effects*](https://doi.org/10.1017/S0956796807006326),
  Journal of Functional Programming 18(1), 2008 — where applicative functors
  come from.
- [Validated](https://typelevel.org/cats/datatypes/validated.html), in the
  Cats documentation — the same design in a library, including why `Validated`
  has no lawful `Monad`.
