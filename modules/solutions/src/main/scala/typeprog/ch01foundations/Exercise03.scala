package typeprog.ch01foundations

/** Exercise 03 — put the evidence to work. *Solved.*
  *
  * All three bodies are applications of the witness, because that is what the
  * witness is. The hierarchy is `A =:= B` extends `A <:< B` extends `A => B`,
  * so an evidence value is a function from the narrower type to the wider one
  * and `ev(a)` is the whole implementation.
  *
  * What makes this more than a trick: the conversion is erased at runtime —
  * `=:=` is only ever instantiated by the compiler's own `given`, and only
  * when the types really do line up. There is no cast here, and no way to get
  * one of these values by lying.
  */
object Exercise03:

  def coerce[A, B](a: A)(using ev: A =:= B): B = ev(a)

  def upcast[A, B](a: A)(using ev: A <:< B): B = ev(a)

  def sum[A](as: List[A])(using ev: A =:= Int): Int = as.map(ev.apply).sum
