package typeprog.ch20validation

import typeprog.core.TypeLevelSuite

import Exercise04.*
import Exercise04.Validated.{Invalid, Valid}

class Exercise04Spec extends TypeLevelSuite:

  private def positive(n: Int): Checked[Int] =
    if n > 0 then Valid(n) else Invalid(List(s"$n is not positive"))

  private def positiveOption(n: Int): Option[Int] = Option.when(n > 0)(n)

  // Guard: traverse asks for an Applicative, and Set has none here.
  test("traverse does not compile for a type with no Applicative") {
    assertTypeError("traverse(List(1))(n => Set(n))")
  }

  test("traverse with Validated: every element valid, in order") {
    assertEquals(traverse(List(1, 2, 3))(positive), Valid(List(1, 2, 3)))
  }

  test("traverse with Validated: the errors of every element that failed, in order") {
    assertEquals(
      traverse(List(1, -2, 3, 0))(positive),
      Invalid(List("-2 is not positive", "0 is not positive"))
    )
  }

  test("traverse with Option: Some of every element, in order, or None") {
    assertEquals(traverse(List(1, 2, 3))(positiveOption), Some(List(1, 2, 3)))
    assertEquals(traverse(List(1, -2, 3))(positiveOption), None)
  }

  test("traverse of an empty list is valid, and empty") {
    assertEquals(traverse(List.empty[Int])(positive), Valid(Nil))
    assertEquals(traverse(List.empty[Int])(positiveOption), Some(Nil))
  }

  test("a valid form signs up") {
    assertEquals(signUp("Ada", 36, "ada@example.com"), Valid(User("Ada", 36, "ada@example.com")))
  }

  test("a form with one wrong field reports that one") {
    assertEquals(signUp("Ada", 12, "ada@example.com"), Invalid(List("must be 18 or older")))
  }

  test("a form with every field wrong reports all of them, in field order") {
    assertEquals(
      signUp("", 12, "x"),
      Invalid(List("name is empty", "must be 18 or older", "email has no @"))
    )
    assertEquals(signUp(" ", 30, "x"), Invalid(List("name is empty", "email has no @")))
  }

  test("signUpAll reports the failures of every form, in order") {
    val forms = List(("", 30, "a@b"), ("Ada", 36, "ada@example.com"), ("Bob", 12, "x"))
    assertEquals(
      signUpAll(forms),
      Invalid(List("name is empty", "must be 18 or older", "email has no @"))
    )
  }

  test("signUpAll of valid forms, and of none") {
    assertEquals(
      signUpAll(List(("Ada", 36, "ada@example.com"), ("Bob", 18, "bob@example.com"))),
      Valid(List(User("Ada", 36, "ada@example.com"), User("Bob", 18, "bob@example.com")))
    )
    assertEquals(signUpAll(Nil), Valid(Nil))
  }
end Exercise04Spec
