package typeprog.ch10mirrorgenericderivation

import typeprog.core.TypeLevelSuite

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  enum Light derives Values:
    case Red, Amber, Green

  sealed trait Coin derives Values
  case object Heads extends Coin
  case object Tails extends Coin

  enum Shape:
    case Circle(radius: Double)
    case Point

  final case class Person(name: String)

  // `derives Values` compiles against the stub, whose `derived` has no body;
  // using an instance fails, which is a red test.
  test("every case of an enum, in the order declared") {
    assertEquals(summon[Values[Light]].all, List(Light.Red, Light.Amber, Light.Green))
  }

  test("every case object of a sealed trait") {
    assertEquals(summon[Values[Coin]].all, List(Heads, Tails))
  }

  test("a case with parameters has no single value, and says so") {
    assertTypeErrorContains("Values.derived[Shape]", "singleton")
  }

  // A guard: the stub asks for a sum's mirror too.
  test("a type that is not a sum is rejected") {
    assertTypeError("Values.derived[Person]")
  }
end Exercise03Spec
