package typeprog.ch26typedhttpapi

/** Exercise 03 — a client from the same values. *Solved.*
  *
  * `call(e: Endpoint)(args: Args[e.In]): Either[String, e.Out]` is a
  * dependent method type (chapter 03): the second parameter list and the
  * result are typed through the first. At `client.call(userName)(1)` the
  * compiler replaces `e` with `userName`, reads `In = Int *: EmptyTuple` and
  * `Out = String` off its type, reduces `Args` to `Int`, and the call takes an
  * `Int` and returns an `Either[String, String]`. The client never names a
  * type; the endpoint value carries them all.
  *
  * The body runs the server's steps backwards. `Args.tuple` undoes `Args.of`
  * — by the path's number of captures, the only arity it has at runtime —
  * and `render` turns the tuple into the path the router will `parse`. The
  * response body is read with the endpoint's own `Codec`, the one the server
  * wrote it with. Client and server are built from the same value, so the
  * path, the arguments and the body cannot disagree between them: a change to
  * the endpoint changes both, or stops one compiling.
  *
  * Failures stay values. A status other than 200, and a body the codec cannot
  * read, are a `Left` with what went wrong — the typed part ends where the
  * text from the other side begins.
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

  /** `Args.of` the other way round: the tuple a path renders, from what a
    * caller passes.
    */
  def tuple[In <: Tuple](path: Path[In], args: Args[In]): In =
    val captures = path.parts.count:
      case _: Capture[?] => true
      case _ => false
    val in = captures match
      case 0 => EmptyTuple
      case 1 => Tuple1(args)
      case _ => args
    in.asInstanceOf[In]

  final class Client(server: Request => Response):

    def call(e: Endpoint)(args: Args[e.In]): Either[String, e.Out] =
      val response = server(Request(e.method, e.path.render(tuple(e.path, args))))
      if response.status != 200 then Left(s"${response.status} ${response.body}")
      else e.codec.decode(response.body).toRight(s"cannot read the body: ${response.body}")
end Exercise03
