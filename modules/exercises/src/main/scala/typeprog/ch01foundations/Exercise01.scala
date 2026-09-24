package typeprog.ch01foundations

/** Exercise 01 — annotate the variance.
  *
  * Three type constructors over the same `A`, each using it differently. As
  * shipped they are all invariant, which is the answer that is never wrong and
  * never useful: `Source[Dog]` and `Source[Animal]` are unrelated types, so a
  * producer of dogs cannot be handed to anything that asked for a producer of
  * animals.
  *
  * Annotate each parameter with the strongest variance its usage allows.
  * `Exercise01Spec` pins both directions: what must start conforming, and what
  * must still be rejected afterwards.
  *
  * Hint: the answer is written on each trait already — look at whether `A`
  * appears as a result type or as a parameter type.
  */
object Exercise01:

  /** Produces an `A`. Never consumes one. */
  trait Source[A]: // TODO: variance
    def emit(): A

  /** Consumes an `A`. Never produces one. */
  trait Sink[A]: // TODO: variance
    def accept(a: A): Unit

  /** Does both. Think before annotating this one — the spec expects a
    * particular answer, and it is not "whatever makes the first two work".
    */
  trait Channel[A] extends Source[A], Sink[A] // TODO: variance
