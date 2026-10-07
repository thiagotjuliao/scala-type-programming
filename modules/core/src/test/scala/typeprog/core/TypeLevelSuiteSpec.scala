package typeprog.core

/** Tests the test helpers.
  *
  * These assertions are the instrument every other chapter is measured with,
  * and an instrument that silently passes everything would make the whole
  * repository look solved. The fixtures below are deliberately trivial and
  * deliberately include the failing direction.
  */
class TypeLevelSuiteSpec extends TypeLevelSuite:

  test("a true claim about types compiles") {
    assertTypeChecks("summon[Int =:= Int]")
    assertTypeChecks("evidence.subtypeOf[List[Int], Seq[Int]]")
  }

  test("a false claim about types is rejected") {
    assertTypeError("summon[Int =:= String]")
    assertTypeError("evidence.subtypeOf[Seq[Int], List[Int]]")
  }

  test("Unsolved satisfies nothing") {
    assertTypeError("summon[Unsolved =:= Int]")
    assertTypeError("summon[Unsolved =:= Nothing]")
  }

  test("a rejection can be pinned to its reason") {
    assertTypeErrorContains("val n: Int = \"not a number\"", "Found:")
  }

  // `rejectedAtTheCallSite` fails where it is called, so the echoed source line
  // is the assertion's own line, which spells out the fragment.
  test("a reason only the echoed source line mentions is the wrong reason") {
    intercept[munit.FailException](
      assertTypeErrorContains("rejectedAtTheCallSite()", "a reason nobody gave")
    )
  }

  test("a rejection can be pinned to how its message opens") {
    assertTypeErrorStartsWith("val n: Int = \"not a number\"", "Found:")
  }

  test("text further into the message does not open it") {
    intercept[munit.FailException](
      assertTypeErrorStartsWith("val n: Int = \"not a number\"", "Required:")
    )
  }
end TypeLevelSuiteSpec

inline def rejectedAtTheCallSite(): Int = scala.compiletime.error("rejected")
