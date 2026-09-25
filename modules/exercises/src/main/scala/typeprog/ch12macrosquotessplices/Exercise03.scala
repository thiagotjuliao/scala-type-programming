package typeprog.ch12macrosquotessplices

import scala.quoted.*

/** Exercise 03 — the name of a field, not a string.
  *
  * Code that needs a field's name — for an error message, a column, a JSON key
  * — usually writes it as a string, and a rename leaves the string behind.
  * `nameOf` takes it from the code instead:
  *
  *   - `nameOf(person.age)` is `"age"`;
  *   - `nameOf(person)` is `"person"`;
  *   - in a chain, the last name: `nameOf(person.name.length)` is `"length"`;
  *   - anything that is not a name or a selection — `nameOf(1 + 2)`,
  *     `nameOf("age")` — does not compile, and the error says it needs
  *     `a name`.
  *
  * Nothing is evaluated: the result is a constant, computed while compiling.
  *
  * Hint: `quotes.reflect` shows an argument as a tree. A selection and a plain
  * name are two kinds of node, each with a name; the argument may arrive
  * wrapped in a node that records where it was inlined from.
  */
object Exercise03:

  inline def nameOf(inline a: Any): String = ${ nameOfImpl('a) }

  /** TODO: the name, or a compile error. */
  def nameOfImpl(a: Expr[Any])(using Quotes): Expr[String] = '{ ??? }
