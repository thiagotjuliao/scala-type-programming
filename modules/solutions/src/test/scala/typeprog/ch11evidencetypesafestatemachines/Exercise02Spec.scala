package typeprog.ch11evidencetypesafestatemachines

import typeprog.core.TypeLevelSuite

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  final class Widget

  // Guards: Int has both a Show and a Numeric, which is the whole exercise,
  // and neither may be removed to make the choice go away.
  test("Int is both showable and numeric") {
    assertTypeChecks("summon[Show[Int]]")
    assertTypeChecks("summon[Numeric[Int]]")
  }

  test("a number is a bare number, whether or not it also has a Show") {
    assertEquals(encode(42), "42")
    assertEquals(encode(-7), "-7")
    assertEquals(encode(2.5), "2.5")
    assertEquals(encode(BigDecimal("1.5")), "1.5")
  }

  // A guard: the stub already quotes these. It catches a solution that fixes
  // Int by making everything a number.
  test("a value that is only showable is a quoted string") {
    assertEquals(encode("hi"), "\"hi\"")
    assertEquals(encode(true), "\"true\"")
  }

  // Guards as well: the stub already gets these right.
  test("every type has at most one rule, and some have none") {
    assertTypeChecks("summon[Json[Int]]")
    assertTypeChecks("summon[Json[String]]")
    assertTypeError("encode(Widget())")
  }
end Exercise02Spec
