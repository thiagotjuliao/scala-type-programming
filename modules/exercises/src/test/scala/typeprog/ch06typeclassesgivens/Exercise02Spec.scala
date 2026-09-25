package typeprog.ch06typeclassesgivens

import typeprog.core.TypeLevelSuite

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  private val v190 = Version(1, 9, 0)
  private val v1100 = Version(1, 10, 0)
  private val v200 = Version(2, 0, 0)
  private val v0999 = Version(0, 9, 99)

  // Guards: the stub already has an instance in the companion. They catch a
  // solution that moves it somewhere an import is needed, or that adds a
  // second one beside it.
  test("an Ordering[Version] is found with no import") {
    assertTypeChecks("summon[Ordering[Version]]")
    assertTypeChecks("List(Version(1, 0, 0)).sorted")
  }

  // Also a guard, and the reason the companion is the right home: the
  // standard library's rules for options and tuples find it as a premise.
  test("the implicit scope includes the type arguments: options and pairs of versions") {
    assertTypeChecks("summon[Ordering[Option[Version]]]")
    assertTypeChecks("summon[Ordering[(Version, String)]]")
  }

  test("versions are ordered by number, not by spelling") {
    assertEquals(List(v1100, v190).sorted, List(v190, v1100))
    assertEquals(List(v200, v0999, v1100, v190).sorted, List(v0999, v190, v1100, v200))
    assertEquals(List(v190, v1100, v0999).max, v1100)
  }

  // A guard: spellings compare equal too. It catches a hand-written compare
  // that never returns zero.
  test("an equal version compares as equal") {
    assertEquals(summon[Ordering[Version]].compare(Version(1, 2, 3), Version(1, 2, 3)), 0)
  }

  test("importing NewestFirst reverses the order, in that scope only") {
    locally {
      import Version.NewestFirst.given
      assertEquals(List(v190, v200, v1100).sorted, List(v200, v1100, v190))
    }
    assertEquals(List(v190, v200, v1100).sorted, List(v190, v1100, v200))
  }

  // What the solution composes from: the standard library's instances for
  // options and tuples, applied to this one.
  test("instances built from it follow it") {
    assertEquals(
      List(Option(v1100), None, Option(v190)).sorted,
      List(None, Option(v190), Option(v1100))
    )
    assertEquals(List((v1100, "b"), (v190, "a")).sorted, List((v190, "a"), (v1100, "b")))
  }
end Exercise02Spec
