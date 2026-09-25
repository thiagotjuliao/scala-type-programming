package typeprog.ch10mirrorgenericderivation

import typeprog.core.TypeLevelSuite

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  final case class Person(name: String, age: Int)
  final case class Nothing0()
  enum Shape:
    case Circle(radius: Double)
    case Rect(width: Double, height: Double)
  final class Plain(val x: Int)

  // `typeName` and `fieldNames` compile against the stub for any type, so
  // these run; against `???` they fail, which is a red test.
  test("a case class's name and field names, in order") {
    assertEquals(typeName[Person], "Person")
    assertEquals(fieldNames[Person], List("name", "age"))
  }

  test("a case class with no fields has no field names") {
    assertEquals(fieldNames[Nothing0], Nil)
  }

  test("an enum case is a case class too") {
    assertEquals(typeName[Shape.Rect], "Rect")
    assertEquals(fieldNames[Shape.Rect], List("width", "height"))
  }

  test("an enum has a name, and no fields") {
    assertEquals(typeName[Shape], "Shape")
  }

  test("a class the compiler cannot see into is rejected") {
    assertTypeError("fieldNames[Plain]")
    assertTypeError("typeName[Plain]")
  }
end Exercise01Spec
