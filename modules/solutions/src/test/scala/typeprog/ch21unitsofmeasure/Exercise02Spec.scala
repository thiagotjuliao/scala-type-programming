package typeprog.ch21unitsofmeasure

import typeprog.core.TypeLevelSuite

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  test("Half halves every exponent") {
    assertTypeChecks("summon[Half[(2, -4, 0)] =:= (1, -2, 0)]")
    assertTypeChecks("summon[Half[Dimensionless] =:= Dimensionless]")
  }

  test("the square root of an area is a length") {
    assertTypeChecks("val m: Quantity[Metre] = (metres(3) * metres(3)).sqrt")
    assertTypeError("val m: Quantity[Second] = (metres(3) * metres(3)).sqrt")
  }

  test("the square root of a speed squared is a speed") {
    assertTypeChecks("""
      val v = metres(10) / seconds(2)
      val back: Quantity[(1, -1, 0)] = (v * v).sqrt
    """)
  }

  test("a unit with an odd exponent has no square root") {
    assertTypeError("metres(1).sqrt")
    assertTypeError("(metres(1) * metres(1) * metres(1)).sqrt")
    assertTypeError("(metres(2) * metres(2) / seconds(1)).sqrt")
  }

  // Generic code cannot know whether U's exponents are even, so the promise
  // has to be asked of its caller in turn — which is what makes this
  // assertion fail on a sqrt that only warns.
  test("nor does a unit nobody knows yet") {
    assertTypeError("def root[U <: Dim](q: Quantity[U]) = q.sqrt")
  }

  test("the number is the square root") {
    assertEquals((metres(3) * metres(3)).sqrt.value, 3.0)
    assertEquals((seconds(0) * seconds(0)).sqrt.value, 0.0)
    assertEquals((metres(2) / metres(8)).sqrt.value, 0.5)
  }
end Exercise02Spec
