package typeprog.ch03pathdependenttypes

/** Exercise 03 — keep the dependency in a function value. *Solved.*
  *
  * `(k: Key) => String => Option[k.Value]` is a function type whose parameter
  * has a name, so its result can select through it. It is still an ordinary
  * `Function1` underneath — a refinement of one whose `apply` has a dependent
  * result — so it can be a `val`, a parameter and a return value, which is all
  * this exercise does with it.
  *
  * `parser`'s body is just `k => k.parse`, and the body was never the problem:
  * without the annotation, the lambda's type would have been inferred as the
  * dependent one. What threw it away in the stub was *writing down*
  * `Key => String => Option[Any]` — an ascription is a promise to forget
  * everything the declared type does not say.
  *
  * `loadWith` takes one dependent function and returns another. The body is the
  * obvious one — look the name up, parse with the reader chosen for *this* key
  * — and it type-checks because `read(k)` is a `String => Option[k.Value]` for
  * the very `k` the result is about.
  *
  * The dependent *parameter* type is what refuses the reader that ignores its
  * key. `(k: Key) => (s: String) => port.parse(s)` returns an `Option[Int]` for
  * every `k`, and an `Option[Int]` is not an `Option[k.Value]` for an arbitrary
  * `k`: `Found: Option[port.Value], Required: Option[k.Value]`. With the stub's
  * non-dependent type, `Key => String => Option[Any]`, that reader was
  * accepted, and would have answered the `host` key with an `Int`.
  */
object Exercise03:

  val parser: (k: Key) => String => Option[k.Value] = k => k.parse

  def loadWith(raw: Map[String, String])(
      read: (k: Key) => String => Option[k.Value]
  ): (k: Key) => Option[k.Value] =
    k => raw.get(k.name).flatMap(read(k))
