package typeprog.ch06typeclassesgivens

import typeprog.core.TypeLevelSuite

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  final class Widget

  // Guards: Int has both a Show and a Numeric, which is the whole exercise, so
  // neither may be removed to make the choice go away.
  test("Int is both showable and numeric") {
    assertTypeChecks("summon[Show[Int]]")
    assertTypeChecks("summon[Numeric[Int]]")
  }

  // Also guards — the stub is not ambiguous, it is wrong. A solution that
  // puts both rules in one object fails these.
  test("encode is available, unambiguously, for each kind of value") {
    assertTypeChecks("summon[Json[Int]]")
    assertTypeChecks("summon[Json[String]]")
    assertTypeChecks("summon[Json[BigDecimal]]")
  }

  // Guards as well: where only one rule applies, the stub already picks it.
  // They catch a solution that fixes Int by breaking every other type.
  test("a type that is neither showable nor numeric cannot be encoded") {
    assertTypeError("encode(Widget())")
    assertTypeError("summon[Json[Widget]]")
  }

  test("a number that is only numeric is a bare number") {
    assertEquals(encode(BigDecimal("1.5")), "1.5")
    assertEquals(encode(2.5), "2.5")
  }

  test("a value that is only showable is a quoted string") {
    assertEquals(encode("hi"), "\"hi\"")
    assertEquals(encode(true), "\"true\"")
  }

  test("an Int, which is both, is a number") {
    assertEquals(encode(42), "42")
    assertEquals(encode(-7), "-7")
  }
end Exercise03Spec
