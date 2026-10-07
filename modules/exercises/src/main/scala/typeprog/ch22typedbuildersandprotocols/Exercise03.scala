package typeprog.ch22typedbuildersandprotocols

/** Exercise 03 — a connection that follows its protocol.
  *
  * A `Server[P]` speaks the protocol `P`: the tuple of messages a client
  * sends, in order. `Server[(Hello, Auth, Data)]` expects a `Hello`, then an
  * `Auth`, then a `Data`, and nothing more. A connection `Conn[Sent, Left]`
  * carries the messages sent so far and the ones still due.
  *
  *   - `send` takes only the next message due; it returns a connection with
  *     that message moved from `Left` to the end of `Sent`. A message out of
  *     order does not compile, and nor does one after the last.
  *   - `close` takes only a connection whose protocol is complete, and returns
  *     its transcript as a `P` — for the server above, a `(Hello, Auth,
  *     Data)`, with the messages that were sent.
  *   - A connection belongs to the server that opened it: closing it on
  *     another server does not compile, even one with the same protocol.
  *
  * As shipped, `Conn` lives outside `Server`, `send` takes anything, and
  * `close` takes any connection. No mutation: each `send` returns a new
  * connection.
  *
  * Hint: chapter 09 — `Tuple.Head`, `Tuple.Tail` and `Tuple.Append` already
  * exist. Chapter 03 — where a class is declared decides whose it is.
  */
object Exercise03:

  final case class Hello(from: String)
  final case class Auth(token: String)
  final case class Data(payload: String)

  final class Server[P <: Tuple]:
    final class Conn[Sent <: Tuple, Left <: Tuple] private[Server] (val sent: Sent):

      def send(
          message: Tuple.Head[Left]
      ): Conn[Tuple.Append[Sent, Tuple.Head[Left]], Tuple.Tail[Left]] =
        new Conn(sent :* message)

    def connect(): this.Conn[EmptyTuple, P] = Conn(EmptyTuple)

    def close(conn: this.Conn[P, EmptyTuple]): P = conn.sent
