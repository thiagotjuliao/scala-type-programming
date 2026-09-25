package typeprog.ch09tuplesashlists

import typeprog.core.TypeLevelSuite

import Exercise04.*

class Exercise04Spec extends TypeLevelSuite:

  // A guard: the empty vector is shipped with its length.
  test("the empty vector has length 0") {
    assertTypeChecks("val e: Vec[0, String] = Vec.empty[String]")
  }

  test("prepending adds one to the length") {
    assertTypeChecks("val v: Vec[2, Int] = 1 :: 2 :: Vec.empty[Int]")
    assertTypeError("val v: Vec[0, Int] = 1 :: 2 :: Vec.empty[Int]")
  }

  test("appending adds the lengths") {
    assertTypeChecks(
      "val w: Vec[5, Int] = (1 :: 2 :: Vec.empty[Int]) ++ (3 :: 4 :: 5 :: Vec.empty[Int])"
    )
    assertTypeError(
      "val w: Vec[2, Int] = (1 :: 2 :: Vec.empty[Int]) ++ (3 :: 4 :: 5 :: Vec.empty[Int])"
    )
  }

  test("the tail is one shorter") {
    assertTypeChecks("val t: Vec[1, Int] = (1 :: 2 :: Vec.empty[Int]).tail")
  }

  // A guard: the stub's `head` and `tail` exist everywhere, so this passes
  // against it. It catches a solution whose evidence rejects too much.
  test("head and tail exist on a non-empty vector") {
    assertTypeChecks("(1 :: Vec.empty[Int]).head")
    assertTypeChecks("(1 :: Vec.empty[Int]).tail")
  }

  test("head and tail do not compile on an empty vector") {
    assertTypeError("Vec.empty[Int].head")
    assertTypeError("Vec.empty[Int].tail")
    assertTypeError("(1 :: Vec.empty[Int]).tail.head")
  }

  // These compile against the stub — its methods exist, with the wrong types —
  // and against `???` they fail, which is a red test.
  test("the elements are the ones put in, in order") {
    assertEquals((1 :: 2 :: Vec.empty[Int]).items, Vector(1, 2))
    assertEquals(((1 :: Vec.empty[Int]) ++ (2 :: 3 :: Vec.empty[Int])).items, Vector(1, 2, 3))
    assertEquals((1 :: 2 :: Vec.empty[Int]).head, 1)
    assertEquals((1 :: 2 :: Vec.empty[Int]).tail.items, Vector(2))
  }
end Exercise04Spec
