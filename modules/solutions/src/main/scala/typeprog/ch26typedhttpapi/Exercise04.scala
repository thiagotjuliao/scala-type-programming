package typeprog.ch26typedhttpapi

import scala.quoted.*

/** Exercise 04 — the path, written as a string and checked as a type.
  * *Solved.*
  *
  * `path"/users/$id/posts"` is exercise 01's `root / "users" / id / "posts"`,
  * written the way a path is read. The macro takes the call apart (chapter
  * 12, and chapter 25's interpolator): the literal parts, with the text the
  * compiler can read, and the arguments, between them. It checks what a path
  * must be — it starts with `/`, no segment is empty, a literal segment has
  * only the characters a path allows, and a capture is a whole segment, since
  * `/users/x$id` would need a `Segment` for `x42` — and then writes out the
  * calls to `/` it stands for.
  *
  * The type comes from those calls. Each step matches the path built so far
  * as `'{ $p: Path[t] }`, binding the tuple it has reached, and each capture
  * as `'{ $c: Capture[a] }`, binding the type it reads; `'{ $p / $c }` is then
  * a `Path[Tuple.Append[t, a]]`, as in exercise 01. The macro's own result
  * type cannot say which tuple — it is `Expr[Path[?]]` for every call — and
  * that is what `transparent` is for (chapter 08): the call takes the type of
  * the code it expands to, so `path"/users/$id/posts"` is a
  * `Path[Int *: EmptyTuple]` and not a `Path[?]` nobody can render.
  *
  * An argument that is not a `Capture` — an `Int`, say — is rejected at the
  * argument, with its type printed widened and short, as in chapter 25.
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

  private val Allowed = ('a' to 'z').toSet ++ ('A' to 'Z') ++ ('0' to '9') ++ "-._~"

  def pathImpl(sc: Expr[StringContext], args: Expr[Seq[Any]])(using Quotes): Expr[Path[?]] =
    import quotes.reflect.*
    val parts = sc match
      case '{ StringContext(${ Varargs(parts) }*) } => parts.toList
      case _ =>
        report.errorAndAbort("path reads its parts while compiling: write them as a literal", sc)
    val arguments = args match
      case Varargs(arguments) => arguments.toList
      case _ => report.errorAndAbort("path needs its arguments written out", args)
    val texts = parts.map(_.valueOrAbort)

    if !texts.head.startsWith("/") then report.errorAndAbort("a path starts with /", parts.head)
    parts
      .zip(texts)
      .foreach: (part, text) =>
        if text.contains("//") then report.errorAndAbort("a path has no empty segment", part)
        text
          .split("/")
          .find(_.exists(c => !Allowed(c)))
          .foreach: segment =>
            report.errorAndAbort(s"invalid segment: '$segment'", part)
    // An argument stands between two parts, and is a segment of its own when
    // the part before it ends a segment and the part after it starts one.
    arguments.zipWithIndex.foreach: (argument, i) =>
      val after = texts(i + 1)
      if !texts(i).endsWith("/") || !(after.isEmpty || after.startsWith("/")) then
        report.errorAndAbort("a capture must be a whole segment", argument)

    val items = texts
      .zip(arguments.map(Some(_)) :+ None)
      .flatMap: (text, argument) =>
        text.split("/").toList.filter(_.nonEmpty).map(Left(_)) ++ argument.map(Right(_))

    items.foldLeft[Expr[Path[?]]]('{ Path.root }): (built, item) =>
      built match
        case '{ $p: Path[t] } =>
          item match
            case Left(literal) => '{ $p / ${ Expr(literal) } }
            case Right(argument) =>
              argument match
                case '{ $c: Capture[a] } => '{ $p / $c }
                case _ =>
                  val name = argument.asTerm.tpe.widen.show(using Printer.TypeReprShortCode)
                  report.errorAndAbort(s"only a Capture can be interpolated, not $name", argument)
  end pathImpl
end Exercise04
