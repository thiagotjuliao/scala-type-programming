package typeprog.ch26typedhttpapi

/** Exercise 01 — a path whose type lists what it captures.
  *
  * A path is built from the root with `/`, a literal segment or a capture at
  * a time: `Path.root / "users" / capture[Int]("id") / "posts"`. Its type
  * parameter `In` is the tuple of what its captures read, in order:
  *
  *   - a literal adds nothing: `Path.root / "users"` is a `Path[EmptyTuple]`;
  *   - a capture adds its type at the end: the path above is a
  *     `Path[Int *: EmptyTuple]`, and one more `/ capture[String]("slug")`
  *     makes it a `Path[(Int, String)]`;
  *   - `render(in)` writes the path with those values — `"/users/42/posts"`
  *     for `Tuple1(42)`, `"/"` for the root — and takes nothing but an `In`;
  *   - `parse(path)` reads them back: `Some(Tuple1(42))` for
  *     `"/users/42/posts"`, `None` when a literal differs, a capture's
  *     `Segment` cannot read its segment, or the number of segments differs.
  *
  * As shipped, a capture returns a `Path[Tuple]`: a path that has forgotten
  * what it captures, so `render` takes any tuple at all.
  *
  * Hint: chapter 09 — the standard library has a match type that puts one
  * more type at the end of a tuple. For the values, `parts` is the path in
  * order, and so is the tuple.
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

    /** TODO: the path with this capture added — and its type. */
    def /[A](capture: Capture[A]): Path[Tuple] = Path(parts :+ capture)

    /** TODO: the path, with the captures' values in place. */
    def render(in: In): String = ???

    /** TODO: the captures' values, if the path matches. */
    def parse(path: String): Option[In] = ???
  end Path

  object Path:
    val root: Path[EmptyTuple] = Path(Nil)
end Exercise01
