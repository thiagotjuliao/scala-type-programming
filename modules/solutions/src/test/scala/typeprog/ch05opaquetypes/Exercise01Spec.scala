package typeprog.ch05opaquetypes

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  test("outside its scope, an Email is not a String") {
    assertTypeError("evidence.sameType[Email, String]")
    assertTypeError("evidence.subtypeOf[Email, String]")
  }

  test("a String cannot be used as an Email") {
    assertTypeError("val e: Email = \"ada@example.com\"")
  }

  test("an Email cannot be used as a String") {
    assertTypeError("Email.parse(\"ada@example.com\").map(e => (e: String))")
  }

  test("String's own methods are not available on an Email") {
    assertTypeError("Email.parse(\"ada@example.com\").map(_.length)")
  }

  test("the extensions apply to an Email, and not to a String") {
    assertTypeChecks("val d: Option[String] = Email.parse(\"ada@example.com\").map(_.domain)")
    assertTypeError("\"ada@example.com\".domain")
  }

  // These compile against the stub as well — the stub's `Email` is a plain
  // alias, so everything type-checks — and run into `???`.
  test("parse accepts an address with exactly one @ and something on each side") {
    assertEquals(Email.parse("ada@example.com").map(_.value), Some("ada@example.com"))
  }

  test("parse rejects anything else") {
    assertEquals(Email.parse("ada.example.com"), None)
    assertEquals(Email.parse("@example.com"), None)
    assertEquals(Email.parse("ada@"), None)
    assertEquals(Email.parse("ada@home@example.com"), None)
  }

  test("domain is what follows the @") {
    assertEquals(Email.parse("ada@example.com").map(_.domain), Some("example.com"))
  }
end Exercise01Spec
