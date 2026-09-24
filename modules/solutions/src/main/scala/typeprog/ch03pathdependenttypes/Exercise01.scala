package typeprog.ch03pathdependenttypes

/** Exercise 01 — give each graph a node type of its own. *Solved.*
  *
  * Moving `Node` inside `Graph` is the whole first half. A class nested in a
  * class is a member of each *instance*, so `g1.Node` and `g2.Node` are two
  * types, and inside `Graph` every mention of `Node` — `edges`, `node`,
  * `connect` — silently means `this.Node`. That is why none of the bodies had
  * to change: `connect` already said "a node of this graph"; it just had no way
  * to mean it while `Node` lived outside. `Graph#Node` comes for free, as the
  * type every instance's `Node` conforms to.
  *
  * `link` needs its nodes typed through `g`, and `g` is a parameter — a stable
  * path — so a later parameter list can select through it: `a: g.Node`. At the
  * call `link(roads)(...)`, `g` is replaced by `roads` and the arguments are
  * checked against `roads.Node`. A node made by `Graph().node(...)` fails for a
  * different reason than one from a named graph: that graph has no path at all,
  * so its node's type was widened to `Graph#Node`, which is no graph's `Node`.
  *
  * The near miss is `link(g: Graph)(a: Graph#Node, b: Graph#Node)`. The
  * signature compiles, and then the body does not: `g.connect(a, b)` wants a
  * `g.Node`, and a `Graph#Node` could belong to any graph —
  * `Found: Graph#Node, Required: g.Node`. The projection is the type to
  * *collect* nodes under, never the one to *use* them at.
  *
  * Scala 3 would also accept `link(g: Graph, a: g.Node, b: g.Node)`, all in one
  * parameter list. The curried form is kept because it reads as what happens:
  * pick the graph, then give it nodes.
  */
object Exercise01:

  final class Graph:
    final class Node(val label: String)

    private var edges = Set.empty[(Node, Node)]

    def node(label: String): Node = Node(label)
    def connect(from: Node, to: Node): Unit = edges += from -> to
    def connected(from: Node, to: Node): Boolean = edges(from -> to)

  def link(g: Graph)(a: g.Node, b: g.Node): Unit =
    g.connect(a, b)
    g.connect(b, a)
