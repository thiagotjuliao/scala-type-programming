package typeprog.ch08inlinescalacompiletime

import typeprog.core.TypeLevelSuite

import Exercise04.*

class Exercise04Spec extends TypeLevelSuite:

  // `describe` compiles against the stub for any argument, so these run; against
  // `???` they fail, which is a red test.
  test("a type with a Show is described by it") {
    assertEquals(describe(true), "yes")
  }

  test("a type with a Numeric and no Show is described as a number") {
    assertEquals(describe(2.5), "number 2.5")
    assertEquals(describe(BigDecimal("1.5")), "number 1.5")
  }

  // Int has both.
  test("a Show wins over a Numeric") {
    assertEquals(describe(42), "#42")
  }

  // The search happens where `describe` is expanded, so an instance defined
  // at the call site counts.
  test("an instance in scope at the call is found") {
    given Show[Double] = d => s"~$d"
    assertEquals(describe(2.5), "~2.5")
  }

  test("a type with neither does not compile, and says why") {
    assertTypeErrorContains("""describe("s")""", "neither a Show nor a Numeric")
    assertTypeErrorContains("describe(List(1))", "neither a Show nor a Numeric")
  }
end Exercise04Spec
