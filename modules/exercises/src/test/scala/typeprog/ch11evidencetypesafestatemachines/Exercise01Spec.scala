package typeprog.ch11evidencetypesafestatemachines

import typeprog.core.TypeLevelSuite

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  test("a stack of numbers can be summed") {
    assertEquals(Stack(List(1, 2, 3)).sum, 6)
    assertEquals(Stack(List(1.5, 2.5)).sum, 4.0)
    assertEquals(Stack(List.empty[Int]).sum, 0)
  }

  test("a stack of anything else cannot") {
    assertTypeError("""Stack(List("a", "b")).sum""")
  }

  test("a stack of stacks flattens into one stack, in order") {
    assertEquals(Stack(List(Stack(List(1, 2)), Stack(List(3)))).flatten.items, List(1, 2, 3))
  }

  test("a stack of anything else does not flatten") {
    assertTypeError("Stack(List(1, 2)).flatten")
  }

  test("a stack of pairs unzips into two stacks") {
    val (numbers, letters) = Stack(List(1 -> "a", 2 -> "b")).unzip
    assertEquals(numbers.items, List(1, 2))
    assertEquals(letters.items, List("a", "b"))
  }

  test("a stack of anything else does not unzip") {
    assertTypeError("Stack(List(1, 2)).unzip")
  }
end Exercise01Spec
