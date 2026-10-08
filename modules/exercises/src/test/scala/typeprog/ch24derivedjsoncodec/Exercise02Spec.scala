package typeprog.ch24derivedjsoncodec

import typeprog.core.TypeLevelSuite

import Exercise02.*
import Exercise02.Json.*

object Exercise02Fixtures:

  final case class Address(city: String)

  final case class User(name: String, age: Int, address: Address, nick: Option[String])
      derives Decoder

  enum Shape derives Decoder:
    case Circle(radius: Int)
    case Dot

  enum Tree derives Decoder:
    case Leaf(value: Int)
    case Node(left: Tree, right: Tree)

import Exercise02Fixtures.*

class Exercise02Spec extends TypeLevelSuite:

  private def user(fields: (String, Json)*) = summon[Decoder[User]].decode(JObject(fields.toList))
  private def shape(fields: (String, Json)*) = summon[Decoder[Shape]].decode(JObject(fields.toList))

  private val london = "address" -> JObject(List("city" -> JString("London")))

  // A guard: `derives` compiles against the stub too.
  test("derives Decoder gives an instance") {
    assertTypeChecks("summon[Decoder[User]]")
  }

  test("an object becomes the case class, its fields read by label in any order") {
    assertEquals(
      user(london, "age" -> JNumber(36), "nick" -> JString("ada"), "name" -> JString("Ada")),
      Right(User("Ada", 36, Address("London"), Some("ada")))
    )
  }

  test("an Option field may be null or absent") {
    val expected = Right(User("Ada", 36, Address("London"), None))
    assertEquals(
      user("name" -> JString("Ada"), "age" -> JNumber(36), london, "nick" -> JNull),
      expected
    )
    assertEquals(user("name" -> JString("Ada"), "age" -> JNumber(36), london), expected)
  }

  test("an enum case is chosen by its \"type\"") {
    assertEquals(shape("type" -> JString("Circle"), "radius" -> JNumber(2)), Right(Shape.Circle(2)))
    assertEquals(shape("type" -> JString("Dot")), Right(Shape.Dot))
  }

  test("a recursive type decodes all the way down") {
    def leaf(n: Int) = JObject(List("type" -> JString("Leaf"), "value" -> JNumber(n)))
    assertEquals(
      summon[Decoder[Tree]].decode(
        JObject(List("type" -> JString("Node"), "left" -> leaf(1), "right" -> leaf(2)))
      ),
      Right(Tree.Node(Tree.Leaf(1), Tree.Leaf(2)))
    )
  }

  test("a value of the wrong type fails at its field") {
    assertEquals(
      user("name" -> JString("Ada"), "age" -> JString("old"), london),
      Left(DecodeError(List("age"), "expected a whole number"))
    )
  }

  test("a missing field fails with its whole path") {
    assertEquals(user("age" -> JNumber(36), london), Left(DecodeError(List("name"), "missing")))
    assertEquals(
      user("name" -> JString("Ada"), "age" -> JNumber(36), "address" -> JObject(Nil)),
      Left(DecodeError(List("address", "city"), "missing"))
    )
  }

  test("an enum fails on a case it does not have, or no case at all") {
    assertEquals(
      shape("type" -> JString("Triangle")),
      Left(DecodeError(List("type"), "unknown case Triangle"))
    )
    assertEquals(shape("radius" -> JNumber(2)), Left(DecodeError(List("type"), "missing")))
  }

  test("and anything but an object is not a case class") {
    assertEquals(
      summon[Decoder[User]].decode(JString("Ada")),
      Left(DecodeError(Nil, "expected an object"))
    )
  }
end Exercise02Spec
