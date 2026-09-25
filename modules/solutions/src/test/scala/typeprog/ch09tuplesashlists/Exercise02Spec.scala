package typeprog.ch09tuplesashlists

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  test("the first element is at index 0") {
    assertTypeChecks("evidence.sameType[IndexOf[(Int, String, Boolean), Int], 0]")
  }

  test("a later element is at its position, counted from 0") {
    assertTypeChecks("evidence.sameType[IndexOf[(Int, String, Boolean), String], 1]")
    assertTypeChecks("evidence.sameType[IndexOf[(Int, String, Boolean), Boolean], 2]")
    assertTypeError("evidence.sameType[IndexOf[(Int, String, Boolean), Boolean], 3]")
  }

  test("an element that occurs twice is found at its first position") {
    assertTypeChecks("evidence.sameType[IndexOf[(Char, Int, String, Int), Int], 1]")
  }

  test("an element that is not there gives -1") {
    assertTypeChecks("evidence.sameType[IndexOf[(Int, String, Boolean), Char], -1]")
    assertTypeChecks("evidence.sameType[IndexOf[EmptyTuple, Char], -1]")
    assertTypeError("evidence.sameType[IndexOf[(Int, String, Boolean), Char], 3]")
  }

  // The index is a literal type, so it can be read as a value while compiling.
  test("the index can be read back as a value") {
    assertTypeChecks(
      "val i: 2 = scala.compiletime.constValue[IndexOf[(Int, String, Boolean), Boolean]]"
    )
  }
end Exercise02Spec
