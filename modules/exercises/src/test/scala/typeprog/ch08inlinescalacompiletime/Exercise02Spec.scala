package typeprog.ch08inlinescalacompiletime

import typeprog.core.TypeLevelSuite

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  final class Widget

  // `default[T]` compiles against the stub for every `T`, and the results are
  // compared as `Any`; against `???` they fail, which is a red test.
  test("numbers default to zero") {
    assertEquals[Any, Any](default[Int], 0)
    assertEquals[Any, Any](default[Long], 0L)
    assertEquals[Any, Any](default[Double], 0.0)
  }

  test("a Boolean defaults to false and a String to empty") {
    assertEquals[Any, Any](default[Boolean], false)
    assertEquals[Any, Any](default[String], "")
  }

  test("an Option defaults to None and a List to empty, whatever they hold") {
    assertEquals[Any, Any](default[Option[Widget]], None)
    assertEquals[Any, Any](default[List[Widget]], Nil)
  }

  test("a pair defaults to the pair of its components' defaults, at any depth") {
    assertEquals[Any, Any](default[(Int, String)], (0, ""))
    assertEquals[Any, Any](default[(Boolean, (Long, Option[Int]))], (false, (0L, None)))
  }

  test("a type with no default does not compile, and says why") {
    assertTypeErrorContains("default[Widget]", "no default value")
    assertTypeErrorContains("default[Char]", "no default value")
  }

  // A pair is only as defaultable as its parts.
  test("a pair with a part that has no default does not compile") {
    assertTypeError("default[(Int, Widget)]")
    assertTypeError("default[(Int, (String, Widget))]")
  }
end Exercise02Spec
