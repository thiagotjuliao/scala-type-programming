package typeprog.ch10mirrorgenericderivation

import typeprog.core.TypeLevelSuite

import Exercise04.*

class Exercise04Spec extends TypeLevelSuite:

  final case class Person(name: String, age: Int)
  enum Shape:
    case Circle(radius: Double)
  final class Plain(val x: Int)

  test("build takes exactly the fields, in order") {
    assertTypeChecks("""val p: Person = build[Person](("Ann", 3))""")
    assertTypeError("""build[Person]((3, "Ann"))""")
    assertTypeError("""build[Person](Tuple1("Ann"))""")
  }

  test("fields gives the tuple of the fields' types") {
    assertTypeChecks("""val t: (String, Int) = fields(Person("Ann", 3))""")
    assertTypeError("""val t: (Int, String) = fields(Person("Ann", 3))""")
  }

  test("a class that is not a case class has no fields to build from") {
    assertTypeError("build[Plain](Tuple1(1))")
    assertTypeError("fields(Plain(1))")
  }

  // Both compile against the stub, whose signatures take and return anything;
  // against `???` they fail, which is a red test.
  test("build and fields are the two directions of the same thing") {
    assertEquals(build[Person](("Ann", 3)), Person("Ann", 3))
    assertEquals[Any, Any](fields(Person("Bo", 4)), ("Bo", 4))
    assertEquals(build[Shape.Circle](Tuple1(2.0)), Shape.Circle(2.0))
  }
end Exercise04Spec
