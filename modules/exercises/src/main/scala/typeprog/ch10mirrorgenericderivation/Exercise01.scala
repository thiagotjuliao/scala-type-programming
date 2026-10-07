package typeprog.ch10mirrorgenericderivation

import scala.compiletime.*
import scala.deriving.*

/** Exercise 01 — a type's name and its field names.
  *
  *   - `typeName[T]` is the name of `T` as written in its definition:
  *     `typeName[Person]` is `"Person"`, and it works for an enum, and for an
  *     enum's case, too;
  *   - `fieldNames[T]` is the names of a case class's fields, in order:
  *     `fieldNames[Person]` is `List("name", "age")`, and `Nil` for a case
  *     class with none;
  *   - for a class the compiler cannot describe — one that is not a case
  *     class — neither compiles.
  *
  * Nothing here may look at a value: there is none, only the type.
  *
  * Hint: the compiler publishes a case class's shape as type members of a
  * given it synthesises, and chapter 08 reads literal types as values. A tuple
  * of literal types can be read in one go.
  */
object Exercise01:

  inline def typeName[T](using m: Mirror.Of[T]): String =
    constValue[m.MirroredLabel]

  inline def fieldNames[T](using m: Mirror.ProductOf[T]): List[String] =
    constValueTuple[m.MirroredElemLabels].toList.map(_.toString)
