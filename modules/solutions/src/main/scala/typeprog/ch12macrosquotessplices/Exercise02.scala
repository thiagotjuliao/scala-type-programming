package typeprog.ch12macrosquotessplices

import java.util.regex.{Pattern, PatternSyntaxException}

import scala.quoted.*
import scala.util.matching.Regex

/** Exercise 02 — a regular expression checked while compiling. *Solved.*
  *
  * `pattern.value` is the string when the argument is a literal, and `None`
  * otherwise — a variable's value does not exist yet. `valueOrAbort` would do
  * the same with the compiler's generic *"Expected a known value"*; matching
  * on the `Option` lets the error say what the caller should write.
  *
  * With the string in hand, the macro runs ordinary code on it:
  * `Pattern.compile` throws `PatternSyntaxException` for an invalid pattern,
  * here, inside the compiler. The exception is turned into
  * `report.errorAndAbort`, positioned at the argument, so the error appears in
  * the caller's code with Java's own description of the problem. Letting the
  * exception escape would also stop the compilation — as *"Exception occurred
  * while executing macro expansion"*, pointing at the macro rather than at the
  * pattern.
  *
  * For a valid pattern the expansion is `"[a-z]+".r`: the check leaves nothing
  * behind at runtime.
  */
object Exercise02:

  inline def regex(inline pattern: String): Regex = ${ regexImpl('pattern) }

  def regexImpl(pattern: Expr[String])(using Quotes): Expr[Regex] =
    import quotes.reflect.*
    pattern.value match
      case None =>
        report.errorAndAbort("regex needs a string literal, to check it while compiling", pattern)
      case Some(p) =>
        try Pattern.compile(p)
        catch
          case e: PatternSyntaxException =>
            report.errorAndAbort(
              s"invalid regex: ${e.getDescription} near index ${e.getIndex}",
              pattern
            )
        '{ ${ Expr(p) }.r }
