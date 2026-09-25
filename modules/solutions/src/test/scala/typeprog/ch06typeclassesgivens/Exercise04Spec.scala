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

  test("the arithmetic is the arithmetic of the values") {
    assertEquals(add(1, 2), 3)
    assertEquals(add(1, 2.5), 3.5)
    assertEquals(add("a", "b"), "ab")
    assertEquals(add((1, "a"), (2.5, "b")), (3.5, "ab"))
  }
end Exercise04Spec
