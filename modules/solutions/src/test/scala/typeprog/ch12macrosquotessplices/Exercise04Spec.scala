package typeprog.ch12macrosquotessplices

import typeprog.core.TypeLevelSuite

import Exercise04.*

class Exercise04Spec extends TypeLevelSuite:

  final case class Person(name: String, age: Int)
  final case class Box(items: List[Int], label: Option[String])
  final case class Empty()
  final class Plain(val x: Int)

  test("a case class, its fields and their types, as short names") {
    assertEquals(describe[Person], "Person(name: String, age: Int)")
  }

  test("field types with type arguments") {
    assertEquals(describe[Box], "Box(items: List[Int], label: Option[String])")
  }

  test("a case class with no fields") {
    assertEquals(describe[Empty], "Empty()")
  }

  test("a type that is not a case class does not compile, and says so") {
    assertTypeErrorContains("describe[Plain]", "not a case class")
    assertTypeErrorContains("describe[Int]", "not a case class")
  }
end Exercise04Spec
