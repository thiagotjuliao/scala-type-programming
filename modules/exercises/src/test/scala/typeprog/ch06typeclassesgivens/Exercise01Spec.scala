package typeprog.ch06typeclassesgivens

import typeprog.core.TypeLevelSuite

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  final class Widget

  // A guard: the stub ships these two, and a solution must not lose them.
  test("Int and String have a Show") {
    assertTypeChecks("summon[Show[Int]]")
    assertTypeChecks("summon[Show[String]]")
  }

  test("a list of showable things is showable, with no import") {
    assertTypeChecks("summon[Show[List[Int]]]")
  }

  test("an option of a showable thing is showable, with no import") {
    assertTypeChecks("summon[Show[Option[String]]]")
  }

  test("the rules compose: a list of options of lists") {
    assertTypeChecks("summon[Show[List[Option[List[Int]]]]]")
  }

  // Also a guard on its own — the stub has no list instance at all. The claim
  // that tells a conditional instance from an unconditional one is the next.
  test("a list of things with no Show has no Show") {
    assertTypeError("summon[Show[List[Widget]]]")
    assertTypeError("summon[Show[Option[Widget]]]")
  }

  test("describe accepts only what can be shown") {
    assertTypeError("describe(Widget())")
    assertTypeError("describe(List(Widget()))")
  }

  test("describe uses the instance") {
    assertEquals(describe(42), "42")
    assertEquals(describe("hi"), "\"hi\"")
  }

  test("lists and options render through their elements' instances") {
    assertEquals(describe(List(1, 2, 3)), "[1, 2, 3]")
    assertEquals(describe(List.empty[Int]), "[]")
    assertEquals(describe(Option("a")), "Some(\"a\")")
    assertEquals(describe(Option.empty[Int]), "None")
    assertEquals(describe(List(Option(1), None)), "[Some(1), None]")
  }
end Exercise01Spec
