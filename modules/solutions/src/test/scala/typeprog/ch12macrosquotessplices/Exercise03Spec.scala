package typeprog.ch12macrosquotessplices

import typeprog.core.TypeLevelSuite

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  final case class Person(name: String, age: Int)

  val person: Person = Person("Ann", 3)

  test("the name of a selected field") {
    assertEquals(nameOf(person.age), "age")
    assertEquals(nameOf(person.name), "name")
  }

  test("the name of a variable") {
    assertEquals(nameOf(person), "person")
  }

  test("the last name in a chain of selections") {
    assertEquals(nameOf(person.name.length), "length")
  }

  test("anything that is not a name does not compile, and says so") {
    assertTypeErrorContains("nameOf(1 + 2)", "a name")
    assertTypeErrorContains("""nameOf("age")""", "a name")
  }
end Exercise03Spec
