# Chapter 05 — Opaque types & phantom newtypes

> Status: **done** — `git show ch05`

## Why this exists

A method that moves money between accounts, written the obvious way:

```scala
def transfer(from: Long, to: Long, cents: Long): Unit
transfer(amount, fromId, toId)   // compiles
```

Three `Long`s, three meanings, and the call site gets the order wrong without a
murmur. The textbook repair is a wrapper per meaning —
`final case class AccountId(value: Long)` — and it works, at a price: every id
is now an object on the heap, a `List[AccountId]` is a list of pointers to
boxes, and every `AccountId` knows it is really a `Long` the moment anybody
calls `.value` or pattern-matches on it. A type alias, `type AccountId = Long`,
costs nothing and protects nothing: an alias *is* the type it names.

What is wanted is a type that is a `Long` where the id is defined — so it can
be built and read without ceremony — and a different type everywhere else, so
it cannot be confused with the other two. Scala 3 has exactly that, and the
runtime never sees it: an **opaque type**.

The second half of the chapter goes one step further, to types that no value
ever has. A `Connection[Open]` and a `Connection[Closed]` are the same object at
runtime; the type parameter exists only so the compiler can refuse to `send`
on a connection that was never opened. That is a **phantom type**, and it is
how a rule about the *order* of calls becomes a rule the compiler checks.

## The idea

### An opaque type is an alias with a boundary

```scala
object Accounts:
  opaque type AccountId = Long

  object AccountId:
    def apply(raw: Long): AccountId = raw

  extension (id: AccountId) def raw: Long = id
```

Inside `Accounts` — the scope that defines it — `AccountId` *is* `Long`, and
`apply` and `raw` are just the identity. Outside, it is an abstract type the
compiler knows nothing about: not a `Long`, not a subtype of one, with none of
`Long`'s methods:

```text
Found:    (42L : Long)
Required: Accounts.AccountId
```

The boundary is the only thing that exists. There is no wrapper class: at
runtime an `AccountId` is a `long`, an `Array[AccountId]` is a `long[]`, and
nothing is allocated to create one. That is the difference from the case class,
and the reason opaque types exist.

What an opaque type can do outside its scope is exactly what its scope chose to
publish: a companion to build values — often returning `Option` or `Either`,
so an invalid one cannot be built at all — and extension methods to use them.

### A bound decides what leaks

An upper bound makes part of the representation public:

```scala
opaque type Port <: Int = Int
```

Outside, a `Port` is now an `Int` — it can be passed where an `Int` is
expected, and `port + 1` is an `Int` — but an `Int` is still not a `Port`. The
relation goes one way, which is usually the one wanted: every port is a number,
not every number is a port.

### Opaque types and `Matchable`

At runtime the boundary is gone, so a `match` could look straight through it:
an `Email` *is* a `String` to `isInstanceOf`. That is why an opaque type does
not extend `Matchable` (chapter 04). Matching on one directly is flagged under
`-source:future`:

```text
pattern selector should be an instance of Matchable,
but it has unmatchable type Email instead
```

And comparing one to its representation with `==` is rejected outright — not
by the type checker proper, but by the multiversal equality check that runs
after it:

```text
Values of types Email and String cannot be compared with == or !=
```

Both can be defeated with an upcast to `Any`. Neither should be.

### A phantom type parameter carries a fact

A type parameter that no field and no method ever uses at runtime can still be
checked at compile time:

```scala
sealed trait State
sealed trait Open extends State
sealed trait Closed extends State

final class Connection[S <: State] private (...)
```

No value of type `Open` or `Closed` will ever exist — the traits are sealed and
never extended — so they are pure labels. `Connection[Closed]` and
`Connection[Open]` erase to the same class; the parameter is a fact the
compiler tracks and the JVM never hears about.

Operations are then made available only for the right label. The direct way is
an extension method on the specific type:

```scala
extension (c: Connection[Closed]) def open: Connection[Open]
extension (c: Connection[Open])   def send(msg: String): Connection[Open]
```

`Connection.closed(host).send("hi")` is rejected, and the message names the
state the call was made in:

```text
value send is not a member of Connection[Closed]
```

With the extensions in the companion object, as here, that is the whole
message. When they are imported into scope instead, the compiler also reports
the attempt, in the chapter's terms:

```text
value send is not a member of Connection[Closed].
An extension method was tried, but could not be fully constructed:
    ...
    failed with:
        Found:    Connection[Closed]
        Required: Connection[Open]
```

The other way is a member that demands evidence, `def send(msg: String)(using
S =:= Open)`, from chapter 01. It keeps the method on the class, and its
message is shorter: *"Cannot prove that Closed =:= Open."*

### Both at once

An opaque type can have type parameters, and they can be phantom:

```scala
opaque type Quantity[U] = Double
```

At runtime every quantity is a `Double`; at compile time a `Quantity[Meters]`
and a `Quantity[Seconds]` are different types, and an extension that adds two
`Quantity[U]`s with the same `U` refuses to add metres to seconds. Units can be
combined with a phantom type constructor — `Quantity[Meters / Seconds]` — which
is never instantiated either. Zero runtime cost, and the dimension checked on
every line.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch05opaquetypes/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

**Expecting the representation's methods outside the scope.** `email.length`
does not compile on an opaque `Email`, however obviously it is a `String`:

```text
value length is not a member of Emails.Email
```

That is the feature. Publish what callers need as an extension; the scope sees
through the alias, so the extension's body can use `String`'s methods freely.

**Calling an extension on the raw type.** An extension declared for the opaque
type does not apply to its representation, and the message shows the attempt:

```text
value domain is not a member of String.
An extension method was tried, but could not be fully constructed:
    ...
        Found:    ("a@b" : String)
        Required: Emails.Email
```

**A transparent alias where an opaque one was meant.** `type Email = String`
compiles everything above — including the calls that should have been rejected.
Nothing fails; nothing is protected. The only visible difference is the
keyword.

**Comparing an opaque value with its representation.** `email == "a@b"` is
rejected (see above), and `compileErrors`-based checks such as this
repository's `assertTypeError` do not see that rejection: it is reported after
type-checking, which is where they stop. It shows up only when the code is
compiled for real.

**Pattern matching on a phantom type.** The label is erased, so the test cannot
be made:

```text
the type test for Connection[Open] cannot be checked at runtime because its
type arguments can't be determined from Any
```

The match would accept a `Connection[Closed]` as well. A phantom type is a
compile-time fact; branch on it with overloads or extensions, never with a
runtime test.

## Exercises

| # | asks for |
| --- | --- |
| 01 | an `Email` that is a `String` inside its scope and not outside, built only through a validating constructor |
| 02 | a `Port` that can be used as an `Int` but not made from one |
| 03 | a `Connection` whose `open`, `send` and `close` are only available in the state they make sense in |
| 04 | quantities with units: added only to their own unit, divided into a new one — all as plain `Double`s at runtime |

```bash
sbt "exercises/testOnly typeprog.ch05opaquetypes.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [Opaque type aliases](https://docs.scala-lang.org/scala3/reference/other-new-features/opaques.html) —
  the reference page, and its
  [more details](https://docs.scala-lang.org/scala3/reference/other-new-features/opaques-details.html)
  on scope, bounds and erasure
- [Opaque types](https://docs.scala-lang.org/scala3/book/types-opaque-types.html) in the Scala 3 book —
  the logarithm example, which shows the runtime cost they avoid
- [Multiversal equality](https://docs.scala-lang.org/scala3/reference/contextual/multiversal-equality.html) —
  where the rejection of `email == "a@b"` comes from
- [SIP-35: opaque types](https://docs.scala-lang.org/sips/opaque-types.html) —
  the design discussion, including why value classes were not enough
