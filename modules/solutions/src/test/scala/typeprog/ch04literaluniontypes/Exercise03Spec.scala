package typeprog.ch04literaluniontypes

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  test("PortResult lists the port and every way of not getting one") {
    assertTypeChecks("evidence.sameType[PortResult, Int | Missing | Malformed | OutOfRange]")
  }

  test("PortResult does not leave a failure out") {
    assertTypeError("evidence.subtypeOf[PortResult, Int | Missing | Malformed]")
    assertTypeError("evidence.subtypeOf[PortResult, Int | Missing | OutOfRange]")
  }

  test("PortResult is not a catch-all") {
    assertTypeError("evidence.sameType[PortResult, Any]")
    assertTypeError("evidence.subtypeOf[String, PortResult]")
  }

  test("port returns a PortResult") {
    assertTypeChecks(
      "val r: Int | Missing | Malformed | OutOfRange = port(Map(\"port\" -> \"8080\"))"
    )
  }

  test("explain takes every member of the union, and nothing else") {
    assertTypeChecks(
      "explain(8080); explain(Missing(\"port\")); explain(Malformed(\"port\", \"x\")); explain(OutOfRange(\"port\", 0))"
    )
    assertTypeError("explain(\"8080\")")
  }

  // `port` compiles as plain code against the stub (it takes a `Map`) and its
  // result is compared as `Any`, so these run — and against `???` they fail.
  test("a valid port is returned as an Int") {
    assertEquals[Any, Any](port(Map("port" -> "8080")), 8080)
  }

  test("an absent key is Missing") {
    assertEquals[Any, Any](port(Map.empty), Missing("port"))
  }

  test("a value that is not a number is Malformed") {
    assertEquals[Any, Any](port(Map("port" -> "eighty")), Malformed("port", "eighty"))
  }

  test("a number outside 1 to 65535 is OutOfRange") {
    assertEquals[Any, Any](port(Map("port" -> "0")), OutOfRange("port", 0))
    assertEquals[Any, Any](port(Map("port" -> "70000")), OutOfRange("port", 70000))
    assertEquals[Any, Any](port(Map("port" -> "65535")), 65535)
  }

  // `explain` takes a `PortResult`, which is `Unsolved` in the stub; the cast
  // compiles against both, and against the stub it fails at runtime.
  private def result(value: Any): PortResult = value.asInstanceOf[PortResult]

  test("explain says what each result means") {
    assertEquals(explain(result(8080)), "port 8080")
    assertEquals(explain(result(Missing("port"))), "port is not set")
    assertEquals(explain(result(Malformed("port", "x"))), "port is not a number: x")
    assertEquals(explain(result(OutOfRange("port", 0))), "port is out of range: 0")
  }
end Exercise03Spec
