package typeprog.ch25macrocheckedinterpolator

import typeprog.core.TypeLevelSuite

import Exercise01.*
import Json.*

class Exercise01Spec extends TypeLevelSuite:

  test("a literal is the JSON it says") {
    assertEquals(
      json"""{"name": "Ada", "age": 36, "admin": false, "boss": null, "tags": ["a", "b"]}""",
      JObject(
        List(
          "name" -> JString("Ada"),
          "age" -> JNumber(36),
          "admin" -> JBool(false),
          "boss" -> JNull,
          "tags" -> JArray(List(JString("a"), JString("b")))
        )
      )
    )
  }

  test("empty containers, numbers and JSON's escapes") {
    assertEquals(
      json"[[], {}, -1.5e2, 0]",
      JArray(List(JArray(Nil), JObject(Nil), JNumber(-150), JNumber(0)))
    )
    assertEquals(
      json"""["a\"b", "x\ny", "é"]""",
      JArray(List(JString("a\"b"), JString("x\ny"), JString("é")))
    )
  }

  test("invalid JSON does not compile, and says what is wrong") {
    assertTypeErrorStartsWith("""json"[1 2]"""", "invalid JSON: expected ',' or ']'")
    assertTypeErrorStartsWith("""json"[1,"""", "invalid JSON: expected a value, found the end")
    assertTypeErrorStartsWith("""json"{1: 2}"""", "invalid JSON: expected a key")
    assertTypeErrorStartsWith("""json"[1] 2"""", "invalid JSON: unexpected text after the value")
  }

  test("an argument does not compile yet: it is exercise 02's") {
    assertTypeErrorContains("""{ val n = 1; json"[$n]" }""", "argument")
  }

  test("parts that are not literals cannot be read, and do not compile") {
    assertTypeErrorContains("""{ val sc = StringContext("[1]"); sc.json() }""", "literal")
  }
end Exercise01Spec
