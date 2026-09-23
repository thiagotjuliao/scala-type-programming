package typeprog.ch02higherkinds

import typeprog.core.TypeLevelSuite

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  test("Mappable takes a one-hole type constructor") {
    assertTypeChecks("def probe(m: Mappable[List], n: Mappable[Option]) = ()")
  }

  test("Mappable rejects a proper type and a two-hole constructor") {
    assertTypeError("def probe(m: Mappable[Int]) = ()")
    assertTypeError("def probe(m: Mappable[Either]) = ()")
  }

  test("Mappable.map changes the element type and keeps the constructor") {
    assertTypeChecks(
      "def probe(m: Mappable[List]): List[String] = m.map(List(1, 2))(_.toString)"
    )
  }

  test("BiMappable takes a two-hole type constructor") {
    assertTypeChecks("def probe(m: BiMappable[Either], n: BiMappable[Tuple2]) = ()")
  }

  test("BiMappable rejects a one-hole constructor") {
    assertTypeError("def probe(m: BiMappable[List]) = ()")
  }

  test("BiMappable.bimap changes both element types independently") {
    // The input is `Either[String, Int]` and the output `Either[Int, String]`:
    // both sides change type, and each function only sees its own side.
    assertTypeChecks(
      """def probe(m: BiMappable[Either], e: Either[String, Int]): Either[Int, String] =
           m.bimap(e)(_.length, _.toString)"""
    )
  }

  test("Instances takes something that itself takes a type constructor") {
    assertTypeChecks("def probe(i: Instances[Mappable]): Mappable[List] = i.forList")
  }

  test("Instances rejects a type constructor over proper types") {
    assertTypeError("def probe(i: Instances[List]) = ()")
    assertTypeError("def probe(i: Instances[Int]) = ()")
  }
end Exercise01Spec
