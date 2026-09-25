package typeprog.ch07matchtypes

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  test("the first of a String is an Option[Char]") {
    assertTypeChecks("""val c: Option[Char] = first("abc")""")
    assertTypeError("""val c: Option[String] = first("abc")""")
  }

  test("the first of a List is an Option of its element") {
    assertTypeChecks("val i: Option[Int] = first(List(1, 2))")
    assertTypeError("val i: Option[String] = first(List(1, 2))")
  }

  test("the first of an Option is the Option itself") {
    assertTypeChecks("val o: Option[Int] = first(Option(1))")
    assertTypeChecks("val o: Option[Int] = first(Some(1))")
    assertTypeError("val o: Option[Option[Int]] = first(Option(1))")
  }

  test("First is the type that first returns") {
    assertTypeChecks("evidence.sameType[First[String], Option[Char]]")
    assertTypeChecks("evidence.sameType[First[List[Boolean]], Option[Boolean]]")
  }

  // Against the stub these compile — `first` takes anything — which is what
  // makes them red there. `First[Int]` would be a stuck match type, and
  // `first(42)` a MatchError at runtime.
  test("anything else is rejected at the call") {
    assertTypeError("first(42)")
    assertTypeError("first(Vector(1))")
  }

  // These compile against the stub too: its signature takes anything and the
  // results are compared as `Any`. Against `???` they fail, which is a red test.
  test("the values are the first elements") {
    assertEquals[Any, Any](first("abc"), Some('a'))
    assertEquals[Any, Any](first(List(1, 2)), Some(1))
    assertEquals[Any, Any](first(Option(3)), Some(3))
  }

  test("an empty argument has no first element") {
    assertEquals[Any, Any](first(""), None)
    assertEquals[Any, Any](first(List.empty[Int]), None)
    assertEquals[Any, Any](first(Option.empty[Int]), None)
  }
end Exercise03Spec
