package typeprog.ch01foundations

/** Exercise 04 — a covariant stack.
  *
  * Variance and bounds, together, on the structure where they most often
  * collide. `Stack` is immutable, so there is no reason for `Stack[Dog]` not
  * to be usable as a `Stack[Animal]` — yet as shipped it is invariant, and
  * `empty` has to be told which element type it is empty *of*.
  *
  * Two changes to make:
  *
  *   1. make `A` covariant;
  *   2. keep `push` compiling afterwards — it puts an `A` in, which covariance
  *      forbids, so it needs a lower-bounded parameter of its own that lets a
  *      push of a wider type return the wider stack.
  *
  * Then make `empty` a single value rather than a method: with a covariant
  * `A`, one `Stack[Nothing]` is already a `Stack[X]` for every `X`.
  *
  * Hint: `def push[B >: A](b: B): Stack[B]`.
  */
object Exercise04:

  final class Stack[A] private (private val items: List[A]): // TODO: variance
    /** TODO: widen this so it still compiles once `A` is covariant. */
    def push(a: A): Stack[A] = new Stack(a :: items)

    def peek: Option[A] = items.headOption

    def size: Int = items.size

  object Stack:
    /** TODO: make this a `val` of type `Stack[Nothing]`. */
    def empty[A]: Stack[A] = new Stack(Nil)
