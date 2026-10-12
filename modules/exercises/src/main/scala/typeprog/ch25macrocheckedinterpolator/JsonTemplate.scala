package typeprog.ch25macrocheckedinterpolator

import scala.annotation.tailrec

/** JSON as a value: what every `json"..."` of this chapter evaluates to. */
enum Json:
  case JNull
  case JBool(value: Boolean)
  case JNumber(value: Double)
  case JString(value: String)
  case JArray(items: List[Json])
  case JObject(fields: List[(String, Json)])

/** The parser the interpolators run while compiling. Given, not an exercise:
  * reading JSON is not what this chapter is about, and a parser is ordinary
  * code — a macro calls it like any other method.
  *
  * It reads the literal parts of an interpolation, `json"""{"id": $id}"""`
  * being `List("{\"id\": ", "}")`, with an argument between every two of
  * them. Where the argument stands is part of the result: a value
  * (`Tpl.Arg`), or an object's key (`Key.Arg`). An argument inside a string,
  * `"id-$id"`, is rejected — a string is a literal, and splicing into it would
  * need its own rules.
  *
  * The parts are taken as written. An interpolator receives its parts without
  * Scala's escapes processed, so `\n` arrives as a backslash and an `n`, and
  * the escapes read here are JSON's.
  */
object JsonTemplate:

  /** A parsed template: JSON, with the arguments' places marked. */
  enum Tpl:
    case Null
    case Bool(value: Boolean)
    case Number(value: Double)
    case Str(value: String)
    case Arr(items: List[Tpl])
    case Obj(fields: List[(Key, Tpl)])

    /** The `index`-th argument of the interpolation, as a value. */
    case Arg(index: Int)

  enum Key:
    case Name(value: String)

    /** The `index`-th argument of the interpolation, as a key. */
    case Arg(index: Int)

  /** What is wrong, and where: `offset` characters into the literal part
    * number `part`, counted from zero.
    */
  final case class ParseError(part: Int, offset: Int, message: String)

  /** Parses the literal parts of an interpolation, with an argument between
    * every two parts. `parts` must not be empty — an interpolation always has
    * one more part than arguments.
    */
  def parse(parts: List[String]): Either[ParseError, Tpl] =
    require(parts.nonEmpty, "an interpolation has at least one part")
    Parser(parts).document

  // An argument is read as one character the parts cannot contain where it
  // matters: a NUL is not valid JSON outside a string, and inside one it must
  // be escaped. `\u0000` written in the source arrives as six characters, not
  // as this one.
  private val Hole = '\u0000'

  private val NumberPattern = """-?(0|[1-9]\d*)(\.\d+)?([eE][+-]?\d+)?""".r

  private val Escapes =
    Map(
      '"' -> '"',
      '\\' -> '\\',
      '/' -> '/',
      'b' -> '\b',
      'f' -> '\f',
      'n' -> '\n',
      'r' -> '\r',
      't' -> '\t'
    )

  /** Recursive descent over the parts joined by `Hole`. Every rule takes the
    * index to start at and answers what it read with the index after it.
    */
  private final class Parser(parts: List[String]):

    private type Result[A] = Either[ParseError, (A, Int)]

    private val text = parts.mkString(Hole.toString)

    // Where each part starts in `text`, to turn an index back into a part and
    // an offset. A hole belongs to the part it ends.
    private val starts =
      parts.scanLeft(0)((start, part) => start + part.length + 1).take(parts.length)

    private def partAt(at: Int): Int = starts.lastIndexWhere(_ <= at)

    private def fail[A](at: Int, message: String): Result[A] =
      val part = partAt(at)
      Left(ParseError(part, at - starts(part), message))

    private def isAt(at: Int, c: Char): Boolean = at < text.length && text(at) == c

    @tailrec private def skip(at: Int): Int =
      if at < text.length && " \t\r\n".contains(text(at)) then skip(at + 1) else at

    def document: Either[ParseError, Tpl] =
      value(0).flatMap: (tpl, next) =>
        val at = skip(next)
        if at == text.length then Right(tpl)
        else fail(at, "unexpected text after the value").map(_._1)

    private def value(from: Int): Result[Tpl] =
      val at = skip(from)
      if at == text.length then fail(at, "expected a value, found the end")
      else
        text(at) match
          case Hole => Right(Tpl.Arg(partAt(at)) -> (at + 1))
          case '{' => obj(at + 1)
          case '[' => arr(at + 1)
          case '"' => str(at + 1, "").map((s, next) => Tpl.Str(s) -> next)
          case c if c == '-' || c.isDigit => number(at)
          case _ if text.startsWith("true", at) => Right(Tpl.Bool(true) -> (at + 4))
          case _ if text.startsWith("false", at) => Right(Tpl.Bool(false) -> (at + 5))
          case _ if text.startsWith("null", at) => Right(Tpl.Null -> (at + 4))
          case c => fail(at, s"unexpected '$c'")

    private def number(at: Int): Result[Tpl] =
      NumberPattern.findPrefixMatchOf(text.substring(at)) match
        case Some(m) => Right(Tpl.Number(m.matched.toDouble) -> (at + m.end))
        case None => fail(at, "invalid number")

    private def arr(from: Int): Result[Tpl] =
      val at = skip(from)
      if isAt(at, ']') then Right(Tpl.Arr(Nil) -> (at + 1)) else items(at, Nil)

    private def items(from: Int, acc: List[Tpl]): Result[Tpl] =
      value(from).flatMap: (item, next) =>
        val at = skip(next)
        if isAt(at, ',') then items(at + 1, item :: acc)
        else if isAt(at, ']') then Right(Tpl.Arr((item :: acc).reverse) -> (at + 1))
        else fail(at, "expected ',' or ']'")

    private def obj(from: Int): Result[Tpl] =
      val at = skip(from)
      if isAt(at, '}') then Right(Tpl.Obj(Nil) -> (at + 1)) else members(at, Nil)

    private def members(from: Int, acc: List[(Key, Tpl)]): Result[Tpl] =
      key(from).flatMap: (k, afterKey) =>
        val colon = skip(afterKey)
        if !isAt(colon, ':') then fail(colon, "expected ':'")
        else
          value(colon + 1).flatMap: (v, next) =>
            val at = skip(next)
            if isAt(at, ',') then members(at + 1, (k -> v) :: acc)
            else if isAt(at, '}') then Right(Tpl.Obj(((k -> v) :: acc).reverse) -> (at + 1))
            else fail(at, "expected ',' or '}'")

    private def key(from: Int): Result[Key] =
      val at = skip(from)
      if isAt(at, '"') then str(at + 1, "").map((s, next) => Key.Name(s) -> next)
      else if isAt(at, Hole) then Right(Key.Arg(partAt(at)) -> (at + 1))
      else fail(at, "expected a key")

    @tailrec private def str(at: Int, acc: String): Result[String] =
      if at == text.length then fail(at, "unterminated string")
      else
        text(at) match
          case '"' => Right(acc -> (at + 1))
          case Hole => fail(at, "an argument cannot be inside a string")
          case '\\' =>
            escape(at + 1) match
              case Some((c, length)) => str(at + 1 + length, acc + c)
              case None => fail(at, "invalid escape")
          case c if c < ' ' => fail(at, "a control character in a string must be escaped")
          case c => str(at + 1, acc + c)

    /** The character an escape stands for, and how many characters after the
      * backslash it took.
      */
    private def escape(at: Int): Option[(Char, Int)] =
      if at == text.length then None
      else if text(at) == 'u' then
        val hex = text.slice(at + 1, at + 5)
        Option.when(hex.length == 4 && hex.forall(Character.digit(_, 16) >= 0)):
          Integer.parseInt(hex, 16).toChar -> 5
      else Escapes.get(text(at)).map(_ -> 1)
  end Parser
end JsonTemplate
