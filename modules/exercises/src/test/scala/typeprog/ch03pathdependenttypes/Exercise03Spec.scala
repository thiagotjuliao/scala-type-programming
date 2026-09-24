package typeprog.ch03pathdependenttypes

import typeprog.core.TypeLevelSuite

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  val port: Key.Aux[Int] = Key[Int]("port")(_.toIntOption)
  val host: Key.Aux[String] = Key[String]("host")(Some(_))

  val raw: Map[String, String] = Map("port" -> "8080", "host" -> "localhost")

  test("parser is a dependent function") {
    assertTypeChecks("val f: (k: Key) => String => Option[k.Value] = parser")
  }

  test("parser reads each key at its own type") {
    assertTypeChecks("val p: Option[Int] = parser(port)(\"8080\")")
    assertTypeChecks("val h: Option[String] = parser(host)(\"localhost\")")
  }

  test("loadWith returns a dependent function too") {
    assertTypeChecks("val read: (k: Key) => Option[k.Value] = loadWith(raw)(parser)")
    assertTypeChecks("val p: Option[Int] = loadWith(raw)(parser)(port)")
  }

  test("loadWith does not mix up the keys' types") {
    assertTypeError("val p: Option[String] = loadWith(raw)(parser)(port)")
  }

  // The reader handed to `loadWith` has to answer every key at *that* key's
  // type. One that always parses a port answers `host` with an `Int`, and a
  // dependent parameter type is what refuses it.
  test("loadWith refuses a reader that ignores the key") {
    assertTypeError("loadWith(raw)((k: Key) => (s: String) => port.parse(s))")
  }

  // These compile against the stub too — its types are `Key => ... Option[Any]`
  // — so they run, and against `???` they fail, which is a red test.
  test("a key present in the raw map is parsed") {
    assertEquals[Any, Any](loadWith(raw)(parser)(port), Some(8080))
    assertEquals[Any, Any](loadWith(raw)(parser)(host), Some("localhost"))
  }

  test("a key missing from the raw map reads as None") {
    assertEquals[Any, Any](loadWith(Map.empty)(parser)(port), None)
  }

  test("a value that does not parse reads as None") {
    assertEquals[Any, Any](loadWith(Map("port" -> "eighty"))(parser)(port), None)
  }
end Exercise03Spec
