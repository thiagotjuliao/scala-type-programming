package typeprog.ch25macrocheckedinterpolator

import scala.quoted.*

import JsonTemplate.{Key, ParseError, Tpl}

/** Exercise 03 — a hole's place decides its type. *Solved.*
  *
  * A key is a `String` in the JSON being built, so an argument in a key's
  * place must be one already. The quoted pattern `'{ $s: String }` matches an
  * expression whose type conforms to `String` — `k` for a `val k: String`,
  * and the literal `${"id"}`, whose type `"id"` is a subtype of it — and
  * gives it back as an `Expr[String]`, ready to be the key. No `Encoder` is
  * involved: one would turn a value into JSON, and a key is not JSON.
  *
  * The check is made where `build` meets the key, not before. Asking every
  * argument for an `Encoder` up front would reject a key of type `Instant`
  * as *"no Encoder for Instant"*: true, and about the wrong thing. Where the
  * hole is decides the question, so the question is asked there.
  *
  * The type in the error is the argument's, widened and printed short:
  * `Int`, not `n.type` nor `scala.Int`.
  *
  * Keys from arguments are only known at runtime, so two of them can be
  * equal, or equal to a literal key; nothing checks that, and the object
  * keeps both.
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
              case None => report.errorAndAbort(s"no Encoder for ${shortType(arg)}", arg)

  private def key(k: Key, args: Seq[Expr[Any]])(using Quotes): Expr[String] =
    import quotes.reflect.*
    k match
      case Key.Name(name) => Expr(name)
      case Key.Arg(i) =>
        args(i) match
          case '{ $s: String } => s
          case arg => report.errorAndAbort(s"a key must be a String, not ${shortType(arg)}", arg)

  /** The argument's type as its author would write it: widened, and short. */
  private def shortType(arg: Expr[Any])(using Quotes): String =
    import quotes.reflect.*
    arg.asTerm.tpe.widen.show(using Printer.TypeReprShortCode)
end Exercise03
