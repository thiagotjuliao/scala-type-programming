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

  /** TODO: check the path, and build it with `/`. */
  def pathImpl(sc: Expr[StringContext], args: Expr[Seq[Any]])(using Quotes): Expr[Path[?]] =
    '{ Path.root.asInstanceOf[Path[Tuple]] }
end Exercise04
