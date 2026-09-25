package typeprog.ch07matchtypes

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  test("a type that is not a container is its own leaf") {
    assertTypeChecks("evidence.sameType[Leaf[Int], Int]")
    assertTypeChecks("evidence.sameType[Leaf[String], String]")
  }

  test("one layer comes off, as with Unwrap") {
    assertTypeChecks("evidence.sameType[Leaf[Option[String]], String]")
    assertTypeChecks("evidence.sameType[Leaf[Either[Boolean, Char]], Char]")
  }

  // What tells Leaf from a one-layer Unwrap: the result of taking a layer off
  // is itself unwrapped, until nothing is left to take off.
  test("every layer comes off") {
    assertTypeChecks("evidence.sameType[Leaf[List[List[Int]]], Int]")
    assertTypeError("evidence.sameType[Leaf[List[List[Int]]], List[Int]]")
    assertTypeChecks("evidence.sameType[Leaf[List[Option[Either[String, Int]]]], Int]")
    assertTypeChecks("evidence.sameType[Leaf[Either[Boolean, List[Char]]], Char]")
  }

  test("the unwrapping stops at a type that is not an Option, Either or List") {
    assertTypeChecks("evidence.sameType[Leaf[Vector[Int]], Vector[Int]]")
    assertTypeChecks("evidence.sameType[Leaf[List[Vector[Int]]], Vector[Int]]")
    assertTypeError("evidence.sameType[Leaf[List[Vector[Int]]], Int]")
  }
end Exercise02Spec
