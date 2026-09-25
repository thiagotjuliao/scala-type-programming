package typeprog.ch09tuplesashlists

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  test("zero plus a number is that number") {
    assertTypeChecks("evidence.sameType[Plus[_0, _3], _3]")
  }

  test("two plus three is five") {
    assertTypeChecks("evidence.sameType[Plus[_2, _3], _5]")
    assertTypeError("evidence.sameType[Plus[_2, _3], _6]")
  }

  test("a number plus zero is that number") {
    assertTypeChecks("evidence.sameType[Plus[_3, _0], _3]")
  }

  test("two times three is six") {
    assertTypeChecks("evidence.sameType[Times[_2, _3], _6]")
    assertTypeError("evidence.sameType[Times[_2, _3], _5]")
  }

  test("zero times anything, and anything times zero, is zero") {
    assertTypeChecks("evidence.sameType[Times[_0, _3], _0]")
    assertTypeChecks("evidence.sameType[Times[_3, _0], _0]")
  }

  test("one is the unit of multiplication") {
    assertTypeChecks("evidence.sameType[Times[_1, _4], _4]")
    assertTypeChecks("evidence.sameType[Times[_4, _1], _4]")
  }

  test("ToInt reads a Peano number as a literal type") {
    assertTypeChecks("evidence.sameType[ToInt[_0], 0]")
    assertTypeChecks("evidence.sameType[ToInt[Times[_2, _3]], 6]")
    assertTypeError("evidence.sameType[ToInt[_3], 4]")
  }
end Exercise01Spec
