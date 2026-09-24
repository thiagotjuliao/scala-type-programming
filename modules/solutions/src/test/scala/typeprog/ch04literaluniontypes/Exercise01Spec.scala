package typeprog.ch04literaluniontypes

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  test("Status is made of the three codes and nothing else") {
    assertTypeChecks("evidence.sameType[Status, 200 | 404 | 500]")
  }

  test("Status is not simply Int") {
    assertTypeError("evidence.sameType[Status, Int]")
  }

  test("describe takes each of the three codes as a literal") {
    assertTypeChecks("describe(200); describe(404); describe(500)")
  }

  test("describe rejects any other code, and any Int that is not a literal") {
    assertTypeError("describe(201)")
    assertTypeError("val code: Int = 200; describe(code)")
  }

  test("the named constants are usable as a Status") {
    assertTypeChecks("describe(Ok); describe(NotFound); describe(ServerError)")
  }

  test("each constant is its own code, not a wider one") {
    assertTypeChecks("evidence.subtypeOf[Ok.type, 200]")
    assertTypeError("evidence.subtypeOf[Ok.type, 404]")
  }

  // Against the stub `Status` is `Unsolved`, so no literal can be handed to
  // `describe` as plain code. The cast compiles against both; against the stub
  // it fails at runtime, which is a red test.
  private def status(code: Int): Status = code.asInstanceOf[Status]

  test("describe names each code") {
    assertEquals(describe(status(200)), "OK")
    assertEquals(describe(status(404)), "Not Found")
    assertEquals(describe(status(500)), "Internal Server Error")
  }
end Exercise01Spec
