package typeprog.ch12macrosquotessplices

import scala.quoted.*

/** Exercise 04 — a case class, as the compiler sees it.
  *
  * `describe[T]` renders a case class from its type alone:
  *
  *   - `describe[Person]` is `"Person(name: String, age: Int)"` — the field
  *     types as short names, not `scala.Predef.String`;
  *   - type arguments are kept: `"Box(items: List[Int], label:
  *     Option[String])"`;
  *   - a case class with no fields is `"Empty()"`;
  *   - anything that is not a case class — `Int`, a plain class — does not
  *     compile, and the error says `not a case class`.
  *
  * Chapter 10 read a case class's shape from its `Mirror`, which has the field
  * names but no way to print a type. This reads it from the compiler itself.
  *
  * As shipped, the implementation returns `'{ ??? }`.
  *
  * Hint: `TypeRepr.of[T]` is the type; its symbol knows whether it is a case
  * class, and which fields it has; each field's type is a member type of `T`.
  * Types print through a `Printer`.
  */
object Exercise04:

  inline def describe[T]: String = ${ describeImpl[T] }

  /** TODO: the description, or a compile error. */
  def describeImpl[T: Type](using Quotes): Expr[String] = '{ ??? }
