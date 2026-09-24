package typeprog.ch03pathdependenttypes

import typeprog.core.TypeLevelSuite

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  val port: Key.Aux[Int] = Key[Int]("port")(_.toIntOption)
  val host: Key.Aux[String] = Key[String]("host")(Some(_))

  test("get returns the value type of the key it is given") {
    assertTypeChecks("val p: Option[Int] = Settings.empty.set(port)(8080).get(port)")
    assertTypeChecks("val h: Option[String] = Settings.empty.set(host)(\"localhost\").get(host)")
  }

  test("get does not return some other key's type") {
    assertTypeError("val h: Option[Int] = Settings.empty.get(host)")
  }

  test("set only takes the value type of its key") {
    assertTypeError("Settings.empty.set(port)(\"8080\")")
    assertTypeError("Settings.empty.set(host)(8080)")
  }

  // A key known only as `Key` still has a `Value` — abstract, but its own. A
  // store typed by the key has to let a value read with a key be written back
  // with the same key, without ever learning what the type is.
  test("an abstract key round-trips at its own abstract type") {
    assertTypeChecks(
      "def copy(k: Key, from: Settings, to: Settings): Settings = from.get(k).fold(to)(to.set(k))"
    )
    assertTypeChecks(
      "def roundTrip(k: Key)(v: k.Value): Option[k.Value] = Settings.empty.set(k)(v).get(k)"
    )
  }

  // These compile against the stub too — its signatures take and return
  // `Any` — so they run, and against `???` they fail, which is a red test.
  test("a value that was set is read back") {
    assertEquals[Any, Any](Settings.empty.set(port)(8080).get(port), Some(8080))
  }

  test("a key that was never set reads as None") {
    assertEquals[Any, Any](Settings.empty.set(port)(8080).get(host), None)
  }

  test("setting a key again replaces its value") {
    assertEquals[Any, Any](Settings.empty.set(port)(80).set(port)(8080).get(port), Some(8080))
  }
end Exercise02Spec
