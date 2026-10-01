package typeprog.ch10mirrorgenericderivation

import scala.deriving.Mirror
import scala.compiletime.*

/** Exercise 02 — `Show` for any case class.
  *
  * Make `derives Show` work on a case class, rendering it as its name and its
  * fields, each shown by its own `Show`:
  *
  *   - `Person("Ann", 3)` is `Person(name = "Ann", age = 3)` — the `String` in
  *     quotes, because that is what `Show[String]` does;
  *   - a case class with no fields is `Empty()`;
  *   - a field that is itself a case class is shown the same way, whether its
  *     class `derives Show` or not: a class with no instance is derived on the
  *     way;
  *   - a field whose type has no `Show` and is not a case class makes the
  *     whole class underivable: `Show.derived` does not compile for it.
  *
  * Hint: `derived` runs at each use, where the mirror's members are known.
  * Walk the field types, take an instance for each — found, or derived — and
  * build the instance in an ordinary method, not inside the `inline` one.
  */
object Exercise02:

  trait Show[A]:
    def show(a: A): String

  object Show:
    given Show[Int] = _.toString
    given Show[Boolean] = _.toString
    given Show[String] = s => "\"" + s + "\""

    /** TODO: a `Show` for any case class. */
    inline def derived[A](using m: Mirror.ProductOf[A]): Show[A] =
      val name = constValue[m.MirroredLabel]
      val labels = constValueTuple[m.MirroredElemLabels].toList.map(_.toString)
      val elems = instances[m.MirroredElemTypes]
      product(name, labels, elems)

    inline def instances[T <: Tuple]: List[Show[Any]] = inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t) => instanceFor[h].asInstanceOf[Show[Any]] :: instances[t]

    inline def instanceFor[H]: Show[H] = summonFrom {
      case s: Show[H] => s
      case m: Mirror.ProductOf[H] => derived[H](using m)
    }

    def product[A](name: String, labels: List[String], elems: => List[Show[Any]]): Show[A] =
      lazy val shows = elems

      (a: A) =>
        val values = a.asInstanceOf[Product].productIterator.toList

        val tokens = labels
          .lazyZip(shows)
          .lazyZip(values)
          .map: (l, e, v) =>
            s"$l = ${e.show(v)}"

        tokens.mkString(s"$name(", ", ", ")")
  end Show
end Exercise02
