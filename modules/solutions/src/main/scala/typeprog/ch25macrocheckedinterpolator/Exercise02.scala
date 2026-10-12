package typeprog.ch25macrocheckedinterpolator

import scala.quoted.*

import JsonTemplate.{Key, ParseError, Tpl}

/** Exercise 02 — arguments, encoded by their type. *Solved.*
  *
  * Exercise 01's reading of the literal is unchanged; what changes is
  * `Tpl.Arg(i)`. In its place goes the code `encoder.encode(arg)`, for an
  * `Encoder` found now, while compiling — so a type with none is a compile
  * error, at the argument, and a type with one costs a method call at
  * runtime.
  *
  * The quoted pattern `'{ $a: t }` binds `t` to the argument's type, and that
  * type is the argument's *own*: `n.type` for a `val n`, the literal type `1`
  * for `${1}`. `Encoder` is invariant, and `Expr.summon[Encoder[n.type]]`
  * finds nothing — not even the `Int` instance. `TypeRepr.of[t].widen` is the
  * type the argument was declared with, `.asType` makes it a `Type` again,
  * and matching it on `'[w]` gives it a name to summon with. `a.asExprOf[w]`
  * then states, with a check made while compiling, what `t <: w` already
  * says.
  *
  * `Expr.summon` is chapter 08's `summonFrom` with an `Option` for an answer:
  * `None` is where the error goes, with a message built from the widened type,
  * printed short (chapter 12) — `Instant`, not `java.time.Instant`.
  *
  * Each argument appears once in the template, so each is evaluated once, in
  * the order written. A key is not a value, and is left to exercise 03.
  */
object Exercise02:

  trait Encoder[A]:
    def encode(a: A): Json

  object Encoder:
    given Encoder[Int] = n => Json.JNumber(n)
    given Encoder[Double] = Json.JNumber(_)
    given Encoder[String] = Json.JString(_)
    given Encoder[Boolean] = Json.JBool(_)
    given Encoder[Json] = json => json
    given [A] => (e: Encoder[A]) => Encoder[Option[A]] = _.fold(Json.JNull)(e.encode)
    given [A] => (e: Encoder[A]) => Encoder[List[A]] = as => Json.JArray(as.map(e.encode))

  extension (inline sc: StringContext)
    inline def json(inline args: Any*): Json = ${ jsonImpl('sc, 'args) }

  def jsonImpl(sc: Expr[StringContext], args: Expr[Seq[Any]])(using Quotes): Expr[Json] =
    import quotes.reflect.*
    val parts = sc match
      case '{ StringContext(${ Varargs(parts) }*) } => parts
      case _ =>
        report.errorAndAbort(
          "json reads its parts while compiling: write them as a literal, json\"...\"",
          sc
        )
    val arguments = args match
      case Varargs(arguments) => arguments
      case _ => report.errorAndAbort("json needs its arguments written out", args)
    JsonTemplate.parse(parts.toList.map(_.valueOrAbort)) match
      case Right(tpl) => build(tpl, arguments)
      case Left(ParseError(part, offset, message)) =>
        val pos = parts(part).asTerm.pos
        report.errorAndAbort(
          s"invalid JSON: $message",
          Position(pos.sourceFile, pos.start + offset, pos.start + offset)
        )

  private def build(tpl: Tpl, args: Seq[Expr[Any]])(using Quotes): Expr[Json] =
    tpl match
      case Tpl.Null => '{ Json.JNull }
      case Tpl.Bool(b) => '{ Json.JBool(${ Expr(b) }) }
      case Tpl.Number(d) => '{ Json.JNumber(${ Expr(d) }) }
      case Tpl.Str(s) => '{ Json.JString(${ Expr(s) }) }
      case Tpl.Arr(items) => '{ Json.JArray(${ Expr.ofList(items.map(build(_, args))) }) }
      case Tpl.Obj(fields) =>
        val pairs = fields.map((k, v) => '{ (${ key(k, args) }, ${ build(v, args) }) })
        '{ Json.JObject(${ Expr.ofList(pairs) }) }
      case Tpl.Arg(i) => encode(args(i))

  private def encode(arg: Expr[Any])(using Quotes): Expr[Json] =
    import quotes.reflect.*
    arg match
      case '{ $a: t } =>
        TypeRepr.of[t].widen.asType match
          case '[w] =>
            Expr.summon[Encoder[w]] match
              case Some(encoder) => '{ $encoder.encode(${ a.asExprOf[w] }) }
              case None =>
                val name = TypeRepr.of[w].show(using Printer.TypeReprShortCode)
                report.errorAndAbort(s"no Encoder for $name", arg)

  private def key(k: Key, args: Seq[Expr[Any]])(using Quotes): Expr[String] = k match
    case Key.Name(name) => Expr(name)
    case Key.Arg(i) =>
      quotes.reflect.report
        .errorAndAbort("an argument is a value here, not a key: keys are exercise 03's", args(i))
end Exercise02
