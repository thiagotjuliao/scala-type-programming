package typeprog.ch12macrosquotessplices

import typeprog.core.TypeLevelSuite

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  test("a valid literal is a working regular expression") {
    assertEquals(regex("[a-z]+").matches("abc"), true)
    assertEquals(regex("[a-z]+").matches("ABC"), false)
    assertEquals(regex("""\d{3}""").findAllIn("12 345 6789").toList, List("345", "678"))
  }

  test("an invalid pattern does not compile, and says so") {
    assertTypeErrorContains("""regex("[a-z")""", "invalid regex")
    assertTypeErrorContains("""regex("(ab")""", "invalid regex")
  }

  test("a string that is not a literal does not compile, and says so") {
    assertTypeErrorContains("""{ val s = "[a-z]"; regex(s) }""", "literal")
  }
end Exercise02Spec
