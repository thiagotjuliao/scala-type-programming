package typeprog.ch07matchtypes

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  test("an Option unwraps to its element") {
    assertTypeChecks("evidence.sameType[Unwrap[Option[Int]], Int]")
    assertTypeError("evidence.sameType[Unwrap[Option[Int]], Option[Int]]")
  }

  // The pattern is a supertype test, not an equality: `Some[String]` is an
  // `Option[a]` with `a = String`.
  test("a subtype of Option unwraps the same way") {
    assertTypeChecks("evidence.sameType[Unwrap[Some[String]], String]")
  }

  test("an Either unwraps to its right side") {
    assertTypeChecks("evidence.sameType[Unwrap[Either[String, Int]], Int]")
    assertTypeError("evidence.sameType[Unwrap[Either[String, Int]], String]")
    assertTypeChecks("evidence.sameType[Unwrap[Right[String, Boolean]], Boolean]")
  }

  test("a List unwraps to its element") {
    assertTypeChecks("evidence.sameType[Unwrap[List[Char]], Char]")
  }

  test("only one layer comes off") {
    assertTypeChecks("evidence.sameType[Unwrap[Option[List[Int]]], List[Int]]")
    assertTypeError("evidence.sameType[Unwrap[Option[List[Int]]], Int]")
  }

  test("anything else is left alone") {
    assertTypeChecks("evidence.sameType[Unwrap[Int], Int]")
    assertTypeChecks("evidence.sameType[Unwrap[String], String]")
    assertTypeChecks("evidence.sameType[Unwrap[Vector[Int]], Vector[Int]]")
    assertTypeError("evidence.sameType[Unwrap[Vector[Int]], Int]")
  }
end Exercise01Spec
