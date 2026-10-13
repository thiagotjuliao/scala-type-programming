package typeprog.ch26typedhttpapi

/** Exercise 01 — a path whose type lists what it captures. *Solved.*
  *
  * `Path[In]` carries, as a tuple type, the types of the values its captures
  * read: `root / "users" / capture[Int]("id") / "posts"` is a
  * `Path[Int *: EmptyTuple]`. The two `/`s differ in what they do to it. A
  * literal segment captures nothing and leaves `In` as it was; a capture adds
  * its type at the end, `Tuple.Append[In, A]` (chapter 09). The type is
  * computed one call at a time, as the path is written, so nobody ever writes
  * it down.
  *
  * `render` and `parse` then have the types the path promises, and nothing
  * else. `render` takes exactly an `In`, so a path with an `Int` capture
  * cannot be rendered with a `String`; `parse` gives one back, or `None` for a
  * path that does not match — a literal that differs, a segment its capture's
  * `Segment` cannot read, a different number of segments.
  *
  * Underneath, the parts are a plain list and the values a plain list too:
  * the tuple's elements, read in order, are the captures' values, in order.
  * The compiler cannot see that, so the two places where the types meet the
  * list — `Capture.show`, and `parse` turning the values back into a tuple —
  * are casts. They are safe for the same reason chapter 10's were: the list
  * and the type are built by the same calls, in the same order. The near miss
  * keeps `In` for a capture too: everything still compiles, and `render`
  * accepts any tuple, because the path has forgotten what it captures.
  */
object Exercise01:

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
end Exercise01
