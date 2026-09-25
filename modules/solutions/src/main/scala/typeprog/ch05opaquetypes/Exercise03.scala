package typeprog.ch05opaquetypes

/** Exercise 03 — only the calls that make sense in each state. *Solved.*
  *
  * The class keeps what every state shares — `host`, `sent` — and each
  * transition moves out into an extension for the one application of
  * `Connection` it belongs to. `open` exists on a `Connection[Closed]` and
  * nowhere else; `send` and `close` exist on a `Connection[Open]`. A call in the
  * wrong state is then not a failed check but a missing method, and the message
  * names the state the call was made in: *"value send is not a member of
  * Connection[Closed]"*. Declared in the companion, the extensions are found
  * wherever a `Connection` is, with no import.
  *
  * `Open` and `Closed` are labels, not data. No value of either type is ever
  * created, and at runtime every `Connection` is the same class with the same
  * two fields; the state is checked entirely at compile time and erased after.
  * That also means it cannot be recovered at runtime — `case c:
  * Connection[Open]` is an unchecked test that would match a closed one too.
  *
  * The alternative keeps the methods in the class and asks for evidence:
  * `def send(message: String)(using S =:= Open): Connection[Open]`. It is
  * equally sound, and its error is shorter — *"Cannot prove that Closed =:=
  * Open."* — at the price of an extra parameter list on every method.
  *
  * The constructor stays private, so `Connection.to` is the only way in, and
  * it always starts `Closed`.
  */
object Exercise03:

  sealed trait State
  sealed trait Open extends State
  sealed trait Closed extends State

  final class Connection[S <: State] private (val host: String, val sent: List[String])

  object Connection:
    def to(host: String): Connection[Closed] = Connection(host, Nil)

    extension (c: Connection[Closed]) def open: Connection[Open] = Connection(c.host, c.sent)

    extension (c: Connection[Open])
      def send(message: String): Connection[Open] = Connection(c.host, c.sent :+ message)
      def close: Connection[Closed] = Connection(c.host, c.sent)
