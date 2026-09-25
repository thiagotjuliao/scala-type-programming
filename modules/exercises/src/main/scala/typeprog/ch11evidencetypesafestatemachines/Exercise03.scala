package typeprog.ch11evidencetypesafestatemachines

/** Exercise 03 — a builder that cannot be misused.
  *
  * `RequestBuilder` collects a URL and a method, and builds a `Request`. Make
  * its misuses compile errors:
  *
  *   - `build` compiles only once both are set, in whichever order;
  *   - without a URL, the error says `no URL`; without a method, `no method`;
  *   - each can be set once: setting the URL, or the method, a second time
  *     does not compile.
  *
  * The builder stays immutable, and its two type parameters say what has been
  * set so far: `RequestBuilder()` is a `RequestBuilder[false, false]`.
  *
  * Hint: the setters can demand a fact about the parameters and return the
  * builder with that fact changed. For the messages, `=:=` evidence is correct
  * and unreadable — a type class of your own, with an instance only for
  * `true`, can have a message of its own.
  */
object Exercise03:

  final case class Request(method: String, url: String)

  final class RequestBuilder[HasUrl <: Boolean, HasMethod <: Boolean] private (
      url: String,
      method: String
  ):

    /** TODO: once only. */
    def url(u: String): RequestBuilder[true, HasMethod] = ???

    /** TODO: once only. */
    def method(m: String): RequestBuilder[HasUrl, true] = ???

    /** TODO: only with both set, saying which one is missing. */
    def build: Request = ???

  object RequestBuilder:
    def apply(): RequestBuilder[false, false] = new RequestBuilder("", "")
