package typeprog.ch20validation

import typeprog.core.TypeLevelSuite

import Exercise02.*
import Exercise02.Validated.{Invalid, Valid}

class Exercise02Spec extends TypeLevelSuite:

  private def valid[A](a: A): Validated[List[String], A] = Valid(a)
  private def invalid[A](errors: String*): Validated[List[String], A] = Invalid(errors.toList)

  test("lists and integers have a Semigroup, found with no import") {
    assertTypeChecks("summon[Semigroup[List[String]]]")
    assertTypeChecks("summon[Semigroup[List[Int]]]")
    assertTypeChecks("summon[Semigroup[Int]]")
  }

  test("map2 does not compile for an error type that cannot be combined") {
    assertTypeError("map2(Invalid(true), Invalid(false))((a: Int, b: Int) => a + b)")
  }

  // A guard: the rejection above is about the error type, not about map2.
  test("map2 compiles for one that can") {
    assertTypeChecks("map2(Invalid(List(\"a\")), Invalid(List(\"b\")))((a: Int, b: Int) => a + b)")
    assertTypeChecks("map2(Invalid(1), Invalid(2))((a: Int, b: Int) => a + b)")
  }

  test("two valid results are combined by the function") {
    assertEquals(map2(valid(20), valid("x"))((n, s) => s * n), valid("x" * 20))
  }

  test("one invalid result is the result") {
    assertEquals(map2(valid(1), invalid[Int]("b"))(_ + _), invalid("b"))
    assertEquals(map2(invalid[Int]("a"), valid(2))(_ + _), invalid("a"))
  }

  test("two invalid results keep the errors of both, the left one's first") {
    assertEquals(map2(invalid[Int]("a", "b"), invalid[Int]("c"))(_ + _), invalid("a", "b", "c"))
  }

  test("the error type decides how errors combine: integers add") {
    val one: Validated[Int, String] = Invalid(1)
    val two: Validated[Int, String] = Invalid(2)
    assertEquals(map2(one, two)(_ + _), Invalid(3))
  }

  test("two invalid results with no errors in them combine to none") {
    assertEquals(map2(invalid[Int](), invalid[Int]())(_ + _), invalid())
  }
end Exercise02Spec
