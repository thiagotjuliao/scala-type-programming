package typeprog.ch24derivedjsoncodec

import typeprog.core.TypeLevelSuite

import Exercise03.*
import Exercise03.Json.*

object Exercise03Fixtures:

  final case class User(name: String, age: Int)

  final case class Event(name: String, at: java.time.Instant)

  final case class Meeting(title: String, event: Event)

import Exercise03Fixtures.*

class Exercise03Spec extends TypeLevelSuite:

  // A guard: it holds against the stub, whose derivation works for what it
  // can encode. It catches a fallback that fires for every field.
  test("a type whose every field has an encoder derives as before") {
    assertTypeChecks("Encoder.derived[User]")
    assertEquals(
      Encoder.derived[User].encode(User("Ada", 36)),
      JObject(List("name" -> JString("Ada"), "age" -> JNumber(36)))
    )
  }

  test("a field with no encoder is a compile error naming the type and the field") {
    assertTypeErrorStartsWith(
      "Encoder.derived[Event]",
      "cannot derive Encoder for Event: field at has no Encoder"
    )
  }

  test("and the same error, when that type is a field of the one being derived") {
    assertTypeErrorStartsWith(
      "Encoder.derived[Meeting]",
      "cannot derive Encoder for Event: field at has no Encoder"
    )
  }

  test("a plain search for a missing encoder says what to do") {
    assertTypeErrorStartsWith(
      "summon[Encoder[java.time.Instant]]",
      "no Encoder for java.time.Instant"
    )
  }
end Exercise03Spec
