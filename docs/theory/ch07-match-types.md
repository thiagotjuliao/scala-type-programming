# Chapter 07 — Match types

> Status: **done** — `git show ch07`

## Why this exists

A method that doubles what it is given — an `Int` into a `Long`, so it cannot
overflow; a `String` by repeating it; an `Option` by pairing its content with
itself:

```scala
def double(x: Int | String | Option[?]): Any = x match
  case i: Int => i.toLong * 2
  case s: String => s + s
  case o: Option[?] => o.map(a => (a, a))

val n = double(21) // an Any: the Long was known, and is lost
```

The body knows exactly what comes out of each case. The signature cannot say
it, because what comes out depends on the *type* of what went in, and a
method's result type is written once, for every argument. Overloading gets
closer — one `double` per type — and stops working the moment the choice has to
be made by a caller that is itself generic.

Chapter 06 had one answer: a type class with a type member, an instance per
case, and a dependent result type `u.Out`. It works, and it is a lot of
machinery for a table with three rows — one instance per row, each spelling
its `Out` out, found by a search that exists only to look the row up.

What is wanted is to write the table: *an `Int` gives a `Long`, a `String` a
`String`, an `Option[a]` an `Option[(a, a)]`*, as a type. That is a **match
type** — a `match` whose scrutinee and results are types, evaluated by the
compiler during type checking:

```scala
type Doubled[X] = X match
  case Int => Long
  case String => String
  case Option[a] => Option[(a, a)]
```

It is a function from types to types, and the first one in this course that is
written *as* a function: chapter 06's `Add` computed a type too, but as a side
effect of a search. Chapter 05's units stopped at `Meters / Seconds` because
cancelling `Seconds` against `Seconds` needed exactly this. Chapters 08 to 10
lean on it throughout — the standard library's `Tuple` operations are match
types.

## The idea

### A match on types

`Doubled[Int]` is not a type the compiler stores; it is one it computes. To
**reduce** `Doubled[X]` for a known `X`, it takes the cases in order and picks
the first whose pattern `X` conforms to:

- `Doubled[Int]` is `Long`;
- `Doubled[Option[Char]]` is `Option[(Char, Char)]` — the pattern `Option[a]`
  *binds* `a`, like a variable in a value pattern, and the result uses it;
- `Doubled[Some[Char]]` is `Option[(Char, Char)]` as well: the test is
  `Some[Char] <: Option[a]`, subtyping, not equality.

A binder is a lower-case name, as in a value pattern. `case List[T] => T`
does not bind `T`; it looks for a type called `T`, and there is none.

### When the compiler cannot decide

Picking the first case that matches is half of the algorithm. The other half is
what it does with a case that does *not* match: it moves past it only if it can
prove that no type could ever satisfy both — that `X` and the pattern are
**disjoint**. If it cannot prove that, it stops, and the match type stays as
written, unreduced. It is *stuck*.

Why not simply move on? Because the case might match *later*. In a generic
method, `Doubled[A]` for a type parameter `A` does not match `case Int` — `A`
is not known to be an `Int` — and it is not disjoint from it either, since `A`
may be instantiated to `Int` at the call site. Skipping the case would reduce
`Doubled[A]` to something that is wrong for `A = Int`. So it stays
`Doubled[A]`, and the compiler says why when that is a problem:

```text
Found:    Doubled[A]
Required: Long

Note: a match type could not be fully reduced:

  trying to reduce  Doubled[A]
  failed since selector A
  does not match  case Int => Long
  and cannot be shown to be disjoint from it either.
  Therefore, reduction cannot advance to the remaining cases

    case String => String
    case Option[a] => Option[(a, a)]
```

What the compiler can prove disjoint:

- two **classes**, neither extending the other — a class has one superclass
  chain, so nothing can be both an `Int` and an `Option`;
- a **final** class, or a **sealed** type whose subtypes are all known, from
  what it does not extend — two sealed traits with no subtypes at all are
  disjoint;
- two different **literal types** — `0` and `3` (chapter 04).

What it cannot:

- two unsealed **traits**: nothing stops a class from extending both;
- a type parameter or abstract type from anything it might turn out to be;
- a literal from its own widened type: `0` *is* an `Int`, so `Int` is not
  disjoint from the pattern `0`.

That last list is where stuck match types come from in practice.

A match type with no case for its argument is stuck in the same way:
`Doubled[Boolean]` matches none of the cases, and stays `Doubled[Boolean]`.
There is no implicit `Nothing` at the end.

### Recursion

A case can apply the match type being defined, to something smaller. The last
element type of a tuple:

```scala
type Last[T <: Tuple] = T match
  case x *: EmptyTuple => x
  case _ *: rest => Last[rest]
```

`Last[(Int, String, Boolean)]` is `Boolean`: two steps down the tuple, then the
first case. This is exactly the standard library's `Tuple.Last`, and
`Tuple.Head`, `Tuple.Concat` and the rest are match types of the same kind —
chapter 09 is built on them.

A catch-all `case _` matches anything, so it goes last — a case after it is
never reached — and it fires only once every case before it is ruled out as
disjoint. For an abstract argument, that is never.

A recursion that never gets smaller does not loop forever; the compiler gives
up with *"Recursion limit exceeded"* (see *Pitfalls*).

### Values of a match type

The type is half of `double`. The body has to produce a value of `Doubled[X]`,
for an `X` it does not know — and in general there is no way to: `Doubled[X]`
is stuck inside the method, and a `Long` is not a `Doubled[X]`.

A `match` expression gets special treatment. When it mirrors the match type
case for case, the compiler checks each branch against the reduced type:

```scala
def double[X <: Int | String | Option[?]](x: X): Doubled[X] = x match
  case i: Int => i.toLong * 2              // checked against Long
  case s: String => s + s                  // against String
  case o: Option[a] => o.map(a => (a, a))  // against Option[(a, a)]

double(21) // 42L, a Long — statically
```

The conditions are strict, and each one is a way to lose it:

- the patterns are **typed patterns**, `x: T`, with the same types as the
  match type's patterns, in the **same order**, and **the same number** of
  them — an extra `case _ => ???` at the end breaks every branch, not just the
  extra one;
- no guards;
- the value matched on has a type that conforms to the match type's
  scrutinee — here `x: X`, matched against `X match`.

When any of them fails, each branch is back to being checked against a stuck
`Doubled[X]`, and each one is rejected with the note above.

The bound on `X` is chapter 04's union, and it is doing a job: without it,
`double(true)` compiles. `Doubled[Boolean]` is stuck, and a stuck type is a
type like any other — the call type-checks, silently or with a warning
(*"Match type reduction failed since selector Boolean matches none of the
cases"*, where a `val`'s type is inferred from it), and throws a `MatchError`
when it runs. With the bound it is rejected at the call.

### Bounds

Generic code holding an unreduced `Doubled[X]` can do nothing with it that
`Any` cannot do — the reference is explicit that an undeclared bound is `Any`.
A bound written on the match type says more, and is checked against every
case:

```scala
type Name[X] <: CharSequence = X match
  case String => String
  case Int => StringBuilder
```

Now `Name[X]` is a `CharSequence` even while it is stuck, and `.length` works
on it in generic code. A case whose result is not a `CharSequence` is rejected
where it is written.

(Scala 3.9 was observed to be more generous than the reference: a match type
with no bound, every case of which was an `Option`, allowed `.isDefined` on an
unreduced application; with cases returning `Int` and `String`, nothing was
allowed. Rely on the declared bound, which is specified, not on the inference,
which is not — checked 2026-09-25, Scala 3.9.0.)

### Match types and type classes

Chapter 06's `Add` as a match type is a table over a pair of types:

```scala
type Sum[A, B] = (A, B) match
  case (Int, Int) => Int
  case (Int, Double) => Double
  // …
  case ((a1, a2), (b1, b2)) => (Sum[a1, b1], Sum[a2, b2])
```

`Sum[(Int, String), (Double, String)]` is `(Double, String)`, with no instance
and no `Aux`. What it does not give is the *value*. A `match` on `(a, b)` with
patterns `p: (Int, Int)` compiles — and cannot work, because a pair's element
types are erased: at runtime `(1, 2.5)` is a `Tuple2` like any other, the first
case takes it, and unboxing `2.5` as an `Int` throws a `ClassCastException`.
The compiler says as much, as a warning:

```text
the type test for (Int, Int) cannot be checked at runtime because its type arguments can't be determined from (A, B)
```

`double` works because `Int`, `String` and `Option` are classes a runtime test
can tell apart. Where only erased type arguments tell the cases apart, the
value needs a dispatch that happens at compile time — a type class (chapter
06), or an `inline match` resolved during expansion (chapter 08).

The trade-off is otherwise a design one. A match type is **closed**: its cases
are in one place, in order, and adding one means editing it. A type class is
**open**: anyone can add an instance for their own type, and priority settles
overlaps. A match type needs no value to compute with, which is why the
compiler's own type operations are written as match types.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch07matchtypes/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

**A trait where a class was meant.** Two unsealed traits are never disjoint,
so a match type over them stops at the first case that does not match:

```text
Cannot prove that Volume[Quiet] =:= ("quiet" : String).

Note: a match type could not be fully reduced:

  trying to reduce  Volume[Quiet]
  failed since selector Quiet
  does not match  case Loud => ("loud" : String)
  and cannot be shown to be disjoint from it either.
  Therefore, reduction cannot advance to the remaining case

    case Quiet => ("quiet" : String)
```

Seal them, or make them classes.

**An upper-case binder.** `case List[T] => T` reads as a reference to a type
`T`:

```text
Not found: type T
```

**The same binder twice.** `case (a, a) => true` does not test that the two
components are equal; it does not compile — *"duplicate pattern variable: a"*.
Two types are compared with a match inside the result, whose pattern is the
type the outer pattern bound: `case (a, b) => b match { case a => … }`. That is
a subtype test, `b <: a`, which for disjoint types is the same thing.

**No case, no error at the definition.** A match type is a partial function.
Applied outside its domain it does not fail, it stays unreduced, and the
failure turns up wherever the result is used:

```text
Note: a match type could not be fully reduced:

  trying to reduce  Doubled[Boolean]
  failed since selector Boolean
  matches none of the cases
```

**A mirroring `match` that is almost right.** A default case added to be safe,
or two cases swapped, and every branch fails:

```text
Found:    Long
Required: Doubled[X]
```

followed by the stuck-match note. Mirror the match type exactly.

**Runaway recursion.**

```text
Recursion limit exceeded.
Maybe there is an illegal cyclic reference?
If that's not the case, you could also try to increase the stacksize using the -Xss JVM option.
A recurring operation is (inner to outer):
```

followed by a trace of `reduce type  List[List[List[…` nested past the width
of the terminal. A recursive case must make its argument smaller;
`Grow[List[X]]` never does. A case that recurses with no pattern at all,
`case _ => Grow[List[X]]`, is caught earlier, as *"illegal cyclic type
reference"*.

**Type tests the runtime cannot make.** A `match` on values with patterns like
`p: (Int, Int)` type-checks and warns (*"cannot be checked at runtime"*,
*"Unreachable case"*) — then takes the wrong branch. The warning is the only
notice, so compile with warnings visible.

## Exercises

| # | asks for |
| --- | --- |
| 01 | `Unwrap`, which takes one layer off an `Option`, an `Either` or a `List`, and leaves anything else alone |
| 02 | `Leaf`, which takes *every* layer off, recursively |
| 03 | a `first` whose result type is computed from its argument's, with a body that needs no cast |
| 04 | chapter 05's units, now with `*` — speed times time is a distance, and the compiler does the cancelling |

```bash
sbt "exercises/testOnly typeprog.ch07matchtypes.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [Match types](https://docs.scala-lang.org/scala3/reference/new-types/match-types.html) —
  the reference: the reduction rules, disjointness, and the conditions for
  typing a `match` expression with a match type
- [SIP-56, *Proper specification for match types*](https://docs.scala-lang.org/sips/match-types-spec.html) —
  the rules as they have been since Scala 3.4, including which patterns are
  *legal*
- Blanvillain, Brachthäuser, Kjaer, Odersky, *Type-level programming with
  match types* — POPL 2022. The formal account, and why disjointness is the
  whole trick.
- [`scala.Tuple`](https://github.com/scala/scala3/blob/main/library/src/scala/Tuple.scala) —
  the standard library's match types, read as examples
