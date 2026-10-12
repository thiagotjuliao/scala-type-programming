package typeprog.ch25macrocheckedinterpolator

import typeprog.core.TypeLevelSuite

import Exercise03.*
import Json.*

class Exercise03Spec extends TypeLevelSuite:

  test("a String argument can be a key") {
    val k = "id"
    assertEquals(json"{$k: 1}", JObject(List("id" -> JNumber(1))))
    assertEquals(json"{${"id"}: 1}", JObject(List("id" -> JNumber(1))))
  }

  test("keys and values from arguments, beside literal ones") {
    val k = "n"
    val v = 2
    assertEquals(
      json"""{$k: $v, "m": [$v]}""",
      JObject(List("n" -> JNumber(2), "m" -> JArray(List(JNumber(2)))))
    )
  }

  test("a key that is not a String does not compile, and names its type") {
    assertTypeErrorStartsWith(
      """{ val n = 1; json"{$n: 1}" }""",
      "a key must be a String, not Int"
    )
    assertTypeErrorStartsWith(
      """{ val o: Option[String] = None; json"{$o: 1}" }""",
      "a key must be a String, not Option[String]"
    )
  }

  // An `Encoder` cannot make a key out of anything, so a key is checked as a
  // key: the error is about the place, not about the missing instance.
  test("a key is checked as a key, whatever Encoders exist") {
    assertTypeErrorStartsWith(
      """{ val at = java.time.Instant.EPOCH; json"{$at: 1}" }""",
      "a key must be a String, not Instant"
    )
  }

  // A guard: it holds against the stub, which ships exercise 02 solved. It
  // catches a key check that took over the values too.
  test("a value still needs an Encoder") {
    assertTypeErrorStartsWith(
      """{ val at = java.time.Instant.EPOCH; json"[$at]" }""",
      "no Encoder for Instant"
    )
  }
end Exercise03Spec
