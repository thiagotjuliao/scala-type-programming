package typeprog.ch25macrocheckedinterpolator

import scala.quoted.*

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

  /** TODO: read the parts, parse them, and build the `Json` they describe. */
  def jsonImpl(sc: Expr[StringContext], args: Expr[Seq[Any]])(using Quotes): Expr[Json] = '{ ??? }
end Exercise01
