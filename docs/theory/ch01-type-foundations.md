# Chapter 01 — Type system foundations

> Status: **done** — `git show ch01`

## Why this exists

Every later chapter is a sentence written in the vocabulary of this one. Match
types are a computation *over* subtyping; given resolution is a search whose
success condition is conformance; derivation reads type members off a `Mirror`.
None of that is approachable while `+A` is still something copied from an
existing signature because the compiler complained.

The four ideas here — subtyping, variance, bounds, type members — are also the
only ones in the repository that Scala 2 had in the same shape. Everything after
chapter 04 is new machinery; this is the ground it stands on.

## The idea

### Subtyping is a permission, not a taxonomy

`Dog <: Animal` says nothing about animals. It says: wherever an `Animal` is
expected, a `Dog` may be supplied, and no code downstream is allowed to notice
the difference. That is the Liskov reading, and it is the only reading that
makes the rest mechanical.

### Variance is how that permission travels into a type constructor

Given `Dog <: Animal`, is `F[Dog] <: F[Animal]`? The annotation on `F`'s
parameter is the answer, and the answer is determined by what `F` does with its
`A`:

| `A` appears | position | annotation | meaning |
| --- | --- | --- | --- |
| only as a result | covariant | `+A` | `F[Dog] <: F[Animal]` |
| only as a parameter | contravariant | `-A` | `F[Animal] <: F[Dog]` |
| as both | invariant | `A` | neither |

The short form is the **get/put principle**: a parameter you only get out may be
covariant, a parameter you only put in may be contravariant, a parameter you do
both to must be invariant. `Function1[-T, +R]` is the canonical case —
arguments go in, results come out — and it is worth re-deriving from the table
above rather than memorising.

The contravariant row is the one that reads backwards on first encounter, and it
is not backwards. A `Sink[Animal]` accepts *any* animal, so it can stand in
anywhere a `Sink[Dog]` was required; the wider the appetite, the more contexts
it fits. `Sink[Animal] <: Sink[Dog]` is the formal way of saying so.

Invariance is not a failure to decide. `Array[+A]` would let this compile:

```scala
val dogs: Array[Dog]       = Array(Dog("Rex"))
val animals: Array[Animal] = dogs      // if Array were covariant
animals(0) = Cat("Tom")                // and now dogs(0) is a Cat
```

Anything that both gets and puts — an array, a `var` field, a mutable buffer —
has no safe variance, and the compiler enforces exactly that. When it says
*"covariant type A occurs in contravariant position"*, it has found a put.

### Bounds constrain a parameter without fixing it

`A <: Animal` is the upper bound: `A` may be anything that conforms to `Animal`,
and the body may therefore use `Animal`'s members. `B >: A` is the lower bound,
and it is the standard repair for the collision between covariance and a method
that wants to put:

```scala
def push[B >: A](b: B): Stack[B]
```

Nothing is put into the existing structure — a wider one is returned instead —
so covariance survives. Pushing a `Cat` onto a `Stack[Dog]` does not corrupt it;
it yields a `Stack[Animal]`, because `Animal` is the least upper bound the
compiler infers for `B`. `List.::` is this signature, and `Nil` being a
`List[Nothing]` usable as any `List[X]` is covariance doing the same work at the
other end.

### Type members are the other way to attach a type

```scala
trait Repo:
  type Id
  type Entity
  def find(id: Id): Option[Entity]

type DogRepo = Repo { type Id = Long; type Entity = Dog }
```

A type *parameter* is supplied by the caller and appears in the type's name; a
type *member* is supplied by the implementation and can stay abstract. The
refinement `Repo { type Id = Long }` fixes a member in the type itself, without
declaring a new trait, and is an ordinary subtype of `Repo`.

This is the feature the second half of the repository leans on hardest: chapter
03's path-dependent types are members selected through a value, and chapter 10's
`Mirror` publishes a case class's shape as `MirroredElemTypes` — a type member
the compiler fills in.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch01foundations/Walkthrough.scala).

## Pitfalls

**Annotating for the error message rather than the design.** Adding `+A` until
the compiler stops complaining, then adding `[B >: A]` until it stops again,
usually arrives at the right signature for the wrong reason. Decide get-or-put
first; the annotation follows.

**Reading contravariance as "the reverse, for symmetry".** It falls out of
substitutability. Re-derive it from a concrete call whenever it feels arbitrary.

**Expecting variance to survive mutation.** A covariant wrapper around a `var`
does not compile, and the fix is not `private[this]` — it is accepting that the
type is invariant.

**Confusing `<:` with `<:<`.** `A <: B` is a bound, written in a type parameter
list and checked at declaration. `A <:< B` is a *value* — evidence, summoned at
the call site — used when the relation cannot be stated in the signature.
Chapter 06 revisits it as an ordinary given instance.

## Exercises

| # | asks for |
| --- | --- |
| 01 | annotate `Source`, `Sink` and `Channel` with the strongest variance each allows |
| 02 | refine `Repo`'s type members so `find(1L): Option[Dog]` type-checks |
| 03 | implement three methods using only the `=:=` / `<:<` evidence they are handed |
| 04 | make an immutable `Stack` covariant, and keep `push` compiling afterwards |

```bash
sbt "exercises/testOnly typeprog.ch01foundations.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [Variances](https://docs.scala-lang.org/tour/variances.html) — the tour's page, and the
  shortest correct statement of the get/put principle
- [Upper type bounds](https://docs.scala-lang.org/tour/upper-type-bounds.html) and
  [lower type bounds](https://docs.scala-lang.org/tour/lower-type-bounds.html)
- [Abstract type members](https://docs.scala-lang.org/tour/abstract-type-members.html) —
  and the [Scala 3 reference on new types](https://docs.scala-lang.org/scala3/reference/new-types/index.html)
  for where they lead
- [`scala.Predef`](https://github.com/scala/scala3/blob/main/library/src/scala/Predef.scala) —
  `=:=` and `<:<` are ordinary library code, roughly twenty lines of it, and reading the
  source once removes most of the mystique
- Odersky, Spoon, Venners, *Programming in Scala* — chapter 19 is still the clearest
  long-form treatment of variance and bounds
