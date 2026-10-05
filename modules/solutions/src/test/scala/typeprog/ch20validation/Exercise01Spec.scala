package typeprog.ch20validation

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise01.*
import Exercise01.Validated.{Invalid, Valid}

class Exercise01Spec extends TypeLevelSuite:

  // Each value goes through a `val` with no ascription first. Written as
  // `val v: Validated[List[String], Int] = Valid(42)`, the expected type would
  // reach `Valid.apply` and infer `E = List[String]` up front, and an
  // invariant `Validated` would pass.
  test("a valid value, with no errors to its name, fits any error type") {
    assertTypeChecks("""
      val valid = Valid(42)
      val v: Validated[List[String], Int] = valid
    """)
    assertTypeChecks("evidence.subtypeOf[Validated[Nothing, Int], Validated[List[String], Int]]")
  }

  test("an invalid one, with no value, fits any value type") {
    assertTypeChecks("""
      val invalid = Invalid(List("no"))
      val v: Validated[List[String], Int] = invalid
    """)
    assertTypeChecks("evidence.subtypeOf[Validated[String, Nothing], Validated[String, Int]]")
  }

  test("results of different validations widen to a common type") {
    assertTypeChecks(
      "evidence.subtypeOf[Validated[List[String], Int], Validated[Seq[String], AnyVal]]"
    )
  }

  // Guards: covariance only goes one way. They catch a parameter made
  // contravariant, or a Validated that is an alias for Any.
  test("an error type does not narrow, nor a value type") {
    assertTypeError(
      "evidence.subtypeOf[Validated[Seq[String], Int], Validated[List[String], Int]]"
    )
    assertTypeError("evidence.subtypeOf[Validated[String, AnyVal], Validated[String, Int]]")
    assertTypeError("evidence.subtypeOf[Validated[String, Int], Validated[String, String]]")
  }

  test("map transforms a valid value") {
    assertEquals(Valid(20).map(_ + 1), Valid(21))
  }

  test("map keeps the errors, and never calls the function") {
    val invalid: Validated[List[String], Int] = Invalid(List("no"))
    assertEquals(invalid.map[Int](_ => fail("map called the function on an Invalid")), invalid)
  }

  test("fold takes the branch that matches") {
    val valid: Validated[List[String], Int] = Valid(2)
    val invalid: Validated[List[String], Int] = Invalid(List("a", "b"))
    assertEquals(valid.fold(_.size, _ * 10), 20)
    assertEquals(invalid.fold(_.size, _ * 10), 2)
  }
end Exercise01Spec
