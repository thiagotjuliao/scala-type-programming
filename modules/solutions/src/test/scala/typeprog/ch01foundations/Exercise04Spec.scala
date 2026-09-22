package typeprog.ch01foundations

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise04.Stack

class Exercise04Spec extends TypeLevelSuite:

  test("a stack of dogs is a stack of animals") {
    assertTypeChecks("evidence.subtypeOf[Stack[Dog], Stack[Animal]]")
  }

  test("covariance does not run the other way") {
    assertTypeError("evidence.subtypeOf[Stack[Animal], Stack[Dog]]")
  }

  test("the empty stack serves every element type") {
    assertTypeChecks("val s: Stack[Dog] = Stack.empty")
    assertTypeChecks("val s: Stack[Int] = Stack.empty")
    // The two above hold of a `def empty[A]` too — inference just picks `A`.
    // This one does not: only a *value* has a singleton type to name.
    assertTypeChecks("evidence.subtypeOf[Stack.empty.type, Stack[Nothing]]")
  }

  // The element type is fixed by the parameter before the push happens, which
  // is what makes this discriminating. Written as a chain off `Stack.empty`,
  // inference is free to pick `Animal` for the whole chain up front and an
  // invariant stack swallows both pushes — the assertion would then pass
  // without the exercise being solved.
  test("pushing a wider element widens the stack") {
    assertTypeChecks(
      """def widened(dogs: Stack[Dog]): Stack[Animal] = dogs.push(Cat("Tom"))"""
    )
  }

  test("the widened stack is not silently narrowed back") {
    assertTypeError(
      """val s: Stack[Dog] = Stack.empty.push(Dog("Rex")).push(Cat("Tom"))"""
    )
  }

  test("peek returns the element pushed last") {
    assertEquals(Stack.empty.push(1).push(2).peek, Some(2))
    assertEquals(Stack.empty.peek, None)
  }

  // Note the element types: this test has to compile against the *unsolved*
  // stack too, so it pushes two dogs rather than a dog and a cat. Every claim
  // that only holds once the exercise is solved goes through a snippet above.
  test("pushing does not mutate the stack it was pushed onto") {
    val one = Stack.empty.push(Dog("Rex"))
    val two = one.push(Dog("Fido"))
    assertEquals(one.size, 1)
    assertEquals(two.size, 2)
  }
end Exercise04Spec
