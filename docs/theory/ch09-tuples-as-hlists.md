# Chapter 09 — Tuples as HLists

> Status: **done** — `git show ch09`

## Why this exists

Chapter 06's `Add` summed pairs: one instance for `(A1, A2)` against `(B1, B2)`,
built from the instances for the components. A triple needs another instance,
with three premises; a quadruple another. Each arity is a new rule, and the
rules say the same thing each time: *add the first components, then add the
rest*.

"The rest" is the missing idea. Written as `(A1, A2, A3)`, a triple is a flat
thing with three slots, and there is no smaller triple inside it to recurse
on. What the rule wants is a list — a first element and a tail, down to an
empty end — so that one rule for "first, then the rest" and one for "the end"
cover every length.

Lists of *types* are also what a length in a type needs. A vector that knows
it has three elements, and refuses `head` when it has none, has to carry a
number in its type and do arithmetic on it: appending two vectors of lengths 2
and 3 gives one of length 5, and the compiler has to know that 2 + 3 is 5.

Scala 3's tuples are those lists, and its types can count.

## The idea

### A tuple is a list of types

```scala
(Int, String, Boolean)  =:=  Int *: String *: Boolean *: EmptyTuple
```

The two are the same type. `*:` is a type constructor with a head and a tail,
like `::` for `List`, and `EmptyTuple` is the end. The sugar `(A, B, C)` stays
for reading and writing; the structure underneath is what recursion works on.

The same holds for values. `1 *: "a" *: EmptyTuple` is an `(Int, String)`,
and a match on any tuple can take it apart one element at a time:

```scala
def describe(t: Tuple): String = t match
  case EmptyTuple => "end"
  case h *: rest => s"$h, " + describe(rest)
```

This is what shapeless called an **HList**, a heterogeneous list, built as a
library in Scala 2. In Scala 3 it is the tuple itself. A one-element tuple is
`Tuple1[A]`, which is `A *: EmptyTuple`; `(1)` is just `1`.

### The standard library's operations are match types

`scala.Tuple` defines the list operations at the type level, as chapter 07's
match types over `*:`:

- `Tuple.Head[(Int, String)]` is `Int`, `Tuple.Tail` is `String *:
  EmptyTuple`;
- `Tuple.Concat[(Int, String), (Boolean, Char)]` is `(Int, String, Boolean,
  Char)`;
- `Tuple.Map[(Int, String), Option]` is `(Option[Int], Option[String])`;
- `Tuple.Size[(Int, String)]` is `2`;
- and `Reverse`, `Take`, `Drop`, `Zip`, `Filter`, `Contains`, …

Each value-level method on a tuple — `++`, `map`, `size` — is typed by the
matching type-level one.

### Writing one

A new operation is a match type with a case for `EmptyTuple` and a case for
`h *: t`. Replacing every `X` in a tuple with a `Y`:

```scala
type Replace[T <: Tuple, X, Y] <: Tuple = T match
  case EmptyTuple => EmptyTuple
  case X *: t => Y *: Replace[t, X, Y]
  case h *: t => h *: Replace[t, X, Y]
```

`case X *: t` uses the type parameter `X` as a pattern, not a binder — it is
upper-case — so the case matches when the head conforms to `X`. For the next
case to be reached, the head must be provably disjoint from `X`: chapter 07's
rule, and the reason this works for `Int` and `String` and not for two
unsealed traits.

### Values by instances: one rule for the end, one for a head and a tail

A type class for every tuple is two instances:

```scala
given Show[EmptyTuple] = _ => "()"
given [H, T <: Tuple] => (head: Show[H], tail: Show[T]) => Show[H *: T] = …
```

Asked for a `Show[(Int, String, Boolean)]`, the search applies the second rule
three times and the first once — one instance per element, assembled at
compile time, for a tuple of any length. This is the shape shapeless was built
on, and chapter 10's generic derivation is the same idea applied to case
classes.

When the instance computes a type, as chapter 06's `Add` did, the rule for `H
*: T` builds its `Out` from the `Out`s of its premises — `O1 *: O2` — and the
result type is assembled element by element, as long as every instance is
declared with a type that still says what its `Out` is.

### Values by expansion: `inline` over `*:`

Chapter 08's `inline match` recurses on a tuple type the same way:

```scala
inline def typeNames[T <: Tuple]: List[String] = inline erasedValue[T] match
  case _: EmptyTuple => Nil
  case _: (h *: t) => nameOf[h] :: typeNames[t]
```

Instances are open — anyone can add one for their own type. An `inline`
recursion is closed, and needs no instance at all.

### Counting in types: Peano numbers

A natural number as a type is the other classic list: `Zero`, or the successor
of a number.

```scala
sealed trait Nat
sealed trait Zero extends Nat
sealed trait Succ[N <: Nat] extends Nat

type _3 = Succ[Succ[Succ[Zero]]]

type Twice[N <: Nat] <: Nat = N match
  case Zero => Zero
  case Succ[n] => Succ[Succ[Twice[n]]]
```

Arithmetic is recursion on the structure: a case for `Zero` and a case for
`Succ[n]`, where the binder `n` is the predecessor. It needs nothing but match
types, it works on the *shape* of a number, and a number of three hundred
nested `Succ`s is no trouble for it.

### Counting in types: `scala.compiletime.ops`

Scala 3 also does arithmetic on literal types directly:

```scala
import scala.compiletime.ops.int.*

summon[1 + 2 =:= 3]
summon[Tuple.Size[(Int, String)] =:= 2]
```

`+`, `-`, `*`, `<`, `>`, `==` and the rest are type constructors the compiler
evaluates when their arguments are literals. A comparison is a `Boolean`
literal type, and `N > 0 =:= true` is evidence that can be asked for in a
`using` clause: a method that exists only for a positive `N`.

For an abstract `N`, nothing is evaluated and nothing is known — not even that
`N + 0` is `N`. Peano's numbers can be taken apart by pattern; these are just
numbers. Chapter 04's literal types and the tuple sizes above are these, which
is why the standard library uses them; Peano is still the clearer way to
*define* arithmetic, and the one that works where the numbers are not known.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch09tuplesashlists/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

**`EmptyTuple` is an object.** Its type is `EmptyTuple.type`, and that is how
the compiler prints it: a failed search reports *"no implicit values were
found that match type Add.Aux[EmptyTuple.type, Int *: EmptyTuple.type, O2]"*
— which, read that way, says exactly where two tuples of different lengths
stopped matching.

**Arithmetic on an abstract number.** `compiletime.ops` evaluates literals and
nothing else:

```text
Cannot prove that N + (0 : Int) =:= N.
```

**Evidence that reads badly.** A method guarded by `N > 0 =:= true`, called on
an empty vector:

```text
Cannot prove that (0 : Int) > (0 : Int) =:= (true : Boolean).
```

It is the right error, stated as the proof that failed. When the message
matters, a dedicated type class with `@implicitNotFound` (chapter 06) says it
in words.

**An instance that forgets its `Out`.** Chapter 06's trap, multiplied: the rule
for `H *: T` needs the `Out` of both premises, and one instance declared
without it turns the whole result into `?1.Out`:

```text
Found:    ?1.Out
Required: (Int, Int)
```

**`Replace` for traits.** A type-level operation that compares elements stops
at the first element it cannot prove different — see chapter 07's
disjointness.

## Exercises

| # | asks for |
| --- | --- |
| 01 | `Plus` and `Times` on Peano numbers |
| 02 | `IndexOf`, the position of a type in a tuple — or `-1` — computed with `compiletime.ops` |
| 03 | chapter 06's `add`, for tuples of any length, nested or not |
| 04 | a vector with its length in its type: `::` adds one, `++` adds the lengths, and `head` does not compile on an empty one |

```bash
sbt "exercises/testOnly typeprog.ch09tuplesashlists.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [`scala.Tuple`](https://github.com/scala/scala3/blob/main/library/src/scala/Tuple.scala) —
  the source, where every type-level operation is a short match type
- [`scala.compiletime.ops`](https://scala-lang.org/api/3.x/scala/compiletime/ops.html) —
  the literal-type arithmetic
- [The Type Astronaut's Guide to Shapeless](https://underscore.io/books/shapeless-guide/) —
  its early chapters build HLists and derive instances for them: Scala 2, and
  the same recursion as here
