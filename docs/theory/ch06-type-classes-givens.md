# Chapter 06 — Type classes, givens & implicit search

> Status: **done** — `git show ch06`

## Why this exists

A method that finds the largest element of a list:

```scala
def largest[A](xs: List[A]): A = xs.reduce((a, b) => if a > b then a else b)
// value > is not a member of A
```

`A` could be anything, so nothing is known about it — in particular not how to
compare two of them. Chapter 01's answer is a bound, `A <: Comparable[A]`, and
it works for exactly the types whose authors thought of it: every type that is
to be compared has to *extend* the trait, in its own definition. `Int` does
not, a class from a library cannot be made to, and a type that should be
comparable two different ways cannot extend the trait twice.

The other answer is to pass the comparison in: `largest(xs, (a, b) => a < b)`.
That puts the behaviour outside the type, where it belongs — and then makes
every caller write the same function at every call, for a choice that depends
on nothing but `A`.

What is wanted is the second design with the compiler doing the passing:
behaviour for a type, written once, anywhere, and supplied at each call site by
looking it up *by type*. That is a **type class**. In Scala 3 the pieces are a
trait with a type parameter, `given` instances of it, `using` clauses that ask
for one, and the **implicit search** that connects them.

The search is the part this course is interested in. It runs at compile time,
it is driven entirely by types, it can combine instances to build new ones, and
it can compute a type as a by-product. Generic derivation (chapter 10) and
type-safe state machines (chapter 11) are built on it.

## The idea

### A type class, its instances, and a demand for one

```scala
trait Show[A]:
  def show(a: A): String

object Show:
  given Show[Int] = i => i.toString
  given Show[String] = s => s"\"$s\""

def describe[A](a: A)(using s: Show[A]): String = s.show(a)

describe(42)     // "42"
describe("hi")   // "\"hi\""
```

`Show[A]` says what a type must be able to do; it is not a supertype of
anything. A `given` is a value the compiler is allowed to pass on its own. A
`using` parameter is one it *must* pass: at `describe(42)` it infers `A = Int`,
then looks for a `Show[Int]`, and fails the call if there is none. `summon[T]`
is the same lookup with nothing around it.

`[A: Show]` — a **context bound** — is short for the `using` clause, with the
instance anonymous. Scala 3.6 lets it be named, `[A: Show as s]`, and bounded
twice, `[A: {Show, Ordering}]`.

### Conditional instances: search is recursive

An instance can itself demand instances:

```scala
given listShow: [A: Show] => Show[List[A]] =
  xs => xs.map(summon[Show[A]].show).mkString("[", ", ", "]")
```

This is a rule, not a value: *if* there is a `Show[A]`, *then* there is a
`Show[List[A]]`. Asked for a `Show[List[List[Int]]]`, the compiler applies it
twice and finishes at `Show[Int]` — an instance nobody wrote, assembled at
compile time from the three that were. When the chain breaks, the message shows
how far it got:

```text
No given instance of type Show[List[Widget]] was found for parameter x of method summon in object Predef.
I found:

    Show.listShow[Widget](/* missing */summon[Show[Widget]])

But no implicit values were found that match type Show[Widget].
```

That recursion is a small logic program run by the type checker, and it is the
engine of everything from here to chapter 11.

The premises can also be written out, named, between the type parameters and
the result:

```scala
given listShow: [A] => (s: Show[A]) => Show[List[A]] =
  xs => xs.map(s.show).mkString("[", ", ", "]")
```

`[A: Show]` is short for this, with the name left out. The long form is the one
to reach for when a premise is not about a single type parameter — a
`Conversion[A, B]` relates two, and a context bound attaches to one — or when
there are several premises the body has to tell apart. Before Scala 3.6 the
same rule was written `given listShow[A](using s: Show[A]): Show[List[A]]`;
that form still compiles, but it is the old syntax and is being phased out.
The change is [SIP-64](https://docs.scala-lang.org/sips/sips/typeclasses-syntax.html).

### Where the compiler looks

Two places, in order.

**The lexical scope** first: givens defined in the enclosing blocks, classes
and objects, and givens *imported*. A wildcard import does not import givens —
`import Instances.*` brings in everything else — and they need their own
selector, `import Instances.given`, or by type, `import Instances.{given
Show[?]}`. The separation is deliberate: bringing a given into scope changes
what other code means, so it is never done by accident.

**The implicit scope of the type** second, and only if the lexical scope found
nothing: the companion objects of the type being searched for and of all its
parts. For `Show[List[Version]]` that is the companions of `Show`, `List` and
`Version`. This is why instances live in companions: an instance in the
companion of the type class or of the data type is found everywhere, without an
import. The instance for a type class and a type the author owns neither of —
an *orphan* — has no companion to live in, and has to be imported.

Because the lexical scope wins, an import is also how an instance is
*overridden* locally: import a second `Ordering[Version]`, and in that block it
is used instead of the companion's.

### When two match

Search fails when nothing matches; it also fails when two things match and
nothing tells them apart. Two orderings for `Version`, `ascending` and
`descending`, defined side by side in an object `Two` and both imported:

```text
Ambiguous given instances: both given instance ascending in object Two and given
instance descending in object Two match type Ordering[Version] of parameter x of
method summon in object Predef
```

Several rules break the tie before it comes to that, and they are worth knowing
in the order they matter in practice:

1. **Nesting.** Of two lexical candidates, the one defined in the more deeply
   nested scope wins.
2. **The owner's class.** A given defined in an object wins over one defined in
   a class or trait that object extends. This is the *low-priority trait*:
   move the instance that should lose into a parent trait of the companion,
   and a tie becomes a preference.
3. **Specificity.** A given `Show[Int]` wins over a polymorphic
   `[A] => Show[A]`, and `[A: Show] => Show[Option[A]]` over the same
   fallback — the more particular rule is chosen.
4. **Generality, between values.** Two plain givens whose types are subtypes of
   each other — a `given Seq[Int]` and a `given List[Int]`, searched for as
   `Seq[Int]` — used to resolve to the most specific. Since Scala 3.5 the
   *most general* one is chosen. It is the rule that changed most recently,
   and the one to check first when an upgrade changes which instance is used.

What the types cannot rank are two rules of the same shape that match for
*different* reasons: `[A: Show] => Describe[A]` and `[A: Ordering] =>
Describe[A]` both match `Describe[Int]`, since `Int` has both premises, and
neither type is more particular than the other. Those are ambiguous wherever
they meet, whatever their order in the source — until rule 2 or rule 1 is
used to say which one is the fallback.

### A given can compute a type

A type class can have a type member, and each instance fixes it:

```scala
trait Elements[C]:
  type Elem
  def first(c: C): Option[Elem]

object Elements:
  type Aux[C, E] = Elements[C] { type Elem = E }

  given [A] => Aux[List[A], A] = ...
  given Aux[String, Char] = ...

def firstOf[C](c: C)(using u: Elements[C]): Option[u.Elem] = u.first(c)
```

The result type is `Option[u.Elem]` — chapter 03's dependent method type — so
it is whatever the instance found says it is: `firstOf("abc")` is an
`Option[Char]` and `firstOf(List(1, 2))` an `Option[Int]`, statically. The
compiler has computed a type by searching.

For that, the instance's type must *say* what `Elem` is. That is what the
refinement `Elements[C] { type Elem = E }` (chapter 01) does, and `Aux` is the
conventional name for the alias that writes it. A conditional instance can go
further and compute its member from the members of its premises — which is how
a search over types becomes a function over types, and what exercise 04 asks
for.

The evidence types from chapter 01 are the same mechanism: `A =:= B` and
`A <:< B` are type classes with instances the standard library provides only
when the relation holds. `summon[Int <:< AnyVal]` is an ordinary given search.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch06typeclassesgivens/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

**Importing with `*` and expecting the givens.** The compiler knows exactly
what happened, and says so:

```text
No given instance of type Show[Widget] was found for parameter x of method summon in object Predef

Note: given instance widgetShow in object Orphans was not considered because it
was not imported with `import given`.
```

**Asking for the instance of a subtype.** A type class is usually invariant, so
a `Show[Option[Int]]` is not a `Show[Some[Int]]`, and `describe(Some(1))`
infers the narrowest type it can:

```text
No given instance of type Show[Some[Int]] was found for a context parameter of method describe
```

Widen at the call site — `describe(Option(1))` — rather than adding instances
for every subclass.

**Syntax that needs the instance in scope.** An extension method declared
inside the type class — `extension (a: A) def shown: String` — is found through
a *visible* instance, and the companion's instances are not visible, they are
only in the implicit scope. `42.shown` fails outside, with a suggestion:

```text
value shown is not a member of Int, but could be made available as an extension method.

The following import might fix the problem:

  import Show.given_Show_Int
```

Inside a method with `[A: Show]` the instance is a parameter, so it is visible
and `a.shown` works.

**Two rules for one type in the same object.** The ambiguity above. The fix is
not deleting one, and not reordering them — order in the source counts for
nothing — but saying which one is the fallback, by moving it to a parent trait.

**A given that uses itself.** Inside its own right-hand side a given is not a
candidate, so `given Show[Boolean] = b => summon[Show[Boolean]].show(b)` does
not loop forever at runtime; it fails to compile, with the ordinary *"No given
instance of type Show[Boolean] was found"*.

**A given that forgets what it computed.** Declared as a plain
`Elements[String]` — the refinement ascribed away — an instance still
type-checks and is still found, and every result is opaque:

```text
Found:    Option[Plain.given_Elements_String.Elem]
Required: Option[Char]
```

**A custom message for the search.** `@implicitNotFound` on the type class
replaces the generic message; `${A}` is substituted with the type:

```scala
import scala.annotation.implicitNotFound

@implicitNotFound("No Codec for ${A}: define a given Codec[${A}] in ${A}'s companion")
trait Codec[A]
```

## Exercises

| # | asks for |
| --- | --- |
| 01 | a `Show` for lists and options of anything that has one, and a `describe` that only accepts what can be shown |
| 02 | an `Ordering[Version]` found everywhere without an import, and a second one that wins wherever it is imported |
| 03 | a JSON encoder with two rules that both apply to `Int`, where the number rule must win |
| 04 | an `add` whose result type is computed by the instance search — `Int + Double` is a `Double`, and pairs add up component by component |

```bash
sbt "exercises/testOnly typeprog.ch06typeclassesgivens.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [Contextual abstractions](https://docs.scala-lang.org/scala3/reference/contextual/index.html) —
  the reference section: [givens](https://docs.scala-lang.org/scala3/reference/contextual/givens.html),
  [using clauses](https://docs.scala-lang.org/scala3/reference/contextual/using-clauses.html),
  [context bounds](https://docs.scala-lang.org/scala3/reference/contextual/context-bounds.html),
  [importing givens](https://docs.scala-lang.org/scala3/reference/contextual/given-imports.html)
- [Changes in implicit resolution](https://docs.scala-lang.org/scala3/reference/changed-features/implicit-resolution.html) —
  the precise definition of the implicit scope, and the priority rules
  including the Scala 3.5 change to *most general*
- [Type classes](https://docs.scala-lang.org/scala3/book/ca-type-classes.html) in the Scala 3 book
- Odersky, Blanvillain et al., *Simplicitly: foundations and applications of
  implicit function types* — POPL 2018, doi:10.1145/3158130. Why Scala's
  implicits are a *term inference* mechanism, and what that buys.
