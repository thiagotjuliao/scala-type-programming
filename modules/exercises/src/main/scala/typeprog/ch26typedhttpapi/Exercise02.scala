package typeprog.ch26typedhttpapi

import typeprog.core.Unsolved

/** Exercise 02 — the endpoint decides the handler's type.
  *
  * Exercise 01's paths are given, solved. An `Endpoint` pairs a method and a
  * path with the type it returns, as type members: `Endpoint.get(path)
  * .returning[String]` has `In` — what the path captures — and `Out =
  * String`. `handle` turns it into a `Route` for a `Router`, given the
  * function that answers it.
  *
  *   - `Args[In]` is what that function receives: `Unit` for a path that
  *     captures nothing, the value itself for one capture — a handler is
  *     `(id: Int) => ...`, not `(in: Tuple1[Int]) => ...` — and the tuple for
  *     more;
  *   - `handle` takes exactly an `Args[In] => Out`: a handler with the wrong
  *     argument, or the wrong result, does not compile;
  *   - the route it returns answers a path the endpoint's path parses with
  *     the handler's result, written with the endpoint's `Codec`, and does
  *     not answer any other.
  *
  * As shipped, `handle` takes a `Nothing => Any`, which every function is —
  * so it accepts any handler, and cannot call one.
  *
  * Hint: chapter 07 for `Args`. Inside `handle`, `In` is abstract and
  * `Args[In]` does not reduce: the value has to be turned into one the same
  * way the type is, case for case, and the compiler told so.
  */
object Exercise02:

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
  // Exercise 02.
  // ---------------------------------------------------------------------------

  /** TODO: what a handler receives for a path that captures `In`. */
  type Args[In <: Tuple] = Unsolved

  trait Endpoint:
    type In <: Tuple
    type Out
    def method: Method
    def path: Path[In]
    def codec: Codec[Out]

    /** TODO: a route that answers with `f`, typed by this endpoint. */
    def handle(f: Nothing => Any): Route = ???

  object Endpoint:
    type Aux[I <: Tuple, O] = Endpoint { type In = I; type Out = O }

    def get[I <: Tuple](path: Path[I]): Returning[I] = Returning(Method.Get, path)
    def post[I <: Tuple](path: Path[I]): Returning[I] = Returning(Method.Post, path)

    final class Returning[I <: Tuple](m: Method, p: Path[I]):
      def returning[O](using c: Codec[O]): Aux[I, O] = new Endpoint:
        type In = I
        type Out = O
        val method = m
        val path = p
        val codec = c

  /** A handled endpoint: the body for a path it matches. */
  final class Route(val method: Method, val run: String => Option[String])

  final class Router(routes: Route*):
    def serve(request: Request): Response =
      routes.iterator
        .filter(_.method == request.method)
        .flatMap(_.run(request.path))
        .nextOption() match
        case Some(body) => Response(200, body)
        case None => Response(404, s"no route for ${request.method} ${request.path}")
end Exercise02
