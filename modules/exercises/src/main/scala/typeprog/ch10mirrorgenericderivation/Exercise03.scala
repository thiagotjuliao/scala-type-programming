package typeprog.ch10mirrorgenericderivation

import scala.deriving.Mirror

/** Exercise 03 — every value of an enum.
  *
  * An enum whose cases have no parameters — `enum Light { case Red, Amber,
  * Green }` — has exactly as many values as it has cases. Make `derives
  * Values` list them:
  *
  *   - `summon[Values[Light]].all` is `List(Red, Amber, Green)`, in the order
  *     the cases are declared;
  *   - a sealed trait whose subtypes are all case objects works the same way;
  *   - a type with a case that has parameters — `case Circle(radius: Double)`
  *     — has no list of values, and `Values.derived` does not compile for it,
  *     with an error that says `singleton`.
  *
  * Hint: a case with no parameters has a singleton type, and a singleton type
  * has a given that holds its only value. Walk the cases the sum's mirror
  * lists, and ask each one for that.
  */
object Exercise03:

  trait Values[A]:
    def all: List[A]

  object Values:
    /** TODO: every value of `A`, or a compile error. */
    inline def derived[A](using m: Mirror.SumOf[A]): Values[A] = ???
