package typeprog.ch05opaquetypes

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise04.*

class Exercise04Spec extends TypeLevelSuite:

  test("a Double is not a Quantity, and a Quantity is not a Double") {
    assertTypeError("val d: Quantity[Meters] = 3.0")
    assertTypeError("val x: Double = Quantity.of[Meters](3.0)")
    assertTypeError("evidence.sameType[Quantity[Meters], Double]")
  }

  test("quantities of the same unit add up, and keep the unit") {
    assertTypeChecks(
      "val d: Quantity[Meters] = Quantity.of[Meters](1.0) + Quantity.of[Meters](2.0)"
    )
  }

  test("quantities of different units do not add up") {
    assertTypeError("Quantity.of[Meters](1.0) + Quantity.of[Seconds](2.0)")
  }

  test("different units are different types") {
    assertTypeError("evidence.sameType[Quantity[Meters], Quantity[Seconds]]")
  }

  test("dividing one quantity by another makes the unit of the ratio") {
    assertTypeChecks(
      "val v: Quantity[Meters / Seconds] = Quantity.of[Meters](10.0).per(Quantity.of[Seconds](2.0))"
    )
  }

  test("the ratio is not the other way round") {
    assertTypeError(
      "val v: Quantity[Seconds / Meters] = Quantity.of[Meters](10.0).per(Quantity.of[Seconds](2.0))"
    )
  }

  test("scaling keeps the unit") {
    assertTypeChecks("val d: Quantity[Meters] = Quantity.of[Meters](1.0).times(3.0)")
  }

  // `value` exists on the stub too, so these compile there and fail on `???`.
  test("the arithmetic is the arithmetic of the numbers") {
    assertEquals((Quantity.of[Meters](1.0) + Quantity.of[Meters](2.0)).value, 3.0)
    assertEquals(Quantity.of[Meters](10.0).per(Quantity.of[Seconds](2.0)).value, 5.0)
    assertEquals(Quantity.of[Meters](1.5).times(2.0).value, 3.0)
  }
end Exercise04Spec
