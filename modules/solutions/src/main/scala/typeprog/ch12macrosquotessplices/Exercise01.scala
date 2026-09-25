package typeprog.ch12macrosquotessplices

import scala.quoted.*

/** Exercise 01 — an expression, and what it was. *Solved.*
  *
  * `'a` is the argument as code, an `Expr[A]`. Two things are taken from it.
  * Its source text, while compiling: through `quotes.reflect`, the expression
  * is a `Term` with a position, and `pos.sourceCode` is the text between the
  * position's start and end, as the caller typed it. And its
  * value, at runtime: spliced back into the quote as `$a`, the expression is
  * simply evaluated where `debug` was called.
  *
  * `Expr(text)` lifts the string computed now into the program; the quote
  * `'{ $text + " = " + $a }` is the code left in place of the call.
  *
  * `a.show` is the tempting alternative for the text, and it is not the text:
  * it prints the typed tree, so `x * 2 + 2` comes out as `x.*(2).+(2)`.
  *
  * The position covers the tree, and parentheses are not part of the tree: for
  * `x * (1 + 1)` the text is `x * (1 + 1` — the closing parenthesis is past
  * the last argument's end. And the text is what was written *after*
  * formatting: this repository's scalafmt rewrites `x+1` as `x + 1` in the
  * spec. The spec's examples avoid both.
  */
object Exercise01:

  inline def debug[A](inline a: A): String = ${ debugImpl('a) }

  def debugImpl[A: Type](a: Expr[A])(using Quotes): Expr[String] =
    import quotes.reflect.*
    val text = Expr(a.asTerm.pos.sourceCode.getOrElse(a.show))
    '{ $text + " = " + $a }
