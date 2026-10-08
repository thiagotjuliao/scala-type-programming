package typeprog.ch24derivedjsoncodec

import typeprog.core.TypeLevelSuite

import Exercise01.*
import Exercise01.Json.*

object Exercise01Fixtures:

  final case class Address(city: String)

  final case class User(
      name: String,
      age: Int,
      address: Address,
      nick: Option[String],
      tags: List[String]
  ) derives Encoder

  enum Shape derives Encoder:
    case Circle(radius: Int)
    case Dot

  enum Tree derives Encoder:
    case Leaf(value: Int)
    case Node(left: Tree, right: Tree)

import Exercise01Fixtures.*

class Exercise01Spec extends TypeLevelSuite:

  // A guard: `derives` compiles against the stub too. It catches a `derived`
  // whose signature no longer fits the clause.
  test("derives Encoder gives an instance") {
    assertTypeChecks("summon[Encoder[User]]")
    assertTypeChecks("summon[Encoder[Shape]]")
  }

  test("a case class is an object keyed by its labels, in their order") {
    assertEquals(
      summon[Encoder[User]].encode(User("Ada", 36, Address("London"), Some("ada"), List("x"))),
      JObject(
        List(
          "name" -> JString("Ada"),
          "age" -> JNumber(36),
          "address" -> JObject(List("city" -> JString("London"))),
          "nick" -> JString("ada"),
          "tags" -> JArray(List(JString("x")))
        )
      )
    )
  }

  test("an empty Option is null, and an empty List an empty array") {
    assertEquals(
      summon[Encoder[User]].encode(User("Ada", 36, Address("London"), None, Nil)),
      JObject(
        List(
          "name" -> JString("Ada"),
          "age" -> JNumber(36),
          "address" -> JObject(List("city" -> JString("London"))),
          "nick" -> JNull,
          "tags" -> JArray(Nil)
        )
      )
    )
  }

  test("an enum case carries its name under \"type\"") {
    assertEquals(
      summon[Encoder[Shape]].encode(Shape.Circle(2)),
      JObject(List("type" -> JString("Circle"), "radius" -> JNumber(2)))
    )
    assertEquals(summon[Encoder[Shape]].encode(Shape.Dot), JObject(List("type" -> JString("Dot"))))
  }

  test("a recursive type encodes all the way down") {
    def leaf(n: Int) = JObject(List("type" -> JString("Leaf"), "value" -> JNumber(n)))
    assertEquals(
      summon[Encoder[Tree]].encode(Tree.Node(Tree.Leaf(1), Tree.Leaf(2))),
      JObject(List("type" -> JString("Node"), "left" -> leaf(1), "right" -> leaf(2)))
    )
  }
end Exercise01Spec
