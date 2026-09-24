package typeprog.ch03pathdependenttypes

import typeprog.core.TypeLevelSuite

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  test("a node belongs to the graph that made it") {
    assertTypeChecks("val g = Graph(); val a: g.Node = g.node(\"a\")")
  }

  // A guard rather than a claim: it passes against the stub as well. What it
  // catches is a solution that rejects too much.
  test("nodes of the same graph connect") {
    assertTypeChecks("val g = Graph(); g.connect(g.node(\"a\"), g.node(\"b\"))")
  }

  test("nodes of two graphs do not connect") {
    assertTypeError(
      "val g1 = Graph(); val g2 = Graph(); g1.connect(g1.node(\"a\"), g2.node(\"b\"))"
    )
  }

  test("every graph's node is a Graph#Node") {
    assertTypeChecks(
      "val g1 = Graph(); val g2 = Graph(); val ns: List[Graph#Node] = List(g1.node(\"a\"), g2.node(\"b\"))"
    )
  }

  test("link takes two nodes of the graph it is given") {
    assertTypeChecks("val g = Graph(); link(g)(g.node(\"a\"), g.node(\"b\"))")
  }

  test("link rejects a node of another graph, in either position") {
    assertTypeError("val g1 = Graph(); val g2 = Graph(); link(g1)(g1.node(\"a\"), g2.node(\"b\"))")
    assertTypeError("val g1 = Graph(); val g2 = Graph(); link(g1)(g2.node(\"a\"), g1.node(\"b\"))")
  }

  test("link rejects a node whose graph has no path") {
    assertTypeError("val g = Graph(); link(g)(g.node(\"a\"), Graph().node(\"b\"))")
  }
end Exercise01Spec
