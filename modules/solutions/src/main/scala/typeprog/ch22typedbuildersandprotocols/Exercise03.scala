package typeprog.ch22typedbuildersandprotocols

/** Exercise 03 — a connection that follows its protocol. *Solved.*
  *
  * `send` takes a `Tuple.Head[Left & NonEmptyTuple]`. `Tuple.Head` is only
  * defined on a non-empty tuple, hence the intersection; for `Left = (Auth,
  * Data)` it reduces to `Auth`, and anything else is a type mismatch. After the
  * last message `Left` is `EmptyTuple`, the intersection matches no case, and
  * no value fits the parameter — so a message too many is rejected too. The
  * result moves that type: `Tuple.Append` adds it to the end of `Sent`, and
  * `Tuple.Tail` drops it from `Left`. The value side does the same with `:*`,
  * so the runtime transcript and its type grow together.
  *
  * `close` needs no evidence: its parameter is `Conn[P, EmptyTuple]`, a
  * conversation that has sent the whole protocol and has nothing left. For
  * `Server[(Hello, Auth, Data)]`, `Sent` after three sends is `Hello *: Auth *:
  * Data *: EmptyTuple`, which is `(Hello, Auth, Data)` — `P` itself — so
  * `close` can return `sent` as a `P` with no cast.
  *
  * `Conn` moved inside `Server`, and that is the whole of the third rule.
  * A class declared inside another is a different class for each instance of
  * the outer one: `server.Conn` and `other.Conn` are path-dependent types
  * (chapter 03), and `other.close` asks for an `other.Conn`. The constructor is
  * `private[Server]`, so a connection exists only through `connect`.
  */
object Exercise03:

  final case class Hello(from: String)
  final case class Auth(token: String)
  final case class Data(payload: String)

  final class Server[P <: Tuple]:

    final class Conn[Sent <: Tuple, Left <: Tuple] private[Server] (val sent: Sent):

      def send(message: Tuple.Head[Left & NonEmptyTuple]): Conn[
        Tuple.Append[Sent, Tuple.Head[Left & NonEmptyTuple]],
        Tuple.Tail[Left & NonEmptyTuple]
      ] =
        Conn(sent :* message)

    def connect(): Conn[EmptyTuple, P] = Conn(EmptyTuple)

    def close(conn: Conn[P, EmptyTuple]): P = conn.sent
