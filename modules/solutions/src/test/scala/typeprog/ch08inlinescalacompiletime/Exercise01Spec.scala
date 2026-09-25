package typeprog.ch08inlinescalacompiletime

import typeprog.core.TypeLevelSuite

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  // `Port[…]` compiles against the stub — it takes any `N <: Int` — so these
  // run, and against `???` they fail, which is a red test.
  test("a literal in range is a port with that number") {
    assertEquals(Port[8080].number, 8080)
    assertEquals(Port[1].number, 1)
    assertEquals(Port[65535].number, 65535)
  }

  test("a literal out of range does not compile, and says why") {
    assertTypeErrorContains("Port[70000]", "a port is between 1 and 65535")
    assertTypeErrorContains("Port[0]", "a port is between 1 and 65535")
    assertTypeErrorContains("Port[-1]", "a port is between 1 and 65535")
    assertTypeError("Port[65536]")
  }

  // The number has to be known while compiling: a type that is not a literal
  // has no number to check.
  test("a type that is not a literal is rejected") {
    assertTypeError("Port[Int]")
    assertTypeError("def open[N <: Int]: Port = Port[N]")
  }

  // A guard: the port is still chapter 05's opaque type.
  test("a Port is not an Int outside its definition") {
    assertTypeError("val i: Int = Port[8080]")
  }
end Exercise01Spec
