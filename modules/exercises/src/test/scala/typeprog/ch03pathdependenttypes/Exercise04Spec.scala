package typeprog.ch03pathdependenttypes

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise04.*

class Exercise04Spec extends TypeLevelSuite:

  test("port's value type is Int") {
    assertTypeChecks("evidence.sameType[port.Value, Int]")
  }

  test("port's value type is not String") {
    assertTypeError("evidence.sameType[port.Value, String]")
  }

  test("host is a path, and its value type is String") {
    assertTypeChecks("evidence.sameType[host.Value, String]")
  }

  test("host's value type is not Int") {
    assertTypeError("evidence.sameType[host.Value, Int]")
  }

  test("a key made by keyOf keeps the value type it was made with") {
    assertTypeChecks(
      "val timeout = keyOf[Long](\"timeout\")(_.toLongOption); evidence.sameType[timeout.Value, Long]"
    )
    assertTypeChecks(
      "val retries = keyOf(\"retries\")(_.toIntOption); evidence.sameType[retries.Value, Int]"
    )
  }

  test("keyOf does not make every key's value type the same") {
    assertTypeError(
      "val timeout = keyOf[Long](\"timeout\")(_.toLongOption); evidence.sameType[timeout.Value, Int]"
    )
  }

  // What the three repairs are for: reading through the key gives back its type
  // with no cast, which is what Exercise 02's `get` relies on.
  test("parsing through each key gives its own type") {
    assertTypeChecks("val p: Option[Int] = port.parse(\"8080\")")
    assertTypeChecks("val h: Option[String] = host.parse(\"localhost\")")
  }

  // Unaffected by the repairs: the keys still parse, before and after.
  test("the keys still parse") {
    assertEquals[Any, Any](port.parse("8080"), Some(8080))
    assertEquals[Any, Any](host.parse("localhost"), Some("localhost"))
    assertEquals[Any, Any](keyOf("retries")(_.toIntOption).parse("3"), Some(3))
  }
end Exercise04Spec
