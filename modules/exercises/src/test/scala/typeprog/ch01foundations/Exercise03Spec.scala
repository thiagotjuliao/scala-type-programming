package typeprog.ch01foundations

import typeprog.core.TypeLevelSuite

class Exercise03Spec extends TypeLevelSuite:

  test("coerce carries a value across an identity witness") {
    assertEquals(Exercise03.coerce[Int, Int](42), 42)
  }

  test("upcast widens without a cast") {
    val rex: Dog = Dog("Rex")
    assertEquals(Exercise03.upcast[Dog, Animal](rex).name, "Rex")
  }

  test("sum reads the element type through the witness") {
    assertEquals(Exercise03.sum(List(1, 2, 3)), 6)
    assertEquals(Exercise03.sum(List.empty[Int]), 0)
  }

  test("no witness, no call") {
    assertTypeError("Exercise03.coerce[Int, String](42)")
    assertTypeError("Exercise03.upcast[Animal, Dog](Dog(\"Rex\"))")
    assertTypeError("""Exercise03.sum(List("a", "b"))""")
  }

  test("=:= is available in both directions, <:< in only one") {
    assertTypeChecks("Exercise03.coerce[Dog, Dog](Dog(\"Rex\"))")
    assertTypeChecks("Exercise03.upcast[Dog, Animal](Dog(\"Rex\"))")
  }
end Exercise03Spec
