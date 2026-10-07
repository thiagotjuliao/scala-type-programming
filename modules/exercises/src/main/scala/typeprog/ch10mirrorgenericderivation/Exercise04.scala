package typeprog.ch10mirrorgenericderivation

import scala.deriving.Mirror

/** Exercise 04 — a case class from the tuple of its fields, and back.
  *
  *   - `build[Person](("Ann", 3))` is `Person("Ann", 3)`, and the tuple must be
  *     exactly the fields' types, in order: `build[Person]((3, "Ann"))` does
  *     not compile, and neither does a tuple that is too short;
  *   - `fields(p)` is the tuple of `p`'s fields, typed: `fields(Person("Ann",
  *     3))` is a `(String, Int)`;
  *   - a class that is not a case class has neither.
  *
  * As shipped, both take and return anything.
  *
  * Hint: the tuple of a case class's field types is a type member of its
  * mirror — and a parameter's type can be a member of an earlier parameter
  * (chapter 03).
  */
object Exercise04:

  def build[T](using m: Mirror.ProductOf[T])(fields: m.MirroredElemTypes): T =
    m.fromProduct(fields)

  def fields[T](t: T)(using m: Mirror.ProductOf[T]): m.MirroredElemTypes =
    Tuple.fromProduct(t.asInstanceOf[Product]).asInstanceOf[m.MirroredElemTypes]
