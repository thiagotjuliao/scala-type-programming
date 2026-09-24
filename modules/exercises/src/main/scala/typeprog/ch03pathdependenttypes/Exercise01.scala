package typeprog.ch03pathdependenttypes

import typeprog.core.Unsolved

/** Exercise 01 — give each graph a node type of its own.
  *
  * As shipped, every `Graph` hands out the same `Node`, so nothing stops a node
  * of one graph from being connected inside another. Change the declarations
  * so that:
  *
  *   - a node made by `g.node(...)` has the type `g.Node`, and a node of a
  *     different graph is rejected by `g.connect`;
  *   - `Graph#Node` is still a type every graph's nodes conform to;
  *   - `link(g)(a, b)` connects `a` and `b` in both directions, and takes only
  *     nodes of the graph `g` it is given — a node of another graph, or of a
  *     graph that was never bound to a name, is rejected in either position.
  *
  * The bodies of `Graph`'s methods are already right. What is wrong is where
  * `Node` is declared, and what `link` says about its parameters.
  *
  * Hint: a class declared inside another class is a different type for every
  * instance of the outer one. A later parameter list can select a type through
  * an earlier parameter.
  */
object Exercise01:

  /** TODO: every graph should have its own. */
  final class Node(val label: String)

  final class Graph:
    private var edges = Set.empty[(Node, Node)]

    def node(label: String): Node = Node(label)
    def connect(from: Node, to: Node): Unit = edges += from -> to
    def connected(from: Node, to: Node): Boolean = edges(from -> to)

  /** TODO: the parameter types, and the body. */
  def link(g: Graph)(a: Unsolved, b: Unsolved): Unit = ???
