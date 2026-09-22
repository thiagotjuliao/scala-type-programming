package typeprog.ch01foundations

/** Exercise 01 — annotate the variance. *Solved.*
  *
  * The annotations are read straight off the usage. In `Source`, `A` appears
  * only as a result type, so it is in covariant position and may be `+A`. In
  * `Sink` it appears only as a parameter type — contravariant position — so it
  * may be `-A`.
  *
  * `Channel` has to stay invariant, and not as a compromise: it is what the
  * type genuinely is. Making it covariant would make `Channel[Dog]` a
  * `Channel[Animal]`, and its inherited `accept` would then let a `Cat` into
  * something whose `emit()` has promised to return dogs. Making it
  * contravariant breaks the mirror image of that. A type that both gets and
  * puts has no safe variance, which is the same reason `Array` and `var`
  * fields have none.
  */
object Exercise01:

  trait Source[+A]:
    def emit(): A

  trait Sink[-A]:
    def accept(a: A): Unit

  trait Channel[A] extends Source[A], Sink[A]
