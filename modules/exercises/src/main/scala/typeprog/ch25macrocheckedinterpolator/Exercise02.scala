package typeprog.ch25macrocheckedinterpolator

import scala.quoted.*

/** Exercise 02 — arguments, encoded by their type.
  *
  * Exercise 01's `json"..."`, with arguments in the places of values:
  *
  *   - `json"""{"name": $name, "tags": $tags}"""` puts each argument where it
  *     stands, encoded by the `Encoder` for its type — a `String` is a
  *     `JString`, a `List[String]` a `JArray`, an empty `Option` a `JNull`, a
  *     `Json` itself;
  *   - an argument written as a literal, `${1}`, is encoded like any `Int`;
  *   - an argument whose type has no `Encoder` does not compile, with a
  *     message that starts `no Encoder for ` and the type as it is written
  *     in the source: `no Encoder for Instant`;
  *   - an argument as an object's key does not compile, with a message that
  *     mentions the `key`: keys are exercise 03's;
  *   - the literal around the arguments is checked as in exercise 01: an
  *     argument inside a string is the parser's error.
  *
  * The `Encoder` is found while compiling. At runtime each argument is
  * evaluated once, and encoded.
  *
  * Hint: `args` is taken apart like the parts. A quoted pattern can bind an
  * expression's type as well as the expression, and `Expr.summon` looks for
  * a given while compiling. Look closely at the type it binds for `${1}` —
  * chapter 04 has a name for it, and `TypeRepr` a method that gets rid of it.
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

  /** TODO: exercise 01, with every argument encoded in its place. */
  def jsonImpl(sc: Expr[StringContext], args: Expr[Seq[Any]])(using Quotes): Expr[Json] = '{ ??? }
end Exercise02
