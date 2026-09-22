package typeprog.ch01foundations

/** The subtyping hierarchy this chapter's exercises are stated against.
  *
  * Kept deliberately small: three types and one relation. Everything the
  * chapter claims is a claim about `Dog <: Animal`, and nothing else.
  */
trait Animal:
  def name: String

final case class Dog(name: String) extends Animal

final case class Cat(name: String) extends Animal
