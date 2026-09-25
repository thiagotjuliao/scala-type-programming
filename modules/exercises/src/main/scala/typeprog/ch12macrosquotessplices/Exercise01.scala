package typeprog.ch12macrosquotessplices

import scala.quoted.*

/** Exercise 01 — an expression, and what it was.
  *
  * `debug(expr)` is the expression's source text, exactly as it was written,
  * followed by `" = "` and its value:
  *
  *   - `debug(x * 2 + 2)`, with `x` being `20`, is `"x * 2 + 2 = 42"`;
  *   - the text is the source as written, not the compiler's rendering of it,
  *     which would be `x.*(2).+(2)`;
  *   - any type of value works: `debug(name.toUpperCase)` is
  *     `"name.toUpperCase = ANN"`.
  *
  * The expression is evaluated once, when the program runs; its text is taken
  * while compiling.
  *
  * The implementation must not throw: it runs inside the compiler. As shipped
  * it returns `'{ ??? }`, code that throws when *it* runs.
  *
  * Hint: an argument expression, seen through `quotes.reflect`, has a position,
  * and a position knows its source code. A quote can splice in both a value
  * computed now and the expression itself.
  */
object Exercise01:

  inline def debug[A](inline a: A): String = ${ debugImpl('a) }

  /** TODO: the source text, `" = "`, and the value. */
  def debugImpl[A: Type](a: Expr[A])(using Quotes): Expr[String] = '{ ??? }
