package typeprog.ch10mirrorgenericderivation

import scala.compiletime.{constValue, constValueTuple}
import scala.deriving.Mirror

/** Exercise 01 — a type's name and its field names. *Solved.*
  *
  * The compiler synthesises a `Mirror` for a case class, an enum and an enum's
  * case, and its members are the shape as types: `MirroredLabel` is the name as
  * a literal type, `"Person"`; `MirroredElemLabels` is the field names as a
  * tuple of literal types, `("name", "age")`.
  *
  * `constValue` reads a literal type back as a value, and `constValueTuple`
  * does it for a whole tuple of them. The mirror arrives as a `using`
  * parameter, typed with the refinement the compiler synthesised, so
  * `m.MirroredLabel` *is* `"Person"` in the expansion. The near miss is `val m
  * = summonInline[Mirror.Of[T]]` in the body: the local `val` holds the mirror
  * with the type it declares, the members are abstract again, and the call
  * fails with *"m.MirroredLabel is not a constant type; cannot take
  * constValue"*.
  *
  * The cast in `fieldNames` is there because `toList` on a tuple of `"name"`
  * and `"age"` is typed by the union of the element types; every element is a
  * `String`, but the type says so only through that union.
  *
  * A class that is not a case class has no mirror, and the search fails at
  * the call: *"class Plain is not a generic product because it is not a case
  * class"*.
  */
object Exercise01:

  inline def typeName[T](using m: Mirror.Of[T]): String =
    constValue[m.MirroredLabel]

  inline def fieldNames[T](using m: Mirror.ProductOf[T]): List[String] =
    constValueTuple[m.MirroredElemLabels].toList.asInstanceOf[List[String]]
