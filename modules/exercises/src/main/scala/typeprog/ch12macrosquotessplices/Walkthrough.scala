package typeprog.ch12macrosquotessplices

import WalkthroughMacros.*

/** Chapter 12 — macros: quotes & splices.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it. The macros it calls are in
  * `WalkthroughMacros.scala`, next to it — a macro cannot be called from the
  * file that defines it.
  *
  * The chapter covers four things, in order:
  *
  *   1. quotes and splices — code as a value, and back;
  *   2. values in and out, and errors at the caller's position;
  *   3. matching on code, and what the macro actually receives;
  *   4. reflection — the compiler's view of types.
  *
  * Prose version, with the motivation, the pitfalls, and when not to write a
  * macro at all: `docs/theory/ch12-macros-quotes-splices.md`.
  */
object Walkthrough:

  val y = 5

  // ---------------------------------------------------------------------------
  // 1. Quotes and splices
  // ---------------------------------------------------------------------------

  // `showCode` receives the argument as code and returns it as text — the
  // typed tree, printed.
  val shownSum: String = showCode(y + 2) // "typeprog.ch12macrosquotessplices.Walkthrough.y.+(2)"

  // `twice` builds `$a + $a`: the argument's code, inserted twice.
  val doubled: Int = twice(y) // 10

  // ---------------------------------------------------------------------------
  // 2. Values in and out
  //
  // `valueOrAbort` takes a literal's value out of its expression; the
  // implementation decides, while compiling, and `report.errorAndAbort` puts
  // the error on the caller's argument.
  // ---------------------------------------------------------------------------

  val three: Int = positive(3)

  //   positive(-1)
  //   // expected a positive literal
  //
  //   positive(y)
  //   // Expected a known value.
  //   //
  //   // The value of: typeprog.ch12macrosquotessplices.Walkthrough.y
  //   // could not be extracted using scala.quoted.FromExpr$PrimitiveFromExpr@…

  // ---------------------------------------------------------------------------
  // 3. Matching on code
  //
  // A quote as a pattern: the shape `$x * 1`, binding `x`.
  // ---------------------------------------------------------------------------

  val simplified: Int = simplify(y * 1) // compiles to `y`
  val untouched: Int = simplify(y * 2) // left as it is

  // The macro receives the *typed* tree. Two literals added together have
  // been folded already; a variable has not.
  val literalSum: Boolean = isSum(1 + 2) // false: the macro sees `3`
  val variableSum: Boolean = isSum(y + 2) // true

  // The source text is still the text that was written.
  val folded: String = sourceAndShow(1 + 2) // "1 + 2 | 3"
  val unfolded: String = sourceAndShow(y + 2)
  // "y + 2 | typeprog.ch12macrosquotessplices.Walkthrough.y.+(2)"

  // ---------------------------------------------------------------------------
  // 4. Reflection
  // ---------------------------------------------------------------------------

  final case class Point(x: Int, y: Int)
  final case class Tagged[A](value: A, tags: List[String])

  val pointIsCase: Boolean = isCaseClass[Point] // true
  val intIsCase: Boolean = isCaseClass[Int] // false

  // `show` prints types in full; the short printer does not.
  val stringNames: (String, String) = typeNames[String] // ("scala.Predef.String", "String")

  // A field's type asked of the type, not of the symbol: the class's `A` comes
  // out as the argument `T` was given.
  val taggedFields: List[String] = fieldTypes[Tagged[Int]] // List("Int", "List[String]")

end Walkthrough
