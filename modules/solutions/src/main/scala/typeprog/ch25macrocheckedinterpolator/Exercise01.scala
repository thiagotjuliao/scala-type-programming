package typeprog.ch25macrocheckedinterpolator

import scala.quoted.*

import JsonTemplate.{Key, ParseError, Tpl}

/** Exercise 01 — JSON, read while compiling. *Solved.*
  *
  * The `StringContext` the macro receives is the code
  * `StringContext("[1, ", "]")`, and the quoted pattern
  * `'{ StringContext(${ Varargs(parts) }*) }` takes it apart into one `Expr`
  * per part. Each is a literal, so `valueOrAbort` has its text. When the
  * pattern does not match, the parts are not there to read — a
  * `StringContext` held in a `val`, say — and the error says so instead of
  * letting `valueOrAbort` print *"Expected a known value"*.
  *
  * `JsonTemplate.parse` runs on the texts, in the compiler. Its `ParseError`
  * names a part and an offset into it, and since a part's position starts at
  * its first character, `pos.start + offset` is the character the parser
  * stopped at: the error is underlined there, inside the literal.
  *
  * `build` turns the parsed template into the code that builds it: a
  * constructor call per node, `Expr(...)` for each string and number, and
  * `Expr.ofList` to make one `Expr[List[...]]` out of a list of `Expr`s. The
  * JSON is parsed once, while compiling; at runtime the call is the
  * constructors and nothing else.
  *
  * An argument is rejected where `build` meets it, with the argument's own
  * position. Checking `args` up front would do as well here; meeting them in
  * the template is the shape the next exercise needs, where what an argument
  * may be depends on where it is.
  */
object Exercise01:

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
      case Tpl.Arg(i) => noArguments(args(i))

  private def key(k: Key, args: Seq[Expr[Any]])(using Quotes): Expr[String] = k match
    case Key.Name(name) => Expr(name)
    case Key.Arg(i) => noArguments(args(i))

  private def noArguments(arg: Expr[Any])(using Quotes): Nothing =
    quotes.reflect.report
      .errorAndAbort("json takes no argument yet: arguments are exercise 02's", arg)
end Exercise01
