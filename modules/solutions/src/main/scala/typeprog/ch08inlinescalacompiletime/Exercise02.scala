package typeprog.ch08inlinescalacompiletime

import scala.compiletime.{erasedValue, error}

/** Exercise 02 — a default value for every type that has one. *Solved.*
  *
  * `erasedValue[T]` is a value of type `T` that is never computed: it exists to
  * be the scrutinee of an `inline match`, which only looks at its type. Each
  * case is a type test made while compiling, against the `T` of the call site,
  * and only the matching case is kept.
  *
  * `case _: (a, b)` binds the pair's component types, and the case recurses on
  * them. Because `default[a]` declares its result type `a`, the pair built from
  * the two recursive calls is an `(a, b)` — after expansion, `(Int, String)`
  * and so on. A pair with a component that has no default reaches `error` in
  * the recursive call, so it fails for the component's reason.
  *
  * The casts are the price of checking the body at the definition. There, `T`
  * is unknown and `0` is not a `T`; after expansion, `0.asInstanceOf[Int]` is a
  * cast from a type to itself. `Option[?]` and `List[?]` need no binder: the
  * default does not depend on what they hold.
  *
  * A runtime `match` on a value could not do this at all — there is no value,
  * and `List[Int]` and `List[String]` are one class at runtime.
  */
object Exercise02:

  inline def default[T]: T = inline erasedValue[T] match
    case _: Int => 0.asInstanceOf[T]
    case _: Long => 0L.asInstanceOf[T]
    case _: Double => 0.0.asInstanceOf[T]
    case _: Boolean => false.asInstanceOf[T]
    case _: String => "".asInstanceOf[T]
    case _: Option[?] => None.asInstanceOf[T]
    case _: List[?] => Nil.asInstanceOf[T]
    case _: (a, b) => (default[a], default[b]).asInstanceOf[T]
    case _ => error("no default value for this type")
