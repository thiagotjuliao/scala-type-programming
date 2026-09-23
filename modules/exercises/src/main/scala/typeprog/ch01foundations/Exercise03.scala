package typeprog.ch01foundations

/** Exercise 03 — put the evidence to work.
  *
  * Each method below is handed a witness it cannot do its job without, and
  * each is currently unimplemented. Implement all three *using the witness* —
  * no casts, no `asInstanceOf`, no pattern matching on runtime classes.
  *
  * Hint: `A =:= B` extends `A <:< B`, which extends `A => B`. The witness is
  * already the conversion you need; it only has to be applied.
  */
object Exercise03:

  /** Converts an `A` to a `B`, given that they are the same type. */
  def coerce[A, B](a: A)(using ev: A =:= B): B = ev(a)

  /** Converts an `A` to a `B`, given that `A` conforms to `B`. */
  def upcast[A, B](a: A)(using ev: A <:< B): B = ev(a)

  /** Sums a list whose element type happens to be `Int`.
    *
    * The point of the exercise: `as` is a `List[A]`, and `A` is opaque to the
    * body. Only the witness can turn it into something summable.
    */
  def sum[A](as: List[A])(using ev: A =:= Int): Int = as.map(ev).sum
