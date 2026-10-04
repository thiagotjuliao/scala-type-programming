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
  *   - a generic case class is named as the type it was given:
  *     `describe[Wrap[Int]]` is `"Wrap[Int](a: Int)"`, and a type parameter no
  *     field mentions still tells `Id[User]` from `Id[Order]`;
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
  def describeImpl[T: Type](using Quotes): Expr[String] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    val sym = tpe.typeSymbol

    val name = tpe.show(using Printer.TypeReprShortCode)

    if !sym.flags.is(Flags.Case) then report.errorAndAbort(s"$name is not a case class")

    val fields = sym.caseFields.map: f =>
      val ftpe = tpe.memberType(f)
      val fieldName = f.name
      val typeName = ftpe.show(using Printer.TypeReprShortCode)
      s"$fieldName: $typeName"

    val result = fields.mkString(s"$name(", ", ", ")")
    Expr(result)
