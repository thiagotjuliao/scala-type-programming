package typeprog.ch05opaquetypes

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  test("a Port can be used wherever an Int is expected") {
    assertTypeChecks("val n: Int = Port.Http")
    assertTypeChecks("evidence.subtypeOf[Port, Int]")
  }

  test("arithmetic on a Port gives an Int") {
    assertTypeChecks("val next: Int = Port.Http + 1")
  }

  test("an Int is not a Port") {
    assertTypeError("val p: Port = 8080")
    assertTypeError("val p: Port = Port.Http + 1")
    assertTypeError("evidence.sameType[Port, Int]")
  }

  // `Port.from` returns an `Option[Port]`, which compiles against the stub as
  // well; `???` makes these fail there.
  test("from accepts 1 to 65535") {
    assertEquals[Any, Any](Port.from(1), Some(1))
    assertEquals[Any, Any](Port.from(65535), Some(65535))
  }

  test("from rejects anything outside that range") {
    assertEquals(Port.from(0), None)
    assertEquals(Port.from(65536), None)
    assertEquals(Port.from(-80), None)
  }

  // A guard: the stub ships the named ports, so this passes there too.
  test("the named ports are the well-known numbers") {
    assertEquals[Any, Any](Port.Http, 80)
    assertEquals[Any, Any](Port.Https, 443)
  }
end Exercise02Spec
