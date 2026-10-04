package typeprog.ch12macrosquotessplices

import scala.quoted.*
import scala.util.matching.Regex
import scala.util.Success
import scala.util.Failure

/** Exercise 02 — a regular expression checked while compiling.
  *
  * `regex("[a-z]+")` is a `Regex`, like `"[a-z]+".r`, with one difference: the
  * pattern is checked while compiling.
  *
  *   - a valid pattern is a working `Regex`;
  *   - an invalid one — `regex("[a-z")` — does not compile, and the error says
  *     `invalid regex`, followed by what is wrong with it;
  *   - a string that is not a literal cannot be checked, so it does not
  *     compile either, and the error says the argument must be a `literal`.
  *
  * As shipped, the implementation returns `'{ ??? }`.
  *
  * Hint: the literal's value can be taken out of its expression, and
  * `java.util.regex.Pattern.compile` can run in the compiler like any other
  * code. The error is reported at the argument, with a message built there.
  */
object Exercise02:

  inline def regex(inline pattern: String): Regex = ${ regexImpl('pattern) }

  /** TODO: the checked `Regex`, or a compile error. */
  def regexImpl(pattern: Expr[String])(using Quotes): Expr[Regex] =
    import quotes.reflect.*

    val value =
      pattern.value.getOrElse(report.errorAndAbort("argument must be a literal"))

    scala.util.Try(java.util.regex.Pattern.compile(value)) match
      case Success(_) =>
        val expr = Expr(value)
        '{ $expr.r }
      case Failure(e) => report.errorAndAbort("invalid regex: " + e.getMessage())
