package typeprog.ch01foundations

import typeprog.core.{TypeLevelSuite, evidence}

class Exercise02Spec extends TypeLevelSuite:

  test("a refinement is a subtype of the trait it refines") {
    assertTypeChecks("evidence.subtypeOf[Exercise02.DogRepo, Exercise02.Repo]")
  }

  test("find takes a Long and returns dogs") {
    assertTypeChecks(
      "def probe(r: Exercise02.DogRepo): Option[Dog] = r.find(1L)"
    )
  }

  test("the unrefined trait offers no usable find") {
    assertTypeError(
      "def probe(r: Exercise02.Repo): Option[Dog] = r.find(1L)"
    )
  }

  test("the refined Id is Long, not anything convertible to one") {
    assertTypeError(
      """def probe(r: Exercise02.DogRepo): Option[Dog] = r.find("1")"""
    )
  }

  test("the refined Entity is Dog, not Animal") {
    assertTypeError(
      "def probe(r: Exercise02.DogRepo): Option[Cat] = r.find(1L)"
    )
  }
end Exercise02Spec
