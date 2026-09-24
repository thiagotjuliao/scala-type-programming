package typeprog.ch04literaluniontypes

import typeprog.core.TypeLevelSuite

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  test("header keeps the subclass it is called on") {
    assertTypeChecks("JsonRequest().header(\"Accept\", \"json\").body(\"{}\")")
    assertTypeChecks("val r: JsonRequest = JsonRequest().header(\"Accept\", \"json\")")
  }

  test("header hands back the very request it was called on") {
    assertTypeChecks("val r = JsonRequest(); val same: r.type = r.header(\"Accept\", \"json\")")
  }

  test("a plain Request still has no body") {
    assertTypeError("Request().header(\"Accept\", \"json\").body(\"{}\")")
  }

  test("withHeaders returns its argument's own type") {
    assertTypeChecks(
      "val r = JsonRequest(); val same: r.type = withHeaders(r)(\"Accept\" -> \"json\")"
    )
  }

  test("withHeaders keeps the subclass for a chain") {
    assertTypeChecks(
      "val r = JsonRequest(); withHeaders(r)(\"Accept\" -> \"json\").body(\"{}\")"
    )
  }

  test("withHeaders does not turn a Request into a JsonRequest") {
    assertTypeError("val r = Request(); withHeaders(r)(\"Accept\" -> \"json\").body(\"{}\")")
  }

  // These compile against the stub too — its signatures return `Request` — so
  // they run, and against `???` they fail, which is a red test.
  test("withHeaders adds every header, in order, to the request it is given") {
    val r = Request()
    withHeaders(r)("Accept" -> "json", "Host" -> "example.com")
    assertEquals(r.headers, List("Accept" -> "json", "Host" -> "example.com"))
  }

  test("withHeaders returns the same instance, not a copy") {
    val r = JsonRequest()
    assert(withHeaders(r)("Accept" -> "json") eq r)
  }

  // A guard rather than a claim: the bodies ship finished, so this passes
  // against the stub as well. It catches a solution that breaks them.
  test("header and body record what they are given") {
    val r = JsonRequest()
    r.header("Accept", "json")
    r.body("{}")
    assertEquals(r.headers, List("Accept" -> "json"))
    assertEquals(r.payload, "{}")
  }
end Exercise02Spec
