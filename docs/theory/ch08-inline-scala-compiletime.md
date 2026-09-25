# Chapter 08 — `inline` & `scala.compiletime`

> Status: **done** — `git show ch08`

## Why this exists

Chapter 05's `Port` is validated at runtime, because that is where its
constructor runs:

```scala
opaque type Port = Int
object Port:
  def apply(n: Int): Option[Port] = Option.when(1 <= n && n <= 65535)(n)

val http = Port(8080).get // a number in the source, checked when the program runs
```

`8080` is a literal. Whether it is a valid port is known the moment it is
typed, and yet the check waits for a run, and the caller holds an `Option` it
knows is a `Some`. Chapter 04 made `8080` a *type*; what is missing is a way to
read that type back as a value while compiling, and to fail the compilation
when it is out of range.

Chapter 07 ended on the same gap from the other side. A match type computes
`Sum[Int, Double] = Double`, but the value — adding an `Int` to a `Double` —
cannot be picked by a runtime `match`, because the types that tell the cases
apart are erased by then.

Both need code that runs *in the compiler*: code that sees the static types at
a call site, picks a branch by them, reads a literal type as a value, looks up
an instance, and reports an error — and leaves behind only the branch it chose.
That is `inline`, and the operations it can use are in `scala.compiletime`.

## The idea

### An `inline` method is expanded where it is called

```scala
inline def twice(inline flag: Boolean): String =
  inline if flag then "yes" else "no"

twice(true) // compiles to "yes"
```

An `inline def` is not called; its body is copied into each call site, with the
arguments substituted, while the call site is being compiled. That alone is an
optimisation. What makes it a tool is what can happen *during* the copy:

- an **`inline` parameter** is substituted as the expression the caller wrote,
  not as a value computed before the call — here, the literal `true`;
- an **`inline if`** is decided on the spot, and only the chosen branch is
  kept. Its condition must then be a constant; given a runtime `Boolean`, the
  compiler refuses, *"Cannot reduce `inline if` because its condition is not a
  constant value: b"*.

The body is still type-checked where it is *defined*, against the result type
it declares. That matters in a moment.

### `inline match`: a branch chosen by static type

```scala
inline def kind[A](a: A): String = inline a match
  case _: Int => "an Int"
  case _: String => "a String"

kind(1)   // "an Int"
kind("s") // "a String"
```

An `inline match` is reduced at the call site against the *static* type of the
scrutinee — `Int`, once `kind(1)` is expanded — and only the first matching
case survives. No runtime test is made, so nothing is erased that the choice
depends on. With no matching case, the call does not compile:

```text
cannot reduce inline match with
 scrutinee:  "s" : ("s" : String)
 patterns :  case _:Int
```

Chapter 07's `Sum` was a table the runtime could not consult, because the
runtime cannot see type arguments. An `inline match` on the arguments is
decided before there is a runtime — exercise 03 is that table, with values.

### `transparent`: the expansion decides the type

```scala
transparent inline def next[A](a: A): Any = inline a match
  case i: Int => i + 1
  case c: Char => (c + 1).toChar
  case l: Long => l + 1L

val n: Int = next(41)    // an Int
val c: Char = next('a')  // a Char
```

`next` declares `Any`, and still `next(41)` is an `Int`. A **`transparent
inline`** method's call has the type of its *expansion*, not of its
declaration; the declared type is only an upper bound. Without `transparent`,
the call would be an `Any`, as declared, however precise the code it expands
to. When the arguments are literals the expansion is folded too, and the type
is a literal type: chapter 07's sum of `1` and `2.5`, written this way, is a
`(3.5d : Double)`.

Could the precise type be declared instead, as a match type computing `Int`
from `Int` and `Char` from `Char`? Here, yes: in each case of an `inline match`
on the parameter itself, the typed pattern tells the compiler that `A` is an
`Int` in that branch, and the match type reduces. It stops working as soon as
the scrutinee is anything else. A match on a pair `(a, b)` tells the compiler
nothing about `A` and `B`, the declared `Sum[A, B]` stays stuck at the
definition, and every branch is rejected, as in chapter 07. `transparent` needs
no table at all: the type falls out of expanding the code that computes the
value.

### `erasedValue`: matching on a type with no value

`inline match` needs a scrutinee. When there is only a type, `erasedValue[T]`
stands in for a value of it:

```scala
inline def typeName[T]: String = inline erasedValue[T] match
  case _: Int => "Int"
  case _: String => "String"
  case _: (a, b) => "(" + typeName[a] + ", " + typeName[b] + ")"
  case _ => error("typeName does not know this type")

typeName[(Int, (String, Int))] // "(Int, (String, Int))"
```

It is never evaluated — it exists to be matched on, and an `inline match` only
looks at its type. Patterns can bind type variables, `(a, b)`, and the chosen
case can recurse on them.

A method that returns a `T` from such a match has one more thing to deal with:
the body is checked at the definition, where `T` is not known to be `Int`, so
`0` is not a `T` there. The usual answer is a cast in each case,
`0.asInstanceOf[T]` — after expansion, a cast from a type to itself.

### `constValue`: a literal type as a value

```scala
inline def percent[N <: Int]: Double =
  inline if constValue[N] > 100 then error("more than 100 percent")
  else constValue[N] / 100.0

percent[25]  // 0.25
// percent[150] — more than 100 percent
```

`constValue[N]` is the value of a literal type — `25` for `N = 25`. It is a
constant, so `inline if` can decide on it, and `error` turns the wrong branch
into a compile error with the message given. For a type that is not a literal
there is no value to read: *"Int is not a constant type; cannot take
constValue"*.

`constValue` also reads a type that was bound by a pattern: `inline
erasedValue[T] match { case _: Tagged[n] => constValue[n] }` turns
`Tagged["user"]` into `"user"`.

### `summonInline` and `summonFrom`: instances, looked up at expansion

A plain `summon[Show[A]]` in an `inline` body is resolved where the body is
written, for an `A` nobody knows yet, and fails there. `summonInline[Show[A]]`
defers the search to the expansion, where `A` is known; failing, it reports the
usual *"No given instance of type Show[String] was found"* at the call site.

`summonFrom` goes further and branches on whether a search succeeds:

```scala
inline def tidy[A](xs: List[A]): List[A] = summonFrom {
  case o: Ordering[A] => xs.sorted(using o)
  case _ => xs
}

tidy(List(3, 1, 2))            // List(1, 2, 3): Int has an Ordering
tidy(List(Widget(), Widget())) // unchanged: Widget has none
```

The cases are tried in order, each an instance search; the first that finds one
is kept. It is chapter 06's priority written out, in one place — and it can
fall back to something that is not an instance at all.

### `error`: the message is part of the design

`scala.compiletime.error(msg)` fails the compilation where the expansion
reaches it, with `msg` as the message. It must be a constant string: the error
is raised while compiling, before any string could be built at runtime.

### Inline all the way up

The static types an `inline` body reduces on are the ones at the call site.
Called from a generic method, the call site knows only a type parameter:

```text
cannot reduce inline match with
 scrutinee:  Tuple2.apply[A, A](a, a) : (A, A)
```

Code that calls an `inline` method generically must itself be `inline`, so that
the reduction happens at *its* call site, one level further up — until it
reaches one where the types are concrete.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch08inlinescalacompiletime/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

**Declaring the type the expansion will have.** An `inline` body is checked at
its definition. A declared result type that depends on the type parameters
rejects the branches that are only right after expansion, unless the branch
itself tells the compiler what the parameter is — which a typed pattern on the
parameter does, and a pattern on a pair of parameters does not:

```text
Found:    Int
Required: Sum[A, B]

Note: a match type could not be fully reduced:
```

Declare a bound and make the method `transparent`, or cast in each branch
(`0.asInstanceOf[T]`), knowing the cast disappears on expansion. A result of
type `T` from an `erasedValue[T]` match needs the cast even so: the pattern
`_: Int` says `T` is *an* `Int`, not that an `Int` is a `T`.

**Forgetting `transparent`.** The expansion is precise and the call is not:

```text
Found:    Any
Required: Double
```

**A runtime value where a constant is needed.** `inline if` and `constValue`
decide while compiling; a parameter that is not `inline`, or a type that is not
a literal, gives them nothing to decide on:

```text
Cannot reduce `inline if` because its condition is not a constant value: b
Int is not a constant type; cannot take constValue
```

**Using `erasedValue` as a value.** Outside the scrutinee of an `inline match`
it has nothing to stand for:

```text
method erasedValue is declared as `erased`, but is in fact used
```

**A plain `summon` in an `inline` body.** It is resolved where the body is
written, for a type parameter: *"No given instance of type Show[A] was found for
parameter x of method summon in object Predef"*, at the definition. Use
`summonInline`, which waits for the expansion.

**A generic caller.** `cannot reduce inline match` with a type parameter in the
scrutinee means the method was called from code that does not know the type.
Make that code `inline` too.

**Building a tuple from recursive `transparent` calls.** A pair built from two
recursive calls of a `transparent inline` method that declares `Any` is typed
`(Any, Any)`: the tuple's type arguments are inferred once, at the definition,
from what the inner calls *declare*. The inner expansions are precise; the
tuple around them is not. A method whose declared result is its type
parameter, `inline def f[T]: T`, does not have the problem, since what it
declares is what it expands to. Tuples of any shape are chapter 09's subject.

**Runaway expansion.** An `inline` method that calls itself without a base case
expands until a limit — 32 by default, raised to 128 in this build by
`CompilerFlags.maxInlines`:

```text
Maximal number of successive inlines (128) exceeded,
Maybe this is caused by a recursive inline method?
You can use -Xmax-inlines to change the limit.
```

## Exercises

| # | asks for |
| --- | --- |
| 01 | a `Port` built from a literal type and checked while compiling: `Port[8080]` is a port, `Port[70000]` does not compile |
| 02 | `default[T]`, a default value for every type that has one, tuples included — and a compile error for the rest |
| 03 | chapter 07's `add`, finally with a value: `add(1, 2.5)` is `3.5`, and a `Double` statically |
| 04 | a `describe` that uses a `Show` if there is one, a `Numeric` if not, and refuses to compile otherwise |

```bash
sbt "exercises/testOnly typeprog.ch08inlinescalacompiletime.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [Inline](https://docs.scala-lang.org/scala3/reference/metaprogramming/inline.html) —
  the reference: inline parameters, `inline if` and `inline match`,
  `transparent`
- [Compile-time operations](https://docs.scala-lang.org/scala3/reference/metaprogramming/compiletime-ops.html) —
  `constValue`, `erasedValue`, `summonInline`, `summonFrom`, `error`
- [Inline](https://docs.scala-lang.org/scala3/guides/macros/inline.html) in the
  macros guide — the same material, with more examples
