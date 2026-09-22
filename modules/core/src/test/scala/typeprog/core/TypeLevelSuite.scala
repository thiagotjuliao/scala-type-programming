package typeprog.core

import munit.{FunSuite, Location}

/** The base class for every chapter's specs.
  *
  * It adds three assertions that talk about *compilation* instead of values.
  * Each takes a snippet of Scala source, type-checks it where the call sits —
  * so everything in scope at the call site is in scope in the snippet, imports
  * included — and reports the outcome as an ordinary test result.
  *
  * The indirection through a string is what keeps a wrong answer from
  * escalating into a build failure. `summon[Flip[(Int, String)] =:= (String,
  * Int)]` written as code either compiles or takes the module down; written as
  * a snippet it is just a test that passes or fails.
  *
  * The snippet must be a literal, or an `inline` value that folds to one: it
  * has to exist at compile time, because that is when it is checked.
  */
abstract class TypeLevelSuite extends FunSuite:

  /** Asserts that the snippet compiles. The workhorse: a positive claim about
    * a type is a snippet that only type-checks when the claim holds.
    */
  inline def assertTypeChecks(inline code: String)(using Location): Unit =
    val errors = compileErrors(code)
    if errors.nonEmpty then
      fail(s"expected this to compile:\n  $code\n\nbut the compiler said:\n$errors")

  /** Asserts that the snippet does *not* compile.
    *
    * Half of what a type-level design is worth lies here. A phantom-typed
    * builder that accepts the right call order is only interesting if it also
    * rejects the wrong one, and a test that never checks the rejection cannot
    * tell a precise type from `Any`.
    */
  inline def assertTypeError(inline code: String)(using Location): Unit =
    val errors = compileErrors(code)
    if errors.isEmpty then fail(s"expected this to be rejected, but it compiled:\n  $code")

  /** Asserts that the snippet is rejected *for the stated reason*.
    *
    * Stricter than [[assertTypeError]], and the right assertion whenever the
    * error message is itself part of the design — a `compiletime.error` in an
    * inline method, say, whose whole purpose is the sentence it prints.
    */
  inline def assertTypeErrorContains(inline code: String, inline fragment: String)(using
      Location
  ): Unit =
    val errors = compileErrors(code)
    if errors.isEmpty then fail(s"expected this to be rejected, but it compiled:\n  $code")
    else if !errors.contains(fragment) then
      fail(
        s"the snippet was rejected, but not for the expected reason.\n" +
          s"expected the error to mention: $fragment\ngot:\n$errors"
      )
end TypeLevelSuite
