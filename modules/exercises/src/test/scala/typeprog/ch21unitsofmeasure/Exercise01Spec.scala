package typeprog.ch21unitsofmeasure

import typeprog.core.TypeLevelSuite

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  test("Mul adds exponents") {
    assertTypeChecks("summon[Mul[Metre, Metre] =:= (2, 0, 0)]")
    assertTypeChecks("summon[Mul[(1, -1, 0), Second] =:= Metre]")
    assertTypeChecks("summon[Mul[(1, 2, 3), (-4, 5, -6)] =:= (-3, 7, -3)]")
  }

  test("Div subtracts them") {
    assertTypeChecks("summon[Div[Metre, Second] =:= (1, -1, 0)]")
    assertTypeChecks("summon[Div[Metre, Metre] =:= Dimensionless]")
    assertTypeChecks("summon[Div[(1, 2, 3), (-4, 5, -6)] =:= (5, -3, 9)]")
  }

  // Guards against a Mul that ignores one side, or swaps the exponents.
  test("and neither confuses its operands' exponents") {
    assertTypeError("summon[Mul[Metre, Second] =:= Metre]")
    assertTypeError("summon[Div[Metre, Second] =:= (-1, 1, 0)]")
  }

  test("metres divided by seconds is a speed, not a length") {
    assertTypeChecks("val v: Quantity[(1, -1, 0)] = metres(10) / seconds(2)")
    assertTypeError("val v: Quantity[Metre] = metres(10) / seconds(2)")
  }

  test("units cancel with no rule written for it") {
    assertTypeChecks("val m: Quantity[Metre] = (metres(10) / seconds(2)) * seconds(2)")
    assertTypeChecks("val r: Quantity[Dimensionless] = metres(4) / metres(2)")
  }

  // A guard: it holds against the stub too, where every computed unit is
  // Nothing. It catches a `*` whose unit is too wide to add to anything.
  test("a computed unit still only adds to the same unit") {
    assertTypeChecks("(metres(1) * metres(1)) + (metres(2) * metres(3))")
    assertTypeError("(metres(1) * metres(1)) + metres(1)")
  }

  test("the numbers multiply and divide") {
    assertEquals((metres(3) * seconds(4)).value, 12.0)
    assertEquals((metres(10) / seconds(4)).value, 2.5)
    assertEquals((kilograms(1) / seconds(0)).value, Double.PositiveInfinity)
  }
end Exercise01Spec
