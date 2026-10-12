package typeprog.ch25macrocheckedinterpolator

import typeprog.core.TypeLevelSuite

import Exercise02.*
import Json.*

class Exercise02Spec extends TypeLevelSuite:

  test("arguments are values, each encoded by its Encoder") {
    val name = "Ada"
    val age = 36
    val tags = List("a", "b")
    val boss: Option[String] = None
    assertEquals(
      json"""{"name": $name, "age": $age, "tags": $tags, "boss": $boss}""",
      JObject(
        List(
          "name" -> JString("Ada"),
          "age" -> JNumber(36),
          "tags" -> JArray(List(JString("a"), JString("b"))),
          "boss" -> JNull
        )
      )
    )
  }

  // A literal argument has a literal type, `1` or `"one"`, and there is no
  // `Encoder[1]`: this is the test that fails when the type is not widened.
  test("a literal as an argument is encoded as its widened type") {
    assertEquals(
      json"[${1}, ${"one"}, ${true}]",
      JArray(List(JNumber(1), JString("one"), JBool(true)))
    )
  }

  test("a Json argument is put in as it is") {
    val inner = JArray(List(JNumber(1)))
    assertEquals(json"""{"inner": $inner}""", JObject(List("inner" -> inner)))
  }

  test("an argument with no Encoder does not compile, and names its type") {
    assertTypeErrorStartsWith(
      """{ val at = java.time.Instant.EPOCH; json"[$at]" }""",
      "no Encoder for Instant"
    )
  }

  test("an argument is a value: as a key, or inside a string, it does not compile") {
    assertTypeErrorContains("""{ val k = "id"; json"{$k: 1}" }""", "key")
    assertTypeErrorStartsWith(
      "{ val n = 1; json\"\"\"[\"a$n\"]\"\"\" }",
      "invalid JSON: an argument cannot be inside a string"
    )
  }

  test("the literal is still checked around the arguments") {
    assertTypeErrorStartsWith(
      """{ val n = 1; json"[$n $n]" }""",
      "invalid JSON: expected ',' or ']'"
    )
  }
end Exercise02Spec
