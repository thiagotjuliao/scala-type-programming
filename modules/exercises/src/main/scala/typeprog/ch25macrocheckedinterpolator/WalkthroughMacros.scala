package typeprog.ch25macrocheckedinterpolator

import scala.quoted.*

/** The interpolators `Walkthrough.scala` calls. They live in their own file
  * because a macro cannot be called from the file that defines it.
  */
object WalkthroughMacros:

  /** A type class for `show"..."`: how a value is written into text. */
  trait Show[A]:
    def show(a: A): String

  object Show:
    given Show[Int] = _.toString
    given Show[String] = s => s
    given [A] => (s: Show[A]) => Show[List[A]] = _.map(s.show).mkString("[", ", ", "]")

  extension (inline sc: StringContext)

    /** What an interpolator receives: its parts, and its arguments' types. */
    inline def received(inline args: Any*): String = ${ receivedImpl('sc, 'args) }

    /** Bytes written in hex, checked while compiling: `hex"ff 0a"`. */
    inline def hex(inline args: Any*): List[Int] = ${ hexImpl('sc, 'args) }

    /** `s"..."`, where every argument is written by its `Show`, found while
      * compiling.
      */
    inline def show(inline args: Any*): String = ${ showImpl('sc, 'args) }

  // ---------------------------------------------------------------------------
  // Taking the call apart. Both patterns fail only when the call was not
  // written as an interpolation, `StringContext(parts*)` held in a `val`.
  // ---------------------------------------------------------------------------

  private def partsOf(sc: Expr[StringContext])(using Quotes): Seq[Expr[String]] =
    sc match
      case '{ StringContext(${ Varargs(parts) }*) } => parts
      case _ => quotes.reflect.report.errorAndAbort("write the parts as a literal", sc)

  private def argumentsOf(args: Expr[Seq[Any]])(using Quotes): Seq[Expr[Any]] =
    args match
      case Varargs(arguments) => arguments
      case _ => quotes.reflect.report.errorAndAbort("write the arguments out", args)

  // ---------------------------------------------------------------------------
  // 1. What an interpolator receives
  // ---------------------------------------------------------------------------

  def receivedImpl(sc: Expr[StringContext], args: Expr[Seq[Any]])(using Quotes): Expr[String] =
    import quotes.reflect.*
    val parts = partsOf(sc).map(_.valueOrAbort)
    val types = argumentsOf(args).map:
      case '{ $a: t } => s"${Type.show[t]} widens to ${TypeRepr.of[t].widen.show}"
    val described = if types.isEmpty then "" else types.mkString("; types: ", ", ", "")
    Expr(parts.mkString("parts: ", " | ", "") + described)

  // ---------------------------------------------------------------------------
  // 2. A literal checked while compiling, with the error inside it
  // ---------------------------------------------------------------------------

  def hexImpl(sc: Expr[StringContext], args: Expr[Seq[Any]])(using Quotes): Expr[List[Int]] =
    import quotes.reflect.*
    argumentsOf(args).headOption.foreach(arg => report.errorAndAbort("hex takes no arguments", arg))
    val part = partsOf(sc).head
    val text = part.valueOrAbort
    val bad = text.indexWhere(c => !c.isWhitespace && Character.digit(c, 16) < 0)
    if bad >= 0 then
      val pos = part.asTerm.pos
      report.errorAndAbort(
        s"not a hex digit: '${text(bad)}'",
        Position(pos.sourceFile, pos.start + bad, pos.start + bad + 1)
      )
    // The bytes are computed here, in the compiler, and lifted: the program
    // holds the list, not the text.
    Expr(text.split("\\s+").toList.filter(_.nonEmpty).map(Integer.parseInt(_, 16)))

  // ---------------------------------------------------------------------------
  // 3. Arguments, each checked by its type
  // ---------------------------------------------------------------------------

  def showImpl(sc: Expr[StringContext], args: Expr[Seq[Any]])(using Quotes): Expr[String] =
    val parts = partsOf(sc).map(_.valueOrAbort)
    val shown = argumentsOf(args).map(showOne)
    // Parts and arguments alternate, and there is one more part: the last part
    // starts the fold, and each argument goes in front with the part before it.
    parts.init
      .zip(shown)
      .foldRight(Expr(parts.last)): (pair, rest) =>
        val (part, value) = pair
        '{ ${ Expr(part) } + $value + $rest }

  private def showOne(arg: Expr[Any])(using Quotes): Expr[String] =
    import quotes.reflect.*
    arg match
      case '{ $a: t } =>
        TypeRepr.of[t].widen.asType match
          case '[w] =>
            Expr.summon[Show[w]] match
              case Some(s) => '{ $s.show(${ a.asExprOf[w] }) }
              case None =>
                val name = TypeRepr.of[w].show(using Printer.TypeReprShortCode)
                report.errorAndAbort(s"no Show for $name", arg)

end WalkthroughMacros
