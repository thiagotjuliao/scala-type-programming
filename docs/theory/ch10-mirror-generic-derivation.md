# Chapter 10 — `Mirror` & generic derivation

> Status: **done** — `git show ch10`

## Why this exists

Equality for a case class, written by hand:

```scala
final case class Person(name: String, age: Int)

given Eq[Person] = (x, y) => x.name == y.name && x.age == y.age
```

It is one line, and it is the same line for every case class: compare each
field with its own `Eq`, and require them all. An `enum` gets its own
variation — same case, then compare that case's fields. A codebase with fifty
case classes and four type classes has two hundred of these, each written by
hand, each out of date the day a field is added.

The recipe is always the same, and it depends on nothing but the *shape* of the
type: the list of its fields and their types, or the list of its cases.
Chapter 09 wrote instances for any tuple with two rules. If a case class could
be seen as the tuple of its fields — and an enum as the list of its cases — the
same two rules would cover every case class ever written.

The compiler knows the shape. What it needs is a way to publish it, as types,
to code that can recurse on them. That is `Mirror`, and an instance built from
it is **derived**.

## The idea

### A `Mirror` is the shape of a type, as types

For a case class, the compiler synthesises a `Mirror.ProductOf`:

```scala
val m = summon[Mirror.ProductOf[Person]]

m.MirroredElemTypes   // (String, Int)
m.MirroredElemLabels  // ("name", "age")
m.MirroredLabel       // "Person"
m.fromProduct(("Ann", 3))  // Person("Ann", 3)
```

The shape is in **type members** (chapter 01): the field types as a tuple
(chapter 09), their names as a tuple of literal types (chapter 04), the type's
name as a literal. `fromProduct` goes the other way, from a tuple of values to
an instance.

For an `enum` or a sealed trait, it is a `Mirror.SumOf`: `MirroredElemTypes` is
the tuple of the cases, and `ordinal(x)` says which case a value is. A case
with no parameters — `case Red` — has a singleton type, and its own mirror is a
product with no elements.

`Mirror.Of[T]` is either. There is none for a class that is not a case class,
or a trait that is not sealed: the compiler cannot know their shape, and says
so.

### Reading the shape

Because the shape is in types, chapter 08's tools read it while compiling:
`constValueTuple[m.MirroredElemLabels]` is the tuple of names as values,
`constValue[m.MirroredLabel]` the type's name. And since `MirroredElemTypes` is
a type, chapter 09's operations apply to it:

```scala
inline def arity[T](using m: Mirror.ProductOf[T]): Int =
  constValue[Tuple.Size[m.MirroredElemTypes]]

arity[Person] // 2
```

It can also be the type of a later parameter — a dependent type (chapter
03) — which is how a method can demand exactly a case class's fields, in
order, as a tuple.

### Deriving: an instance from a mirror

A derived instance is chapter 09's recursion, run over the mirror's element
types. For every element, an instance — found if one exists, derived if not —
and a way to combine them:

```scala
object Eq:
  inline def derived[A](using m: Mirror.Of[A]): Eq[A] = inline m match
    case s: Mirror.SumOf[A] => eqSum(s, instances[m.MirroredElemTypes])
    case _: Mirror.ProductOf[A] => eqProduct(instances[m.MirroredElemTypes])
```

- `instances` walks the element types with an `inline match` on `erasedValue`
  (chapter 08) and, for each, uses `summonFrom` to take an existing `Eq` or
  derive one from the element's own mirror;
- for a product, the combination is *every field equal*;
- for a sum, it is *the same case, and that case's instance says equal* —
  `ordinal` picks the case, and the case's instance does the rest.

`derived` is `inline` so that it runs at each use, where `A` is a concrete type
whose mirror has concrete members.

### `derives`

```scala
final case class Person(name: String, age: Int) derives Eq
```

A `derives Eq` clause asks the compiler to put `given Eq[Person] =
Eq.derived` in `Person`'s companion — where every search for an `Eq[Person]`
finds it, with no import (chapter 06). The only contract is a method called
`derived` in the type class's companion. Without one:

```text
NoDerived[Q] cannot be derived since trait NoDerived has no companion object
```

### Recursive types, and why the instances are lazy

```scala
enum Tree derives Eq:
  case Leaf(value: Int)
  case Node(left: Tree, right: Tree)
```

Deriving `Eq[Tree]` needs `Eq[Node]`, which needs `Eq[Tree]` for its fields —
the instance being defined. `summonFrom` finds it: `derives` has already put it
in the companion. If the derivation *evaluates* the element instances while the
instance is being built, it reads itself before it exists, and the compiler
warns:

```text
Infinite loop in function body
```

The element instances are therefore passed **by name** to the method that
builds the instance, and evaluated on first use — by which time the instance
exists. A plain method taking `elems: => List[Eq[Any]]` does it, and keeps the
anonymous class out of the `inline` body, where it would be duplicated at every
use.

### The cost

Derivation trades code for compile time: every `derives`, and every
`summonFrom` that falls back to `derived`, is an expansion. Deep or wide types
expand a lot — this build raises `-Xmax-inlines` to 128 for that reason. The
runtime side is a list of instances and some casts; the casts are safe because
the mirror's types and the value's fields are the same by construction.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch10mirrorgenericderivation/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

**No mirror for a plain class.**

```text
No given instance of type scala.deriving.Mirror.ProductOf[Plain] was found for parameter x of method summon in object Predef. Failed to synthesize an instance of type scala.deriving.Mirror.ProductOf[Plain]: class Plain is not a generic product because it is not a case class
```

**A mirror held in a local `val`.** `val m = summonInline[Mirror.Of[T]]` inside
an `inline` body keeps the mirror with the type it *declares*, and its members
are abstract again:

```text
m.MirroredLabel is not a constant type; cannot take constValue
Tuple element types must be known at compile time
```

Take the mirror as a `using` parameter: the given the compiler passes has the
refined type, with every member known.

**An element nobody can provide.** A field whose type has no instance and no
mirror stops the expansion with a message about the machinery, not the type:

```text
cannot reduce summonFrom with
 patterns :  case given e @ _:Eq[Plain]
             case given m @ _:scala.deriving.Mirror.Of[Plain]
```

A last `case _ => error("…")` in the `summonFrom` replaces it with one that
names the problem.

**Only looking for existing instances.** `summonAll[Tuple.Map[m.MirroredElemTypes,
Eq]]` finds the instances that exist, and the cases of an `enum` usually have
none — they are case classes nobody derived anything for:

```text
No given instance of type Eq[S2.C] was found
```

Derive the missing ones on the way, with `summonFrom` and a fallback to
`derived`.

**Evaluating the instances eagerly.** For a recursive type, *"Infinite loop in
function body"*: pass them by name and cache them lazily.

**A class inside the `inline` body.** An instance written as `new Eq[A] { … }`
inside `derived` is a new anonymous class at every derivation:

```text
New anonymous class definition will be duplicated at each inline site
```

Build it in an ordinary method, called from the `inline` one.

## Exercises

| # | asks for |
| --- | --- |
| 01 | a type's name and its field names, read from its mirror |
| 02 | `Show` for any case class, `derives Show`, nested case classes included |
| 03 | every value of an enum whose cases have no parameters — and a compile error for one that has |
| 04 | `build`, a case class from the tuple of its fields, type-checked field by field, and its inverse |

```bash
sbt "exercises/testOnly typeprog.ch10mirrorgenericderivation.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [Type class derivation](https://docs.scala-lang.org/scala3/reference/contextual/derivation.html) —
  the reference: `Mirror`, `derives`, and a complete `Eq` derivation
- [`scala.deriving.Mirror`](https://github.com/scala/scala3/blob/main/library/src/scala/deriving/Mirror.scala) —
  short enough to read in full
- [shapeless-3](https://github.com/typelevel/shapeless-3) — derivation as a
  library, built on these primitives
