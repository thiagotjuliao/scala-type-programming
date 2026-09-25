package typeprog.ch11evidencetypesafestatemachines

import scala.annotation.implicitNotFound

/** Exercise 03 — a builder that cannot be misused. *Solved.*
  *
  * The two type parameters are the builder's state, as `Boolean` literal
  * types. Each setter returns the builder with its fact set to `true`, and
  * asks that it was `false` — so each can be called once, in any order.
  * `build` asks for both facts. At runtime there is one immutable object and
  * three plain methods; the parameters and the evidence are erased.
  *
  * The facts could be asked for as `HasUrl =:= true`, and every misuse would
  * be rejected — with *"Cannot prove that (false : Boolean) =:= (true :
  * Boolean)."*, which does not say what is missing. Four small type classes,
  * one per fact, each with an instance for the one value that satisfies it and
  * an `@implicitNotFound` message, say it in the caller's terms.
  *
  * The constructor is private: a `RequestBuilder[true, true]` made directly
  * would skip every check. The only way in is `RequestBuilder()`, which starts
  * with nothing set.
  */
object Exercise03:

  final case class Request(method: String, url: String)

  @implicitNotFound("the request has no URL yet: call url(…) before build")
  sealed trait UrlSet[B <: Boolean]

  object UrlSet:
    given UrlSet[true] = new UrlSet[true] {}

  @implicitNotFound("the request has no method yet: call method(…) before build")
  sealed trait MethodSet[B <: Boolean]

  object MethodSet:
    given MethodSet[true] = new MethodSet[true] {}

  @implicitNotFound("the URL is already set")
  sealed trait UrlUnset[B <: Boolean]

  object UrlUnset:
    given UrlUnset[false] = new UrlUnset[false] {}

  @implicitNotFound("the method is already set")
  sealed trait MethodUnset[B <: Boolean]

  object MethodUnset:
    given MethodUnset[false] = new MethodUnset[false] {}

  final class RequestBuilder[HasUrl <: Boolean, HasMethod <: Boolean] private (
      url: String,
      method: String
  ):

    def url(u: String)(using UrlUnset[HasUrl]): RequestBuilder[true, HasMethod] =
      new RequestBuilder(u, method)

    def method(m: String)(using MethodUnset[HasMethod]): RequestBuilder[HasUrl, true] =
      new RequestBuilder(url, m)

    def build(using UrlSet[HasUrl], MethodSet[HasMethod]): Request = Request(method, url)

  object RequestBuilder:
    def apply(): RequestBuilder[false, false] = new RequestBuilder("", "")
end Exercise03
