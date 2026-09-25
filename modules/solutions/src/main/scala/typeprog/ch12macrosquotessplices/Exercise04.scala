package typeprog.ch12macrosquotessplices

import scala.quoted.*

/** Exercise 04 — a case class, as the compiler sees it. *Solved.*
  *
  * `TypeRepr.of[T]` is the compiler's representation of `T`, and its
  * `typeSymbol` is the class's definition. The symbol's flags say whether it
  * is a case class; its `caseFields` are the constructor's fields, in order.
  * Each field's type is `memberType` of `T` — asked of the type, not of the
  * symbol, so that a field declared as `List[A]` in a generic class would come
  * out with `T`'s own arguments.
  *
  * `show` prints a type in full, `scala.Predef.String`; the
  * `Printer.TypeReprShortCode` printer prints `String`, and keeps type
  * arguments: `List[Int]`.
  *
  * A `Mirror` (chapter 10) has the field names and types too, but only as
  * types to compute with: there is no way to turn `MirroredElemTypes` into the
  * text `List[Int]` without the compiler's help. That — reading the program
  * rather than computing with it — is where a macro is the right tool.
  */
object Exercise04:

  inline def describe[T]: String = ${ describeImpl[T] }

  def describeImpl[T: Type](using Quotes): Expr[String] =
    import quotes.reflect.*
    val tpe = TypeRepr.of[T]
    val symbol = tpe.typeSymbol
    if !symbol.flags.is(Flags.Case) then
      report.errorAndAbort(s"${Type.show[T]} is not a case class")
    val fields = symbol.caseFields.map: field =>
      s"${field.name}: ${tpe.memberType(field).show(using Printer.TypeReprShortCode)}"
    Expr(fields.mkString(s"${symbol.name}(", ", ", ")"))
