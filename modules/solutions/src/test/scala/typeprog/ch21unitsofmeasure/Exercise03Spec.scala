package typeprog.ch21unitsofmeasure

import typeprog.core.TypeLevelSuite

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  test("the units are built from the ones before them") {
    assertTypeChecks("summon[Velocity =:= Div[Metre, Second]]")
    assertTypeChecks("summon[Acceleration =:= Div[Velocity, Second]]")
    assertTypeChecks("summon[Energy =:= Mul[Kilogram, Mul[Velocity, Velocity]]]")
  }

  test("and they are the units of physics") {
    assertTypeChecks("summon[Velocity =:= (1, -1, 0)]")
    assertTypeChecks("summon[Acceleration =:= (1, -2, 0)]")
    assertTypeChecks("summon[Energy =:= (2, -2, 1)]")
  }

  // Guard: an energy is not a momentum.
  test("an energy is not a mass times a speed") {
    assertTypeError("summon[Energy =:= Mul[Kilogram, Velocity]]")
  }

  test("the fall time formula checks, and its wrong version does not") {
    assertTypeChecks(
      "val t: Quantity[Second] = (metres(1).scale(2) / metresPerSecondSquared(9.8)).sqrt"
    )
    assertTypeError("val t: Quantity[Second] = (metres(1) * metresPerSecondSquared(9.8)).sqrt")
  }

  test("speed is distance over time") {
    assertEquals(speed(metres(100), seconds(8)).value, 12.5)
    assertEquals(speed(metres(0), seconds(1)).value, 0.0)
  }

  test("kinetic energy is half the mass times the speed squared") {
    assertEquals(kineticEnergy(kilograms(2), metresPerSecond(3)).value, 9.0)
    assertEquals(kineticEnergy(kilograms(5), metresPerSecond(0)).value, 0.0)
  }

  test("a fall from 19.6 m under 9.8 m/s² takes two seconds") {
    assertEqualsDouble(fallTime(metres(19.6), metresPerSecondSquared(9.8)).value, 2.0, 1e-9)
    assertEquals(fallTime(metres(0), metresPerSecondSquared(9.8)).value, 0.0)
  }
end Exercise03Spec
