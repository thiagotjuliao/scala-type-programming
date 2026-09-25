package typeprog.ch06typeclassesgivens

import typeprog.core.TypeLevelSuite

import Exercise04.*

class Exercise04Spec extends TypeLevelSuite:

  test("Int plus Int is an Int") {
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

  // The result type is exact, not merely wide enough: a mixed sum is not an Int.
  test("the result type is the one computed, and no other") {
    assertTypeError("val i: Int = add(1, 2.5)")
    assertTypeError("val s: String = add(1, 2)")
  }

  test("pairs add component by component, each with its own result type") {
    assertTypeChecks("""val p: (Double, String) = add((1, "a"), (2.5, "b"))""")
    assertTypeChecks("val p: (Int, (Double, Int)) = add((1, (1, 1)), (2, (0.5, 2)))")
    assertTypeError("""val p: (Int, String) = add((1, "a"), (2.5, "b"))""")
  }

  test("values that cannot be added are rejected") {
    assertTypeError("""add(1, "a")""")
    assertTypeError("add(true, false)")
    assertTypeError("add((1, 2), 3)")
    assertTypeError("""add((1, "a"), (2, 3))""")
  }

  // Compared as `Any`: the exact result types are the claims of the tests
  // above. Compared at their own types, an `add` whose instances do not yet
  // say what `Out` is — the trap this exercise is about — finds no munit
  // `Compare` and takes the whole module down instead of failing a test.
  // Against the stub's `???` they fail, which is a red test. An instance that
  // is missing altogether still stops compilation — calling `add` at all needs
  // one — but then the error names the instance.
  test("the arithmetic is the arithmetic of the values") {
    assertEquals[Any, Any](add(1, 2), 3)
    assertEquals[Any, Any](add(1, 2.5), 3.5)
    assertEquals[Any, Any](add("a", "b"), "ab")
    assertEquals[Any, Any](add((1, "a"), (2.5, "b")), (3.5, "ab"))
  }
end Exercise04Spec
