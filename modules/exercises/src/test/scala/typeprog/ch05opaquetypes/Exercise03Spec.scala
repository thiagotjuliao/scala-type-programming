package typeprog.ch05opaquetypes

import typeprog.core.TypeLevelSuite

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  // A guard rather than a claim: the stub allows every call, so this passes
  // against it too. It catches a solution that forbids the intended order.
  test("the intended order compiles: open, send, close") {
    assertTypeChecks(
      "val c: Connection[Closed] = Connection.to(\"db\").open.send(\"a\").send(\"b\").close"
    )
  }

  // Also a guard: the stub's result types are already the right ones.
  test("opening gives an open connection, closing a closed one") {
    assertTypeChecks("val c: Connection[Open] = Connection.to(\"db\").open")
    assertTypeChecks("val c: Connection[Closed] = Connection.to(\"db\").open.close")
  }

  test("a connection that was never opened cannot send") {
    assertTypeError("Connection.to(\"db\").send(\"a\")")
  }

  test("a closed connection cannot send") {
    assertTypeError("Connection.to(\"db\").open.close.send(\"a\")")
  }

  test("an open connection cannot be opened again") {
    assertTypeError("Connection.to(\"db\").open.open")
  }

  test("a closed connection cannot be closed again") {
    assertTypeError("Connection.to(\"db\").close")
    assertTypeError("Connection.to(\"db\").open.close.close")
  }

  test("a connection cannot be built in any state but Closed") {
    assertTypeError("val c: Connection[Open] = Connection.to(\"db\")")
  }

  // Also guards: the bodies ship finished, and the stub runs them.
  test("send records each message, in order") {
    val c = Connection.to("db").open.send("a").send("b")
    assertEquals(c.sent, List("a", "b"))
  }

  test("the host survives every transition") {
    assertEquals(Connection.to("db").open.close.host, "db")
  }
end Exercise03Spec
