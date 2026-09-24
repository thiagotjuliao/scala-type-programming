# Chapter 03 — Path-dependent & dependent function types

> Status: **done** — `git show ch03`

## Why this exists

Two graphs, each handing out nodes, and a method that connects two of them:

```scala
final class Node(val label: String)

final class Graph:
  def node(label: String): Node = Node(label)
  def connect(from: Node, to: Node): Unit = ...

val roads  = Graph()
val rivers = Graph()
roads.connect(roads.node("Lisbon"), rivers.node("Tagus"))  // compiles
```

The last line is nonsense: an edge between two graphs. Nothing in the types
objects, because nothing in the types *can*. `roads` and `rivers` are both a
`Graph`, their nodes are both a `Node`, and a type parameter is no help — make
it `Graph[N]` and the two graphs are `Graph[Node]` again. Every tool from the
first two chapters describes values by their *type*; what is missing here is a
type that belongs to one particular *value*: "a node of `roads`".

The second problem is the same one seen from the other side. A settings store
keyed by name is easy to write and miserable to use:

```scala
def get(key: Key): Option[Any]
val port = settings.get(portKey).map(_.asInstanceOf[Int])  // every call site
```

The key knows what its value is; the signature cannot say so, because the
result type would have to depend on *which key was passed* — on the argument's
value, not on its type.

Scala answers both with one idea: a type can be selected *through a value*.
`roads.Node` is the node type of `roads` and no other graph, and a method can
return `Option[key.Value]` — a type that is only decided when a `key` is
supplied.

## The idea

### A type member selected through a value

Move `Node` inside `Graph` and every graph gets a node type of its own:

```scala
final class Graph:
  final class Node(val label: String)
  def node(label: String): Node = Node(label)
  def connect(from: Node, to: Node): Unit = ...
```

Inside the class, `Node` means `this.Node`. Outside, it has to be selected
through a graph: `roads.Node`, `rivers.Node`. Those are two different types, and
the bad call from above is now rejected:

```text
Found:    rivers.Node
Required: roads.Node
```

`roads.Node` is a **path-dependent type**: its meaning depends on the path
`roads` in front of it. Two paths are the same type only when they are the same
path — two graphs built identically are still two graphs.

Every graph's node type conforms to one shared supertype, written with `#`:
`Graph#Node` is "a `Node` of *some* graph", which is what a list mixing nodes
from both graphs has to hold. It is the type projection, and it is the escape
hatch, not the goal — a `Graph#Node` can no longer be passed to either graph's
`connect`.

### Only a stable path can carry a type

Not every expression can stand in front of `.Node`. The prefix has to be a
**stable path**: a `val`, an `object`, a method parameter, `this` — something
that is guaranteed to denote the same value every time it is evaluated. A `var`
can change under the type; a `def` can return a new graph on every call:

```scala
var current: Graph = Graph()
def fresh(): Graph = Graph()

val a: current.Node = ...  // not an immutable path
val b: fresh().Node = ...  // not even syntax: a type cannot contain a call
```

The call itself is fine — `fresh().node("x")` compiles — but the result has no
path to be typed through, so the compiler widens it to the projection,
`Graph#Node`, and it is rejected by every graph's `connect`.

### Dependent method types

A method parameter is a stable path, so a later part of the signature can
select through it:

```scala
def link(g: Graph)(a: g.Node, b: g.Node): Unit
```

`link(roads)(...)` now wants two `roads.Node`s, and the path is picked at the
call site. The result type can depend on a parameter just the same, which is
the settings store with its cast moved inside, once:

```scala
trait Key:
  type Value
  def name: String

def get(key: Key): Option[key.Value]
```

`key.Value` is an abstract type member, as in chapter 01, selected through the
argument. At the call site the compiler substitutes the actual argument for
`key` and reads its `Value` off its type. If `portKey`'s type is the refinement
`Key { type Value = Int }`, then `portKey.Value` is `Int` and `get(portKey)` is
an `Option[Int]`.

That makes the *declared* type of the key part of the API. The same key
ascribed as a plain `Key` has forgotten its refinement, and its `Value` is
abstract again — no longer `Int`, just `portKey.Value`, and the only thing
known about it is that it is the value of that key. The Pitfalls section shows
the message.

### Dependent function types

A method can be dependent; before Scala 3, a function *value* could not. Eta-
expanding `get` gave `Key => Option[Any]`, and the dependency was lost. Scala 3
has a function type for it:

```scala
val read: (k: Key) => Option[k.Value] = get
```

The parameter is named so the result can mention it. `read(portKey)` is an
`Option[Int]`, exactly like the method. It is still a `Function1` — the type is
sugar for a refinement of one, with an `apply` whose result depends on its
argument — so it can be stored, passed and returned like any other function.

A lambda whose body selects through its parameter gets the dependent type
without being asked: `(k: Key) => k.parse(raw)` is inferred as
`(k: Key) => Option[k.Value]`. The ascription is needed where the type is
*declared* rather than inferred — a parameter, a `val` with an explicit type, a
method's result.

The conformance only runs one way. A dependent function is a `Key =>
Option[Any]`; a `Key => Option[Any]` is not a dependent function, because it
promises nothing about which `Option` comes back.

### A path that escapes its scope

A type that names a local value cannot outlive the value's scope:

```scala
val n = { val g = Graph(); g.node("a") }
```

`g.Node` means nothing after the block, so the compiler widens the type until
it no longer mentions `g`: here `Graph#Node`. For an abstract member the
widening goes to its bound — a `k.Value` with nothing known about it becomes
`Any`. This is *type avoidance*, and it is silent: the code compiles, and the
precision is gone.

### Wildcards are paths in disguise

Scala 3 handles a wildcard type the same way. Given `c: Cell[?]`, the unknown
element type is modelled as a type selected through `c` — `c.T` — which is why
`c.set(c.get)` compiles: what came out of `c` goes back into `c`, the same
path. Two wildcards are two paths, and the message says so in exactly the
chapter's terms:

```scala
def mix(a: Cell[?], b: Cell[?]): Unit = a.set(b.get)
```

```text
Found:    b.T
Required: a.T
```

A type parameter and a type member are two encodings of the same thing; the
member one is simply the one that lets a value carry the type.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch03pathdependenttypes/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

**Mixing values from two instances.** The message is the whole diagnosis: two
paths, one expected, one found.

```text
Found:    g2.Node
Required: g1.Node
```

**Using a `def` or a `var` as a path.** Selecting a type through
`def portDef: Key.Aux[Int]` or through `var current: Graph` is rejected with the
same sentence; the `=>` in the first one is the compiler pointing at the `def`:

```text
(portDef : => Key.Aux[Int]) is not a valid type prefix, since it is not an immutable path
(current : Graph) is not a valid type prefix, since it is not an immutable path
```

Make it a `val`, or bind the result to one before selecting through it.

**Ascribing a key as its supertype.** `val port: Key = Key[Int]("port")(...)`
compiles and forgets that `Value` is `Int`. Nothing fails at the declaration;
the failure is at the first use, far away, and it names a type nobody wrote:

```text
Found:    Option[port.Value]
Required: Option[Int]
```

`port.Value` in a message is the sign that the path is right and the type
behind it is gone. Declare `Key.Aux[Int]`, or leave the type to inference.

**A function type that dropped the dependency.** Annotating
`val f: Key => Option[Any] = get` is legal and loses the result type for good.
Every use then fails with `Found: Option[Any]`, and it cannot be recovered by
annotating further along:

```text
Found:    (f : Key => Option[Any])
Required: (k: Key) => Option[k.Value]
```

**A graph that is built and used in one expression.** `fresh().node("a")` has
the type `Graph#Node`, which no `connect` accepts:

```text
Found:    Graph#Node
Required: g1.Node
```

Bind the graph to a `val` first; the path is what the type needs.

## Exercises

| # | asks for |
| --- | --- |
| 01 | give each `Graph` its own `Node` type, and a `link` that takes two nodes of the graph it is given |
| 02 | a settings store whose `get` and `set` are typed by the key passed to them |
| 03 | a dependent function that reads every key at its own type, and a loader that takes one and returns one |
| 04 | three declarations that each lose a key's value type — a widened `val`, a `def`, a result type — repaired |

```bash
sbt "exercises/testOnly typeprog.ch03pathdependenttypes.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [Dependent function types](https://docs.scala-lang.org/scala3/reference/new-types/dependent-function-types.html) —
  the reference page, and the
  [spec](https://docs.scala-lang.org/scala3/reference/new-types/dependent-function-types-spec.html)
  that shows the refinement of `Function1` it desugars to
- [Dependent function types](https://docs.scala-lang.org/scala3/book/types-dependent-function.html)
  in the Scala 3 book — a typed key-value store, from the other direction
- [Inner classes](https://docs.scala-lang.org/tour/inner-classes.html) in the Tour of Scala —
  the graph example, and the projection `Graph#Node`
- [Dropped: general type projection](https://docs.scala-lang.org/scala3/reference/dropped-features/type-projection.html) —
  why `T#A` is only allowed when `T` is a class, and the unsoundness that
  closed the general case
- Amin, Grütter, Odersky, Rompf, Stucki,
  [*The Essence of Dependent Object Types*](https://www.cs.purdue.edu/homes/rompf/papers/amin-wf16.pdf)
  (2016) — DOT, the calculus Scala 3's type system is built on, in which path-dependent
  types are the primitive and type parameters are encoded with them
