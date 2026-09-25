package typeprog.ch12macrosquotessplices

import scala.quoted.*

/** Exercise 03 — the name of a field, not a string. *Solved.*
  *
  * Through `quotes.reflect` the argument is a `Term`, and the two shapes that
  * have a name are `Select(qualifier, name)` — `person.age` — and `Ident(name)`
  * — `person`. For a chain, `person.name.length` is a `Select` whose qualifier
  * is another `Select`; the outermost one is the last name written.
  *
  * An `inline` argument arrives wrapped in `Inlined` nodes, which record where
  * the code was inlined from; they are unwrapped first. A call with an empty
  * argument list is unwrapped too: `String.length` is a Java method declared
  * with parentheses, so `person.name.length` is `Apply(Select(…, length),
  * Nil)` — written without the parentheses, and called with them. Anything
  * else — `"age"`, or `1 + 2`, which is folded to the literal `3` before the
  * macro sees it — is rejected with `report.errorAndAbort`, at the argument.
  *
  * The result is `Expr(name)`, a string literal: nothing of the argument is
  * evaluated, and a rename of the field changes the string with it, because
  * the string is computed from the code.
  */
object Exercise03:

  inline def nameOf(inline a: Any): String = ${ nameOfImpl('a) }

  def nameOfImpl(a: Expr[Any])(using Quotes): Expr[String] =
    import quotes.reflect.*

    def nameIn(term: Term): String = term match
      case Inlined(_, _, inner) => nameIn(inner)
      case Apply(inner, Nil) => nameIn(inner)
      case Select(_, name) => name
      case Ident(name) => name
      case other =>
        report.errorAndAbort(s"nameOf needs a name or a selection like a.b, not ${other.show}", a)

    Expr(nameIn(a.asTerm))
