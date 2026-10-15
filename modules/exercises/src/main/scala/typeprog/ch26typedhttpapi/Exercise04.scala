package typeprog.ch26typedhttpapi

import scala.quoted.*

/** Exercise 04 — the path, written as a string and checked as a type.
  *
  * Exercise 01's paths are given, solved. `path"/users/$id/posts"`, with `id`
  * a `Capture[Int]`, is the path `Path.root / "users" / id / "posts"`, typed
  * the same way:
  *
  *   - `path"/users/$id/posts"` is a `Path[Int *: EmptyTuple]`, and
  *     `path"/health"` a `Path[EmptyTuple]` — the type, not just the value;
  *   - a path that does not start with `/` does not compile: `a path starts
  *     with /`;
  *   - nor one with an empty segment, `/users//posts`: `a path has no empty
  *     segment`;
  *   - nor a literal segment with a character outside letters, digits and
  *     `-._~`: `invalid segment: 'a b'`;
  *   - nor a capture that is not a whole segment, `/users/x$id`: `a capture
  *     must be a whole segment`;
  *   - nor an argument that is not a `Capture`: `only a Capture can be
  *     interpolated, not Int`, the type as written in the source.
  *
  * The implementation must not throw: it runs inside the compiler. As shipped
  * it returns the root as a `Path[Tuple]` — a path that has forgotten what it
  * captures, as exercise 01's stub did — so every call compiles, and nothing
  * is checked.
  *
  * Hint: chapter 25 for taking the call apart, and exercise 01's `/` for
  * building the result — as code, one call at a time, with each capture's
  * type bound by a quoted pattern. Chapter 08 says what the `transparent` on
  * `path` is for.
  */
object Exercise04:

  // ---------------------------------------------------------------------------
  // Exercise 01, solved.
  // ---------------------------------------------------------------------------

  final class Capture[A](val name: String)(using segment: Segment[A]):
    // The value comes from the tuple, whose element at this capture's
    // position is an `A` by construction (see `Path`).
    def show(value: Any): String = segment.show(value.asInstanceOf[A])
    def read(text: String): Option[A] = segment.parse(text)

  def capture[A: Segment](name: String): Capture[A] = Capture[A](name)

  final class Path[In <: Tuple] private (val parts: List[String | Capture[?]]):

    def /(literal: String): Path[In] = Path(parts :+ literal)

    def /[A](capture: Capture[A]): Path[Tuple.Append[In, A]] = Path(parts :+ capture)

    def render(in: In): String =
      def go(parts: List[String | Capture[?]], values: List[Any]): List[String] =
        (parts, values) match
          case (Nil, _) => Nil
          case ((literal: String) :: ps, vs) => literal :: go(ps, vs)
          case ((c: Capture[?]) :: ps, v :: vs) => c.show(v) :: go(ps, vs)
          case ((_: Capture[?]) :: _, Nil) =>
            throw AssertionError("a path has as many captures as its tuple has elements")
      go(parts, in.toList).mkString("/", "/", "")

    def parse(path: String): Option[In] =
      val segments = path.split("/").toList.filter(_.nonEmpty)
      if segments.length != parts.length then None
      else
        parts
          .zip(segments)
          .foldRight(Option(List.empty[Any])):
            case ((literal: String, s), rest) => if literal == s then rest else None
            case ((c: Capture[?], s), rest) => for v <- c.read(s); vs <- rest yield v :: vs
          .map(values => Tuple.fromArray(values.toArray).asInstanceOf[In])
  end Path

  object Path:
    val root: Path[EmptyTuple] = Path(Nil)

  // ---------------------------------------------------------------------------
  // Exercise 04.
  // ---------------------------------------------------------------------------

  extension (inline sc: StringContext)
    transparent inline def path(inline args: Any*): Path[?] = ${ pathImpl('sc, 'args) }

  def pathImpl(sc: Expr[StringContext], args: Expr[Seq[Any]])(using Quotes): Expr[Path[?]] =
    import quotes.reflect.*

    val texts = sc match
      case '{ StringContext(${ Varargs(parts) }*) } =>
        Expr.ofList(parts).valueOrAbort
      case _ =>
        report.errorAndAbort(
          "path reads its parts while compiling: write them as a literal, path\"...\"",
          sc
        )

    val arguments = args match
      case Varargs(as) => as
      case _ => report.errorAndAbort("path needs its arguments written out", args)

    // Validations
    if texts.isEmpty || !texts.head.startsWith("/") then
      report.errorAndAbort("a path starts with /", sc)

    if texts.find(_.contains("//")).isDefined then
      report.errorAndAbort("a path has no empty segment", sc)

    val validCharSet = (c: Char) => c.isLetterOrDigit || Seq('~', '.', '_', '-').contains(c)

    texts.map(_.split("/")).flatten.find(_.find(!validCharSet(_)).isDefined) match
      case Some(seg) => report.errorAndAbort(s"invalid segment: '$seg'")
      case None => ()

    val validArg = (s1: String, s2: String) =>
      s1.endsWith("/") && (s2.startsWith("/") || s2.isEmpty)

    // texts has at least 1 element so zipping with itself will give at least an empty list, hence not
    // throwing any errors
    texts
      .zip(texts.tail)
      .zipWithIndex
      .foreach:
        case ((s1, s2), i) =>
          if !validArg(s1, s2) then
            report.errorAndAbort("a capture must be a whole segment", arguments(i))

    val splitAndFilter: String => List[String] = _.split("/").filterNot(_.isEmpty).toList

    texts
      .zipAll(arguments.map(Option(_)), "", None)
      .flatMap[Either[String, Expr[Any]]]:
        case (s, Some(c)) =>
          c match
            case '{ $cap: Capture[a] } =>
              splitAndFilter(s).map(Left(_)) ++ List(Right(cap))
            case _ =>
              val name = c.asTerm.tpe.widen.show(using Printer.TypeReprShortCode)
              report.errorAndAbort(s"only a Capture can be interpolated, not $name")
        case (s, None) => splitAndFilter(s).map(Left(_))
      .foldLeft[Expr[Path[?]]]('{ Path.root }):
        case ('{ $p: Path[t] }, Left(s)) => '{ $p / ${ Expr(s) } }
        case ('{ $p: Path[t] }, Right('{ $cap: Capture[a] })) => '{ $p / $cap }
        case (_, item) =>
          report.errorAndAbort(s"path: unexpected $item, a bug in the macro, not in the path")
  end pathImpl
end Exercise04
