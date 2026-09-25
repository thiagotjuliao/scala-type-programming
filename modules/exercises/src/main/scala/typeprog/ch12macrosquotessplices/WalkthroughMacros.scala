package typeprog.ch12macrosquotessplices

import scala.quoted.*

/** The macros `Walkthrough.scala` calls. They live in their own file because a
  * macro cannot be called from the file that defines it — the implementation
  * has to be compiled before any call is expanded.
  */
object WalkthroughMacros:

  // ---------------------------------------------------------------------------
  // Quotes and splices: `'a` is the argument as code, `${ … }` runs now.
  // ---------------------------------------------------------------------------

  inline def showCode(inline a: Any): String = ${ showCodeImpl('a) }

  def showCodeImpl(a: Expr[Any])(using Quotes): Expr[String] = Expr(a.show)

  /** A quote builds code; a splice inside it inserts an `Expr`. */
  inline def twice(inline a: Int): Int = ${ twiceImpl('a) }

  def twiceImpl(a: Expr[Int])(using Quotes): Expr[Int] = '{ $a + $a }

  // ---------------------------------------------------------------------------
  // Values in, values out, and errors at the caller's position.
  // ---------------------------------------------------------------------------

  inline def positive(inline n: Int): Int = ${ positiveImpl('n) }

  def positiveImpl(n: Expr[Int])(using Quotes): Expr[Int] =
    if n.valueOrAbort > 0 then n
    else quotes.reflect.report.errorAndAbort("expected a positive literal", n)

  // ---------------------------------------------------------------------------
  // Matching on code.
  // ---------------------------------------------------------------------------

  inline def simplify(inline e: Int): Int = ${ simplifyImpl('e) }

  def simplifyImpl(e: Expr[Int])(using Quotes): Expr[Int] = e match
    case '{ ($x: Int) * 1 } => x
    case _ => e

  inline def isSum(inline e: Int): Boolean = ${ isSumImpl('e) }

  def isSumImpl(e: Expr[Int])(using Quotes): Expr[Boolean] = e match
    case '{ ($x: Int) + ($y: Int) } => Expr(true)
    case _ => Expr(false)

  /** The source text of an argument, next to what `show` makes of it. */
  inline def sourceAndShow(inline e: Int): String = ${ sourceAndShowImpl('e) }

  def sourceAndShowImpl(e: Expr[Int])(using Quotes): Expr[String] =
    import quotes.reflect.*
    Expr(e.asTerm.pos.sourceCode.getOrElse("?") + " | " + e.show)

  // ---------------------------------------------------------------------------
  // Reflection.
  // ---------------------------------------------------------------------------

  inline def isCaseClass[T]: Boolean = ${ isCaseClassImpl[T] }

  def isCaseClassImpl[T: Type](using Quotes): Expr[Boolean] =
    import quotes.reflect.*
    Expr(TypeRepr.of[T].typeSymbol.flags.is(Flags.Case))

  /** A type as `show` prints it, and as the short printer does. */
  inline def typeNames[T]: (String, String) = ${ typeNamesImpl[T] }

  def typeNamesImpl[T: Type](using Quotes): Expr[(String, String)] =
    import quotes.reflect.*
    val tpe = TypeRepr.of[T]
    Expr((tpe.show, tpe.show(using Printer.TypeReprShortCode)))

  /** The type of each case field, as a member of `T`. */
  inline def fieldTypes[T]: List[String] = ${ fieldTypesImpl[T] }

  def fieldTypesImpl[T: Type](using Quotes): Expr[List[String]] =
    import quotes.reflect.*
    val tpe = TypeRepr.of[T]
    Expr(
      tpe.typeSymbol.caseFields.map(f => tpe.memberType(f).show(using Printer.TypeReprShortCode))
    )

end WalkthroughMacros
