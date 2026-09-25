package typeprog.ch10mirrorgenericderivation

import typeprog.core.TypeLevelSuite

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  final case class Person(name: String, age: Int) derives Show
  final case class Empty() derives Show
  final case class Address(city: String)
  final case class Customer(id: Int, person: Person, address: Address, vip: Boolean) derives Show
  final class Plain(val x: Int)
  final case class HasPlain(p: Plain)

  // The stub's `derived` has the right signature and no body, so `derives`
  // compiles against it, and using an instance fails: a red test.
  test("a case class is shown as its name and its fields") {
    assertEquals(summon[Show[Person]].show(Person("Ann", 3)), """Person(name = "Ann", age = 3)""")
  }

  test("a case class with no fields is shown with empty parentheses") {
    assertEquals(summon[Show[Empty]].show(Empty()), "Empty()")
  }

  // `Person` derives its own instance; `Address` does not, and is derived on
  // the way.
  test("fields that are case classes are shown the same way") {
    assertEquals(
      summon[Show[Customer]].show(Customer(7, Person("Ann", 3), Address("Oslo"), true)),
      """Customer(id = 7, person = Person(name = "Ann", age = 3), address = Address(city = "Oslo"), vip = true)"""
    )
  }

  test("a field with no Show and no mirror makes the class underivable") {
    assertTypeError("Show.derived[HasPlain]")
  }
end Exercise02Spec
