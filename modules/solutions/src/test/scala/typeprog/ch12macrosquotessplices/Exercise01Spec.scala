package typeprog.ch12macrosquotessplices

import typeprog.core.TypeLevelSuite

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  val x = 20
  val name = "ann"

  // The stub's implementation returns `'{ ??? }`: the calls compile, and throw
  // when they run — a red test, not a module that fails to compile.
  test("the source text of the expression, then its value") {
    assertEquals(debug(x * 2 + 2), "x * 2 + 2 = 42")
  }

  // `show` would print the compiler's rendering, `x.*(2).+(1)`; the source
  // text is what the caller wrote.
  test("the text is the source as written, not the compiler's rendering") {
    assertEquals(debug(x * 2 + 1), "x * 2 + 1 = 41")
    assertEquals(debug(name.length * 2), "name.length * 2 = 6")
  }

  test("any type of value") {
    assertEquals(debug(name.length), "name.length = 3")
    assertEquals(debug(name.toUpperCase), "name.toUpperCase = ANN")
  }
end Exercise01Spec
