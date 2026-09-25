package typeprog.ch10mirrorgenericderivation

import scala.compiletime.{constValue, constValueTuple, erasedValue, summonFrom}
import scala.deriving.Mirror

/** Exercise 02 — `Show` for any case class. *Solved.*
  *
  * `derived` runs at each `derives Show`, where `A` is a concrete case class
  * and its mirror's members are known. It reads three things from the mirror:
  * the name (`MirroredLabel`), the field names (`MirroredElemLabels`), and an
  * instance for every field type (`MirroredElemTypes`, walked by `instances`).
  *
  * `instances` is chapter 09's recursion over a tuple type. For each field
  * type, `summonFrom` takes the `Show` that exists — the one in this companion
  * for `Int`, the one `derives` put in `Person`'s companion — and otherwise,
  * if the type has a mirror, derives one on the spot: that is how `Address`,
  * which derives nothing, is shown. With neither, the `summonFrom` has no case
  * that applies and the derivation fails where it is asked for.
  *
  * The instances are passed *by name* to `product`, an ordinary method, and
  * evaluated there on first use. That is not needed for these classes and is
  * what keeps a recursive one — a `Tree` with `Tree` fields — from reading its
  * own instance before it exists (*"Infinite loop in function body"*). Building
  * the instance in an ordinary method also keeps its anonymous class out of the
  * `inline` body, where it would be copied into every derivation.
  */
object Exercise02:

  trait Show[A]:
    def show(a: A): String

  object Show:
    given Show[Int] = _.toString
    given Show[Boolean] = _.toString
    given Show[String] = s => "\"" + s + "\""

    inline def derived[A](using m: Mirror.ProductOf[A]): Show[A] =
      product(
        constValue[m.MirroredLabel],
        constValueTuple[m.MirroredElemLabels].toList.asInstanceOf[List[String]],
        instances[m.MirroredElemTypes]
      )

    inline def instances[T <: Tuple]: List[Show[Any]] = inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t) => instanceFor[h].asInstanceOf[Show[Any]] :: instances[t]

    inline def instanceFor[H]: Show[H] = summonFrom {
      case s: Show[H] => s
      case m: Mirror.ProductOf[H] => derived[H](using m)
    }

    def product[A](name: String, labels: List[String], elems: => List[Show[Any]]): Show[A] =
      lazy val shows = elems
      a =>
        val values = a.asInstanceOf[Product].productIterator.toList
        val fields = labels.lazyZip(values).lazyZip(shows).map((l, v, s) => s"$l = ${s.show(v)}")
        fields.mkString(s"$name(", ", ", ")")
  end Show
end Exercise02
