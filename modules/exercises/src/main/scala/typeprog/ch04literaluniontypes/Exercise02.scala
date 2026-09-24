package typeprog.ch04literaluniontypes

/** Exercise 02 — keep the caller's type through a chain.
  *
  * `Request` records headers and `JsonRequest` adds a body. Both methods return
  * the request they were called on, so they chain — until a `JsonRequest`
  * calls `header`, gets back a plain `Request`, and has lost `body`:
  *
  * {{{
  * JsonRequest().header("Accept", "json").body("{}")  // value body is not a member of Request
  * }}}
  *
  * Change the result types so that:
  *
  *   - `header` and `body` hand back the type of the very request they were
  *     called on — for `val r = JsonRequest()`, `r.header(...)` is an `r.type`;
  *   - `withHeaders(r)(pairs*)` adds every pair to `r`, in order, and returns
  *     `r` itself with `r`'s own type — so a `JsonRequest` passed in can still be
  *     given a `body` afterwards, and a `Request` still cannot.
  *
  * The bodies of `header` and `body` are already right. `withHeaders` needs one.
  *
  * Hint: `this` has a type of its own, narrower than the class it is in. A
  * parameter is a stable path, and so it has one too.
  */
object Exercise02:

  class Request:
    private var recorded = List.empty[(String, String)]

    def headers: List[(String, String)] = recorded

    /** TODO: the result type. */
    def header(name: String, value: String): Request =
      recorded = recorded :+ (name -> value)
      this

  final class JsonRequest extends Request:
    private var json = ""

    def payload: String = json

    /** TODO: the result type. */
    def body(content: String): JsonRequest =
      json = content
      this

  /** TODO: the result type, and the body. */
  def withHeaders(r: Request)(pairs: (String, String)*): Request = ???
