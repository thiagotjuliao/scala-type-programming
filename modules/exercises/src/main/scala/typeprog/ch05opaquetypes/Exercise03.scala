package typeprog.ch05opaquetypes

/** Exercise 03 — only the calls that make sense in each state.
  *
  * A `Connection` is opened, used, closed. As shipped, every method is
  * available in every state, so `Connection.to("db").send("hi")` — sending on a
  * connection nobody opened — compiles. The state is already in the type, as a
  * phantom parameter: `Open` and `Closed` are never instantiated, and a
  * `Connection[Open]` is the same object at runtime as a `Connection[Closed]`.
  *
  * Make the compiler enforce the order:
  *
  *   - `Connection.to(host)` gives a `Connection[Closed]`;
  *   - `open` is available only on a closed connection, and gives an open one;
  *   - `send` is available only on an open connection, and keeps it open;
  *   - `close` is available only on an open connection, and gives a closed one.
  *
  * The bodies are right as they are; what changes is where each method is
  * available. `sent` and `host` stay readable in every state.
  *
  * Hint: an extension method can be declared for one particular application
  * of a generic type. Alternatively, a method can ask for evidence about its
  * class's type parameter.
  */
object Exercise03:

  sealed trait State
  sealed trait Open extends State
  sealed trait Closed extends State

  final class Connection[S <: State] private (val host: String, val sent: List[String]):

    /** TODO: only when closed. */
    def open: Connection[Open] = Connection(host, sent)

    /** TODO: only when open. */
    def send(message: String): Connection[S] = Connection(host, sent :+ message)

    /** TODO: only when open. */
    def close: Connection[Closed] = Connection(host, sent)

  object Connection:
    def to(host: String): Connection[Closed] = Connection(host, Nil)
