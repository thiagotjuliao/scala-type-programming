package typeprog.ch08inlinescalacompiletime

import typeprog.core.TypeLevelSuite

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  test("Int plus Int is an Int, statically") {
    assertTypeChecks("val i: Int = add(1, 2)")
  }

  test("Int and Double, in either order, make a Double") {
    assertTypeChecks("val d: Double = add(1, 2.5)")
    assertTypeChecks("val d: Double = add(2.5, 1)")
    assertTypeChecks("val d: Double = add(2.5, 0.5)")
  }

  test("String plus String is a String") {
    assertTypeChecks("""val s: String = add("a", "b")""")
  }

  // A guard: against the stub's `Any` these are rejected too. They catch a
  // result type that is too wide, or one that is `Nothing` and fits anything.
  test("the result type is the one computed, and no other") {
    assertTypeError("val i: Int = add(1, 2.5)")
    assertTypeError("val s: String = add(1, 2)")
  }

  // The choice is made on the static types, not on literal values: variables
  // work as well as constants.
  test("the arguments need not be constants") {
    assertTypeChecks("def f(x: Int, y: Double): Double = add(x, y)")
  }

  test("values that cannot be added are rejected") {
    assertTypeError("""add(1, "a")""")
    assertTypeError("add(true, false)")
  }

  // Against the stub, which returns `Any` for anything, this compiles.
  test("a generic caller does not know which case applies") {
    assertTypeError("def f[A](a: A) = add(a, a)")
  }

  // `add` compiles against the stub and the results are compared as `Any`;
  // against `???` they fail, which is a red test.
  test("the arithmetic is the arithmetic of the values") {
    assertEquals[Any, Any](add(1, 2), 3)
    assertEquals[Any, Any](add(1, 2.5), 3.5)
    assertEquals[Any, Any](add(2.5, 1), 3.5)
    assertEquals[Any, Any](add("a", "b"), "ab")
  }
end Exercise03Spec
