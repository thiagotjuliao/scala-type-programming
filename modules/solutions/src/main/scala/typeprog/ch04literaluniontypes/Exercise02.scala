package typeprog.ch04literaluniontypes

/** Exercise 02 — keep the caller's type through a chain. *Solved.*
  *
  * `this.type` is the singleton type of the receiver: for `val r =
  * JsonRequest()`, `r.header(...)` is an `r.type`, which is a `JsonRequest`.
  * Declared as `: Request`, the same method told every caller "some request",
  * and a subclass's own methods were gone after the first link. Nothing in the
  * bodies changes — they already returned `this`; the result type is what
  * promised less than they did.
  *
  * `withHeaders` does from the outside what `this.type` does from the inside.
  * `r` is a parameter, so a stable path, so `r.type` is a type the result can
  * be declared as: the very value that came in. A `JsonRequest` passed in comes
  * back as that `JsonRequest`; a `Request` comes back as a `Request`, and still
  * has no `body`.
  *
  * A type parameter, `def withHeaders[R <: Request](r: R)(...): R`, is a
  * close alternative rather than a near miss: it passes this spec too. Where
  * the caller expects an `r.type`, inference simply picks `R = r.type`. The
  * difference is in what each signature says on its own. `R` is "a value of
  * the caller's type", and it takes an expected type to narrow it to this
  * value; `r.type` says "the value you passed in" to every caller, with nothing
  * to infer.
  *
  * The real near miss is keeping `: Request` on `header` and fixing only
  * `withHeaders`. Then `withHeaders(r)` is an `r.type`, but the `header` calls
  * inside a caller's chain still hand back a `Request`, and the first test
  * fails exactly as the stub did.
  */
object Exercise02:

  class Request:
    private var recorded = List.empty[(String, String)]

    def headers: List[(String, String)] = recorded

    def header(name: String, value: String): this.type =
      recorded = recorded :+ (name -> value)
      this

  final class JsonRequest extends Request:
    private var json = ""

    def payload: String = json

    def body(content: String): this.type =
      json = content
      this

  def withHeaders(r: Request)(pairs: (String, String)*): r.type =
    pairs.foreach((name, value) => r.header(name, value))
    r
