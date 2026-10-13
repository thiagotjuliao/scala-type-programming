package typeprog.ch26typedhttpapi

/** Exercise 02 — the endpoint decides the handler's type. *Solved.*
  *
  * An `Endpoint` carries its input and output as type members, `In` and `Out`
  * (chapter 03): `Endpoint.get(path).returning[String]` is an
  * `Endpoint.Aux[Int *: EmptyTuple, String]`, the refinement keeping both
  * (chapter 06's `Aux`). `handle` then asks for a function from what the path
  * captures to what the endpoint returns, and a handler of any other shape is
  * a type error at the line that registers it.
  *
  * `Args` is what the handler is handed: chapter 07's match type over the
  * tuple. No capture is `Unit`, one capture is its value — `(id: Int) =>` and
  * not `(in: Tuple1[Int]) =>` — and more are the tuple itself, which a
  * `{ case (id, slug) => }` takes apart. At a call `In` is concrete, so
  * `Args[In]` reduces to the type the handler is written against.
  *
  * Inside `handle` it does not: `In` is abstract there, and `Args[In]` is
  * stuck. `Args.of` therefore makes the same decision on the value, by its
  * arity, and states the result with a cast — the value match mirrors the
  * match type case for case, which is what makes it safe (chapter 07,
  * exercise 03, without the typed patterns, since all three cases are
  * tuples).
  *
  * The stub's `handle(f: Nothing => Any)` accepted every handler, because
  * `Function1` is contravariant in its argument and covariant in its result
  * (chapter 01): any function is a `Nothing => Any`. It also could not call
  * one — there is no `Nothing` to pass it.
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
end Exercise02
