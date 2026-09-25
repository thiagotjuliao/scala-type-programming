package typeprog.ch10mirrorgenericderivation

import scala.deriving.Mirror

/** Exercise 04 — a case class from the tuple of its fields, and back. *Solved.*
  *
  * `m.MirroredElemTypes` is the tuple of `T`'s field types, and it is a type
  * member of the mirror `m` — so a parameter declared after `m` can have it as
  * its type (chapter 03). `build[Person]` then takes exactly a `(String, Int)`;
  * anything else is an ordinary type mismatch, reported field by field.
  * `fromProduct` builds the instance: a tuple is a `Product`, with the fields
  * in order.
  *
  * The mirror is asked for first, in its own `using` clause, and the tuple
  * second: the tuple's type depends on it. `build[Person](("Ann", 3))` still
  * reads naturally, the `using` clause being filled in by the compiler.
  *
  * `fields` goes the other way with `Tuple.fromProductTyped`, whose result is
  * typed by the same member.
  */
object Exercise04:

  def build[T](using m: Mirror.ProductOf[T])(fields: m.MirroredElemTypes): T =
    m.fromProduct(fields)

  def fields[T <: Product](t: T)(using m: Mirror.ProductOf[T]): m.MirroredElemTypes =
    Tuple.fromProductTyped(t)
