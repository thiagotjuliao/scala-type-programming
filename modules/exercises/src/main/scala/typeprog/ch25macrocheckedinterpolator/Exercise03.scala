package typeprog.ch25macrocheckedinterpolator

import scala.quoted.*

import JsonTemplate.{Key, ParseError, Tpl}

/** Exercise 03 — a hole's place decides its type.
  *
  * Exercise 02's `json"..."` is given, solved — for arguments in the places of
  * values. An argument in a key's place compiles, and throws when it runs.
  *
  *   - `json"{$k: 1}"`, with `k` a `String`, is the object whose key is the
  *     value of `k`; a literal `${"id"}` works as well;
  *   - an argument in a key's place that is not a `String` does not compile,
  *     with a message that starts `a key must be a String, not ` and the type
  *     as it is written in the source: `not Int`, `not Option[String]`;
  *   - that holds whatever the type, `Encoder` or not: a key of type
  *     `Instant` is reported as a key that is not a `String`, never as a
  *     missing `Encoder`;
  *   - a value still needs its `Encoder`, as in exercise 02.
  *
  * Hint: chapter 12 — a quoted pattern can ascribe a type, and only matches
  * an expression of that type. For the message, the argument's type is in its
  * tree, and is narrower than the one to print.
  */
object Exercise03:

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

  /** TODO: an argument as a key — a `String`, or an error that names its type. */
  private def key(k: Key, args: Seq[Expr[Any]])(using Quotes): Expr[String] = k match
    case Key.Name(name) => Expr(name)
    case Key.Arg(_) => '{ ??? }
end Exercise03
