package typeprog.ch25macrocheckedinterpolator

import scala.quoted.*
import JsonTemplate.{Key, Tpl, ParseError}

/** Exercise 01 — JSON, read while compiling.
  *
  * `json"..."` is the JSON written between the quotes, as a `Json` value:
  *
  *   - `json"""{"name": "Ada", "tags": ["a", "b"]}"""` is the `JObject` with
  *     those two fields, in that order;
  *   - JSON's escapes are read as JSON reads them: `"x\ny"` holds a newline;
  *   - invalid JSON does not compile, with a message that starts `invalid
  *     JSON: ` followed by what the parser says is wrong — and the error
  *     points at the place in the literal where it went wrong;
  *   - parts that cannot be read while compiling, a `StringContext` held in a
  *     `val`, do not compile, with a message that says they must be a
  *     `literal`;
  *   - an argument, `json"[$n]"`, does not compile either, with a message
  *     that mentions the `argument`: arguments are exercise 02's.
  *
  * The JSON is parsed once, while compiling. The program contains the code
  * that builds the value, and no parser.
  *
  * The parser is given: `JsonTemplate.parse`, in this package, reads the
  * parts. The implementation must not throw — it runs inside the compiler. As
  * shipped it returns `'{ ??? }`, code that throws when *it* runs.
  *
  * Hint: chapter 12 — a quoted pattern can take the `StringContext(...)` call
  * apart, and `Varargs` is the pattern for the parts written out in it. A
  * parse error names a part and an offset; a part, seen through
  * `quotes.reflect`, has a position. `Expr.ofList` makes one expression out
  * of a list of them.
  */
object Exercise01:

  extension (inline sc: StringContext)
    inline def json(inline args: Any*): Json = ${ jsonImpl('sc, 'args) }

  private def build(tpl: Tpl, args: Seq[Expr[Any]])(using Quotes): Expr[Json] =
    import quotes.reflect.*

    tpl match
      case Tpl.Null => '{ Json.JNull }
      case Tpl.Bool(b) => '{ Json.JBool(${ Expr(b) }) }
      case Tpl.Number(n) => '{ Json.JNumber(${ Expr(n) }) }
      case Tpl.Str(s) => '{ Json.JString(${ Expr(s) }) }
      case Tpl.Arr(ls) => '{ Json.JArray(${ Expr.ofList(ls.map(tpl => build(tpl, args))) }) }
      case Tpl.Obj(fs) =>
        val fs_ = fs.map:
          case Key.Name(n) -> tpl => '{ (${ Expr(n) }, ${ build(tpl, args) }) }
          case Key.Arg(i) -> _ =>
            report.errorAndAbort("json does not take arguments yet", args(i))
        '{ Json.JObject(${ Expr.ofList(fs_) }) }
      case Tpl.Arg(i) =>
        report.errorAndAbort("json does not take arguments yet", args(i))

  def jsonImpl(sc: Expr[StringContext], args: Expr[Seq[Any]])(using Quotes): Expr[Json] =
    import quotes.reflect.*

    sc match
      case '{ StringContext(${ Varargs(parts) }*) } =>
        JsonTemplate.parse(parts.map(_.valueOrAbort).toList) match
          case Right(tpl) =>
            args match
              case Varargs(arguments) => build(tpl, arguments)
              case _ => report.errorAndAbort("json needs its arguments written out", args)
          case Left(ParseError(part, offset, message)) =>
            val pos = parts(part).asTerm.pos
            report.errorAndAbort(
              "invalid JSON: " + message,
              Position(pos.sourceFile, pos.start + offset, pos.start + offset)
            )
      case _ => report.errorAndAbort("parts must be written as a literal", sc)

end Exercise01
