package typeprog.ch09tuplesashlists

import typeprog.core.TypeLevelSuite

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  // A guard: the numbers chapter 06 could already add.
  test("numbers still add as before") {
    assertTypeChecks("val i: Int = add(1, 2)")
    assertTypeChecks("val d: Double = add(1, 2.5)")
  }

  test("triples add element by element, each with its own result type") {
    assertTypeChecks("""val t: (Double, String, Double) = add((1, "a", 2.5), (2.5, "b", 1))""")
    assertTypeError("""val t: (Int, String, Double) = add((1, "a", 2.5), (2.5, "b", 1))""")
  }

  test("any length works, from none to five") {
    assertTypeChecks("val e: EmptyTuple = add(EmptyTuple, EmptyTuple)")
    assertTypeChecks("val one: Tuple1[Int] = add(Tuple1(1), Tuple1(2))")
    assertTypeChecks(
      """val five: (Int, Int, String, Double, Int) = add((1, 2, "a", 1, 3), (1, 2, "b", 0.5, 3))"""
    )
  }

  test("tuples nest") {
    assertTypeChecks("""val n: (Int, (Double, String)) = add((1, (1, "x")), (2, (0.5, "y")))""")
  }

  test("tuples of different lengths do not add") {
    assertTypeError("add((1, 2), (1, 2, 3))")
    assertTypeError("add(EmptyTuple, Tuple1(1))")
  }

  test("an element that does not add makes the whole tuple not add") {
    assertTypeError("""add((1, "a"), (2, 3))""")
  }

  // `add` compiles against the stub — it takes anything — and the results are
  // compared as `Any`; against `???` they fail, which is a red test.
  test("the values are added element by element") {
    assertEquals[Any, Any](add((1, "a", 2.5), (2.5, "b", 1)), (3.5, "ab", 3.5))
    assertEquals[Any, Any](add((1, (1, "x")), (2, (0.5, "y"))), (3, (1.5, "xy")))
    assertEquals[Any, Any](add(EmptyTuple, EmptyTuple), EmptyTuple)
  }
end Exercise03Spec
