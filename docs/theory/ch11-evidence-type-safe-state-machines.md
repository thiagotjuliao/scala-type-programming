# Chapter 11 — Evidence & type-safe state machines

> Status: **done** — `git show ch11`

## Why this exists

An HTTP request builder, the usual way:

```scala
final class RequestBuilder:
  private var url: Option[String] = None
  private var method: Option[String] = None
  def url(u: String): this.type = { url = Some(u); this }
  def method(m: String): this.type = { method = Some(m); this }
  def build(): Request =
    Request(url.getOrElse(sys.error("no url")), method.getOrElse(sys.error("no method")))
```

It has three ways to be misused, and every one of them compiles: `build()`
before the URL is set, `build()` before the method is set, and `url` called
twice, the second silently winning. Each is found by a test, if there is a
test, or in production, if there is not.

Chapter 05 put a *state* in a type parameter: a `Connection[Open]` has `send`,
a `Connection[Closed]` does not. A builder is the same idea with the state
changing at each call — `url` takes a builder with no URL and returns one with
a URL — and with the rule for `build` depending on *several* facts at once.
A state machine is the general case: states, events, and a table of which
event is allowed in which state.

All of it rests on one question the compiler can answer: *is there evidence
that this fact holds?* — and, sometimes, *is there evidence that it does not?*

## The idea

### Evidence as a precondition

Chapter 01's `=:=` and `<:<` are type classes whose instances exist only when
the relation holds (chapter 06). Asked for in a `using` clause, they make a
method callable only when a fact about the type parameters is true:

```scala
final class Pairs[A](items: List[A]):
  def toMap[K, V](using ev: A <:< (K, V)): Map[K, V] = items.map(ev).toMap
```

`Pairs(List(1 -> "a")).toMap` compiles; `Pairs(List(1, 2)).toMap` does not —
*"Cannot prove that Int <:< (K, V)."* The evidence is also a function, `A =>
(K, V)`, which is how the body turns each element into a pair without a cast.
Any type class works the same way: `def total(using Numeric[A])` is a method
only numbers have.

### State that changes with each call

A builder carries one phantom parameter per fact it tracks. With literal
`Boolean` types (chapter 04) as the facts:

```scala
final class Pizza[HasSize <: Boolean, HasBase <: Boolean] private (…):
  def size(s: Int)(using HasSize =:= false): Pizza[true, HasBase] = …
  def base(b: String)(using HasBase =:= false): Pizza[HasSize, true] = …
  def bake(using HasSize =:= true, HasBase =:= true): Baked = …
```

Each setter returns the builder with its fact flipped to `true`, and demands it
was `false` — so it can be called once. `bake` demands both. The order of the
calls is free; forgetting one, or repeating one, is a compile error. At runtime
it is an ordinary immutable object: the parameters are erased.

### Transitions as data

A state machine's rules are a table: *from this state, this event leads to
that state*. Chapter 06's type class with a type member is exactly a table
row, and a missing row is a missing instance:

```scala
trait Transition[S, E]:
  type Next

object Transition:
  type Aux[S, E, N] = Transition[S, E] { type Next = N }
  given Aux[Green, Timer, Amber] = …
  given Aux[Amber, Timer, Red] = …

final class Light[S]:
  def on[E](using t: Transition[S, E]): Light[t.Next] = Light()
```

`Light.green.on[Timer].on[Timer]` is a `Light[Red]`, computed by the search;
an event with no row for the current state does not compile. The table is open,
like any type class — a new transition is a new given — and the next state is
computed, not written by the caller.

The same table can be a match type instead (chapter 07): `Next[S, E]`, with a
case per row. It is closed — every row in one place — and the state it
computes is a plain type, which matters for the next section.

### A message that says what went wrong

The failures above are correct and unreadable: *"Cannot prove that (false :
Boolean) =:= (true : Boolean)."* for a missing field, *"No given instance of
type Transition[Red, Walk]"* for a forbidden event. `@implicitNotFound` on a
type class (chapter 06) replaces the message, with the type arguments
substituted:

```scala
@implicitNotFound("a light that is ${S} does not react to ${E}")
trait Transition[S, E]
```

For the builder, a dedicated evidence type — a type class with an instance
only for `true`, and its own message — says *"the pizza has no size yet"*
instead of a proof about Booleans.

`${S}` is the state as the failed call's type spells it. After a transition
found by a type class, that is `t.Next`, a path to the found instance's
member, and the message prints the path:

```text
a light that is Walkthrough.Transition.
  given_Aux_Amber_Timer_Red.Next does not react to Walkthrough.Walk
```

A state computed by a match type reduces to the state type itself, and the
message names it.

### `NotGiven`: evidence that something is absent

`scala.util.NotGiven[T]` has an instance exactly when a search for `T` fails.
It turns a question the search cannot ask — *is there **no** Numeric?* — into
one it can:

```scala
given numeric: [A: Numeric] => Json[A] = …
given quoted: [A: Show] => NotGiven[Numeric[A]] => Json[A] = …
```

Chapter 06 ranked these two rules with a low-priority trait; here the second
simply does not apply where the first does, and both live in one object.
`NotGiven[A =:= B]` says two types differ, and `NotGiven[Tuple.Contains[T, A]
=:= true]` says a tuple (chapter 09) does not hold `A` yet — a collection that
refuses a second element of the same type.

A search that fails because it is *ambiguous* is still a failure, so `NotGiven`
holds then too — which is rarely what was meant.

### What stays at runtime

Nothing of it. The phantom parameters and the evidence are erased; what runs is
the plain object and the plain methods. The guarantee is exactly as strong as
the type system's view of the code, and a cast, or a builder constructed
outside its smart constructor, is outside that view — which is why the
constructors here are private.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch11evidencetypesafestatemachines/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

**Messages about the proof, not the mistake.** A builder whose `build` is
guarded by `HasUrl =:= true`, called too early:

```text
Cannot prove that (false : Boolean) =:= (true : Boolean).
```

Nothing says *which* fact. One evidence type per fact, each with an
`@implicitNotFound`, is the difference between a type-safe API and a usable
one.

**`NotGiven`'s own message.** Just as opaque:

```text
No given instance of type scala.util.NotGiven[Tuple.Contains[(String, Int), Int] =:= (true : Boolean)] was found for parameter x$2 of method add in class Registry
```

**`NotGiven` and ambiguity.** Two instances of `T` in scope, neither preferred:
`summon[T]` fails as ambiguous, and `summon[NotGiven[T]]` *succeeds*. The
negation is of "the search succeeded", not of "an instance exists".

**A state that is a path.** A transition typed `Light[t.Next]` puts the path
to an instance's member in the next error message, where the state's name was
wanted. Compute the state with a match type when the messages matter.

**Names in `@implicitNotFound` are printed as the call site sees them.** From
outside the object that defines them, `${S}` and `${E}` are fully qualified:

```text
cannot typeprog.ch11evidencetypesafestatemachines.Exercise04.Lock a door that is typeprog.ch11evidencetypesafestatemachines.Exercise04.Opened
```

Write the message so that it reads well either way.

**A public constructor.** A `Pizza[true, true]` built directly skips every
check. Keep the constructor private and the starting state in a factory.

## Exercises

| # | asks for |
| --- | --- |
| 01 | a `Stack` whose `sum`, `flatten` and `unzip` exist only for stacks of the right kind of element |
| 02 | chapter 06's JSON rules, both in one object, with no priority trick: `NotGiven` |
| 03 | a request builder where `build` needs a URL and a method, each set exactly once, and says which one is missing |
| 04 | a door with states and events, where a forbidden event does not compile and says why |

```bash
sbt "exercises/testOnly typeprog.ch11evidencetypesafestatemachines.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [Negated givens](https://docs.scala-lang.org/scala3/reference/contextual/previous-givens.html#negated-givens) —
  `NotGiven` is described in the reference's page on the given syntax before
  Scala 3.6; it did not move with the rest when the syntax changed
- [`@implicitNotFound`](https://www.scala-lang.org/api/current/scala/annotation/implicitNotFound.html) —
  the annotation and its `${…}` substitution
- Yaron Minsky, *Effective ML* — the talk that popularised "make illegal states
  unrepresentable"; the phantom builder is that slogan applied to a sequence
  of calls
