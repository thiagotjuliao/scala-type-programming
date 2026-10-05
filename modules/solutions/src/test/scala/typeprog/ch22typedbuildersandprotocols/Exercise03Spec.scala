package typeprog.ch22typedbuildersandprotocols

import typeprog.core.TypeLevelSuite

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  private val server = Server[(Hello, Auth, Data)]()
  private val other = Server[(Hello, Auth, Data)]()

  test("a complete conversation closes into a transcript of the protocol's type") {
    assertTypeChecks("""
      val transcript: (Hello, Auth, Data) =
        server.close(server.connect().send(Hello("ada")).send(Auth("t0k3n")).send(Data("42")))
    """)
  }

  test("a message out of order does not compile") {
    assertTypeError("server.connect().send(Auth(\"t0k3n\"))")
    assertTypeError("server.connect().send(Hello(\"ada\")).send(Data(\"42\"))")
  }

  test("nor does a message after the last") {
    assertTypeError(
      "server.connect().send(Hello(\"ada\")).send(Auth(\"t\")).send(Data(\"1\")).send(Data(\"2\"))"
    )
  }

  test("a conversation closes only once it is complete") {
    assertTypeError("server.close(server.connect())")
    assertTypeError("server.close(server.connect().send(Hello(\"ada\")).send(Auth(\"t\")))")
  }

  test("and only on the server that opened it") {
    assertTypeError(
      "other.close(server.connect().send(Hello(\"ada\")).send(Auth(\"t\")).send(Data(\"42\")))"
    )
  }

  // Compared as Any: against the stub, close returns a plain Tuple.
  test("the transcript holds the messages sent, in order") {
    val transcript =
      server.close(server.connect().send(Hello("ada")).send(Auth("t0k3n")).send(Data("42")))
    assertEquals[Any, Any](transcript, (Hello("ada"), Auth("t0k3n"), Data("42")))
  }

  test("an empty protocol closes at once, into an empty transcript") {
    val silent = Server[EmptyTuple]()
    assertEquals[Any, Any](silent.close(silent.connect()), EmptyTuple)
  }
end Exercise03Spec
