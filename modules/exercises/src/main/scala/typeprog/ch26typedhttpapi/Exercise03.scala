package typeprog.ch26typedhttpapi

/** Exercise 03 — a client from the same values.
  *
  * Exercises 01 and 02 are given, solved. A `Client` calls a server — any
  * `Request => Response`, here a `Router`'s `serve` — through the endpoints
  * the server was built from:
  *
  *   - `client.call(userName)(1)` takes what `userName`'s handler receives,
  *     `Args[In]` — an `Int` here, a tuple for two captures, `()` for none —
  *     and returns an `Either[String, Out]`: `Right("Ada")`, typed
  *     `Either[String, String]`;
  *   - arguments of the wrong type, or the wrong number, do not compile;
  *   - the request is the endpoint's method and its path rendered with the
  *     arguments; a `200` body is read with the endpoint's `Codec`;
  *   - any other status is `Left("<status> <body>")`, and a body the codec
  *     cannot read is `Left("cannot read the body: <body>")`.
  *
  * As shipped, `call` takes `Any` and returns `Either[String, Any]`.
  *
  * Hint: chapter 03 — a later parameter list, and the result, can be typed
  * through an earlier parameter. The arguments go back into a tuple the way
  * `Args.of` took them out, and at runtime the path knows how many captures
  * it has.
  */
object Exercise03:

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
  // Exercise 02, solved.
  // ---------------------------------------------------------------------------

  /** What a handler receives for a path that captures `In`. */
  type Args[In <: Tuple] = In match
    case EmptyTuple => Unit
    case a *: EmptyTuple => a
    case _ => In

  object Args:
    // The value counterpart of the match type: the same three cases, decided
    // by arity, since `Args[In]` cannot reduce while `In` is abstract.
    def of[In <: Tuple](in: In): Args[In] =
      val args = in.productArity match
        case 0 => ()
        case 1 => in.productElement(0)
        case _ => in
      args.asInstanceOf[Args[In]]

  trait Endpoint:
    type In <: Tuple
    type Out
    def method: Method
    def path: Path[In]
    def codec: Codec[Out]

    def handle(f: Args[In] => Out): Route =
      Route(method, url => path.parse(url).map(in => codec.encode(f(Args.of(in)))))

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

  // ---------------------------------------------------------------------------
  // Exercise 03.
  // ---------------------------------------------------------------------------

  final class Client(server: Request => Response):

    /** TODO: call `e` on the server, typed by `e`. */
    def call(e: Endpoint)(args: Any): Either[String, Any] = ???
end Exercise03
