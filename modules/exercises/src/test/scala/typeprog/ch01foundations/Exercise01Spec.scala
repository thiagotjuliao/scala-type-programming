package typeprog.ch01foundations

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  test("a source of dogs is a source of animals") {
    assertTypeChecks("evidence.subtypeOf[Source[Dog], Source[Animal]]")
  }

  test("a sink of animals is a sink of dogs") {
    assertTypeChecks("evidence.subtypeOf[Sink[Animal], Sink[Dog]]")
  }

  test("neither relation runs the other way") {
    assertTypeError("evidence.subtypeOf[Source[Animal], Source[Dog]]")
    assertTypeError("evidence.subtypeOf[Sink[Dog], Sink[Animal]]")
  }

  test("a channel is invariant in both directions") {
    assertTypeError("evidence.subtypeOf[Channel[Dog], Channel[Animal]]")
    assertTypeError("evidence.subtypeOf[Channel[Animal], Channel[Dog]]")
  }

  test("a covariant source can be consumed where a wider one is expected") {
    assertTypeChecks("""val s: Source[Animal] = new Source[Dog] { def emit() = Dog("Rex") }""")
  }
