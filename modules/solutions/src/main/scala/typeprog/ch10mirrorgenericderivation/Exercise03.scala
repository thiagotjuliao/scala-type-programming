package typeprog.ch10mirrorgenericderivation

import scala.compiletime.{erasedValue, error, summonFrom}
import scala.deriving.Mirror

/** Exercise 03 — every value of an enum. *Solved.*
  *
  * A sum's mirror lists its cases in `MirroredElemTypes`, in the order they
  * are declared. A case with no parameters — an enum's `case Red`, a `case
  * object` — has a *singleton* type, `Light.Red.type`, and every singleton
  * type has a `ValueOf` given holding its one value. So the values are found by
  * walking the cases and asking each for its `ValueOf`.
  *
  * A case with parameters, `Circle(radius: Double)`, is a class with any
  * number of values; there is no `ValueOf[Circle]`, the `summonFrom` reaches
  * its last case, and `error` stops the derivation with a message that says
  * why.
  *
  * `derived` asks for a `Mirror.SumOf`, so a case class — a product — is
  * rejected before any of this runs.
  */
object Exercise03:

  trait Values[A]:
    def all: List[A]

  object Values:

    inline def derived[A](using m: Mirror.SumOf[A]): Values[A] =
      of(singletons[m.MirroredElemTypes, A])

    inline def singletons[T <: Tuple, A]: List[A] = inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t) => valueOf[h].asInstanceOf[A] :: singletons[t, A]

    inline def valueOf[H]: H = summonFrom {
      case v: ValueOf[H] => v.value
      case _ => error("Values needs every case to be a singleton, with no parameters")
    }

    def of[A](values: List[A]): Values[A] = new Values[A]:
      def all: List[A] = values
