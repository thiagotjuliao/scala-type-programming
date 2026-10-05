# Chapter 21 — Units of measure

> Status: **done** — `git show ch21`

The second case study. It builds a `Quantity[U]` whose unit is part of its
type, so that the compiler checks a formula's dimensions the way it checks any
other type. It uses opaque types (chapter 05) for a wrapper that costs
nothing, literal types (chapter 04) for the exponents of a unit, match types
(chapter 07) and `compiletime.ops` (chapter 09) for the arithmetic on them, and
`=:=` evidence (chapter 01) for the one operation that is not always defined.

## Why this exists

A physics formula, with doubles:

```scala
def fallTime(height: Double, gravity: Double): Double =
  math.sqrt(height * gravity * 2) // should be height / gravity
```

It compiles, it runs, and it returns a number — of the wrong unit. A height in
metres and an acceleration in metres per second squared multiply into m²/s²,
whose square root is a speed, not a time. Nothing in `Double` knows that, so
nothing complains; the mistake surfaces later, as a wrong answer, if at all.

Naming the units does not help by itself. `type Metres = Double` is an alias,
and an alias is the type it names: `Metres + Seconds` is still `Double +
Double`.

Chapter 05's exercise 04 went further: an opaque `Quantity[U]` with the unit as
a phantom parameter, so that metres and seconds no longer add. Its units are
*names* — `Meters`, `Seconds`, and a type constructor `/` to combine them —
and names cannot be computed with. `Meters / Seconds` is a type, but
`(Meters / Seconds) / (Meters / Seconds)` is another one, not "no unit"; there
is no `*` to bring a speed times a time back to `Meters`, and no way to write
one, because nothing says which combinations of names are equal. A formula with
more than one operation has a unit the types can spell but not simplify.

What the formula needs is a unit that is **computed**: the type of
`metres / seconds` worked out from the types of its operands, at compile time,
in a form where two spellings of the same unit are the same type.

## The idea

### A unit is a list of exponents

Every unit in mechanics is a product of powers of three base units: metre,
second, kilogram. A speed is m¹·s⁻¹, an energy is m²·s⁻²·kg¹. So a unit is
three integers, and with literal types (chapter 04) three integers are a type:

```scala
type Dim = (Int, Int, Int) // exponents of (metre, second, kilogram)

type Metre = (1, 0, 0)
type Second = (0, 1, 0)
type Velocity = (1, -1, 0)
```

Negative literals are literal types too: `-1` is one.

### A quantity is a `Double` with a phantom unit

As in chapter 05:

```scala
opaque type Quantity[U <: Dim] = Double
```

At runtime a `Quantity[Metre]` is a bare `Double`, with no box and no
allocation. Outside the scope that defines it,
`Quantity[Metre]` and `Quantity[Second]` are unrelated types, and neither is a
`Double`. `U` is a phantom parameter — no value of type `(1, 0, 0)` ever exists.
Addition is defined only between quantities of the same `U`:

```scala
extension [U <: Dim](q: Quantity[U])
  def +(r: Quantity[U]): Quantity[U] = q + r
```

### Multiplying units adds exponents

`m · m = m²`, `m / s = m·s⁻¹`: multiplication adds the exponents, division
subtracts them. That is a function from two types to a type — a match type
(chapter 07) — and the arithmetic on the literals is `compiletime.ops.int`
(chapter 09):

```scala
type Mul[A <: Dim, B <: Dim] <: Dim = (A, B) match
  case ((m1, s1, k1), (m2, s2, k2)) => (m1 + m2, s1 + s2, k1 + k2)
```

and `*` between quantities returns `Quantity[Mul[U, V]]`. The compiler reduces
`Mul[Metre, Second]` to `(1, 1, 0)` while type-checking the call, so
`(metres(10) / seconds(2)) * seconds(2)` is a `Quantity[Metre]` again: the
exponents cancel on their own, with no rule written for it.

### An operation that is not always defined: the square root

`√(m²) = m` halves every exponent. `√m` has no unit at all, and should not
compile. Halving is a match type too, with a case only for even numbers —
but a match type that cannot reduce is **not an error**. The compiler warns
and leaves the type unreduced, so `metres(1).sqrt` compiles and its type is
a `Quantity` of something no one can name.

The repair is evidence, as in chapter 01: `sqrt` asks for proof that its
result, squared, is the unit it started from.

```scala
def sqrt(using Mul[Half[U], Half[U]] =:= U): Quantity[Half[U]]
```

For `(2, 0, 0)` the compiler reduces both sides to the same tuple and supplies
the proof. For `(1, 0, 0)`, `Half` does not reduce, the two sides are not the
same type, and the call does not compile — an error, not a warning.

### What it buys

`fallTime` with units:

```scala
def fallTime(h: Quantity[Metre], g: Quantity[Acceleration]): Quantity[Second] =
  (h.scale(2) / g).sqrt
```

Written as `(h * g).sqrt`, the result is a `Quantity[Velocity]`, and the
declared `Quantity[Second]` rejects it. The formula is checked against its own
units.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch21unitsofmeasure/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

Measured on 2026-10-04 with Scala 3.9.0; package prefixes trimmed from the
messages.

**Inside the opaque scope, nothing is checked.** Where `Quantity` is defined it
*is* `Double`, so a formula written next to it compiles with any unit — the
wrong one included, with no message at all:

```scala
opaque type Quantity[U <: Dim] = Double
def fallTime(h: Quantity[Metre], g: Quantity[Acceleration]): Quantity[Second] =
  h * g // Double * Double: compiles
```

The library goes in an object of its own, and the code that uses it outside.
A related symptom is an extension method that suddenly *"is not a member of
Double"*: the receiver was a quantity created inside that scope, where its type
is `Double`.

**A match type with no declared bound.** `Mul` written as
`type Mul[A <: Dim, B <: Dim] = (A, B) match ...` reduces fine for literal
units, but the library's own signatures stop compiling:

```
Type argument Mul[U, V] does not conform to upper bound Dim

Note: a match type could not be fully reduced:

  trying to reduce  Mul[U, V]
  failed since selector (U, V)
  does not uniquely determine parameters m1, s1, k1, m2, s2, k2 in
    case ((m1, s1, k1), (m2, s2, k2)) => (m1 + m2, s1 + s2, k1 + k2)
```

For an abstract `U`, the only thing known about `Mul[U, V]` is its declared
bound. `type Mul[A <: Dim, B <: Dim] <: Dim = ...` declares it.

**A match type that no case matches is a warning, not an error.**
`metres(1).sqrt`, with `Half` defined for even exponents and nothing else:

```
Match type reduction failed since selector (1 : Int)
matches none of the cases

    case (0 : Int) => (1 : Int) / (2 : Int)
```

It is an E184 *Type Warning*, and the expression compiles, with a type that
never reduces. The Scala 3 reference describes the same behaviour: no matching
case means no reduction. `assertTypeError` cannot see warnings, so a
spec has to catch the mistake where the unreduced type is used — or, as
exercise 02 does, the operation has to demand evidence that makes it an
error.

**Generic code cannot take a square root.** With the evidence in place, a
method over an abstract unit is rejected:

```scala
def root[U <: Dim](q: Quantity[U]) = q.sqrt
```
```
Cannot prove that Mul[
  Half[U],
  Half[U]] =:= U.

where:    U is a type in method root with bounds <: Dim
```

Correct: nothing says `U`'s exponents are even. Such a method asks its own
caller for the same `using Mul[Half[U], Half[U]] =:= U`.

**A wrong formula is reported where the unit is checked.** `(h * g).sqrt`
declared as a `Quantity[Second]` gives an error that starts with *"value sqrt is
not a member of Quantity[((2 : Int), (-2 : Int), (0 : Int))]"*, and the real
reason is further down:

```
Found:    Quantity[((1 : Int), (-1 : Int),
  (0 : Int))]
Required: Quantity[
  Second]
```

The expected type reaches into the call to `sqrt`, and its failure is reported
as the extension method not fitting. Read the whole message.

**Comparing with a plain number.** `metres(1) == 1.0` is rejected:

```
Values of types Quantity[
  Metre] and Double cannot be compared with == or !=
```

This check runs after type-checking, so `assertTypeError` does not see it
either (see [CONTRIBUTING.md](../../CONTRIBUTING.md)). Compare `value`s.

## Exercises

| # | asks for |
| --- | --- |
| 01 | `Mul` and `Div` as match types, and `*` and `/` between quantities whose unit is computed |
| 02 | `Half`, and a `sqrt` that compiles only when every exponent is even |
| 03 | three formulas — speed, kinetic energy, fall time — whose units the compiler checks |

```bash
sbt "exercises/testOnly typeprog.ch21unitsofmeasure.*"
```

## Further reading

- [Units of Measure](https://learn.microsoft.com/en-us/dotnet/fsharp/language-reference/units-of-measure),
  in the F# language reference — the same idea built into a language, with
  the compiler normalising unit formulas itself.
- [Match Types](https://docs.scala-lang.org/scala3/reference/new-types/match-types.html),
  in the Scala 3 reference — the reduction rules, including why a match type
  that no case matches is left unreduced.
- [coulomb](https://github.com/erikerlandson/coulomb) — a Scala 3 library for
  unit analysis at compile time, with conversions between units, which this
  chapter leaves out.
