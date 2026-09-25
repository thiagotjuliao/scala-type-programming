package typeprog.ch07matchtypes

import typeprog.core.TypeLevelSuite

import Exercise04.*

class Exercise04Spec extends TypeLevelSuite:

  val distance: Quantity[Meters] = Quantity.of[Meters](100)
  val time: Quantity[Seconds] = Quantity.of[Seconds](20)
  val mass: Quantity[Kilograms] = Quantity.of[Kilograms](2)
  val speed: Quantity[Meters / Seconds] = distance.per(time)

  test("a speed times a time is a distance") {
    assertTypeChecks("val d: Quantity[Meters] = speed * time")
    assertTypeError("val d: Quantity[Seconds] = speed * time")
    assertTypeError("val d: Quantity[Meters / Seconds * Seconds] = speed * time")
  }

  test("the cancelling works from either side") {
    assertTypeChecks("val d: Quantity[Meters] = time * speed")
  }

  test("units that do not cancel make a product") {
    assertTypeChecks("val a: Quantity[Meters * Seconds] = distance * time")
    assertTypeError("val a: Quantity[Meters] = distance * time")
    assertTypeChecks("val p: Quantity[Kilograms * (Meters / Seconds)] = mass * speed")
  }

  // A ratio next to a unit it does not divide by: nothing cancels, and a
  // solution that cancels whenever it sees a ratio fails here.
  test("only the unit divided by cancels") {
    assertTypeChecks("val q: Quantity[(Meters / Seconds) * Meters] = speed * distance")
    assertTypeError("val q: Quantity[Meters] = speed * distance")
    assertTypeError("val q: Quantity[Seconds] = speed * distance")
  }

  // Guards: the chapter 05 behaviour `*` is built next to must survive.
  test("addition still needs the same unit") {
    assertTypeChecks("distance + distance")
    assertTypeError("distance + time")
  }

  // `*` compiles against the stub — its type is `Quantity[Times[U, V]]` whatever
  // `Times` is — and `amount` is a `Double`, so these run; against `???` they
  // fail, which is a red test.
  test("the amounts multiply") {
    assertEqualsDouble((speed * time).amount, 100.0, 1e-9)
    assertEqualsDouble((distance * time).amount, 2000.0, 1e-9)
  }
end Exercise04Spec
