package typeprog.ch01foundations

/** Exercise 04 — a covariant stack. *Solved.*
  *
  * `push` is the whole exercise. Written as `def push(a: A): Stack[A]` it puts
  * an `A` into a covariant structure, which the get/put principle forbids and
  * the compiler rejects outright: "covariant type A occurs in contravariant
  * position".
  *
  * The lower bound is the standard repair. `B >: A` says: accept anything at
  * least as wide as what is already in here, and hand back a stack of that
  * wider type. Nothing is ever put into the original stack — a new, wider one
  * is returned — so the covariance is never violated. Pushing a `Cat` onto a
  * `Stack[Dog]` does not corrupt it; it produces a `Stack[Animal]`, because
  * `Animal` is the least upper bound the compiler can infer for `B`.
  *
  * `empty` falls out of the same annotation. With `+A`, a `Stack[Nothing]` is
  * a `Stack[X]` for every `X` — `Nothing` is a subtype of everything — so one
  * value serves as the empty stack of every element type, which is exactly why
  * `List.empty` and `Nil` can coexist.
  */
object Exercise04:

  final class Stack[+A] private (private val items: List[A]):
    def push[B >: A](b: B): Stack[B] = new Stack(b :: items)

    def peek: Option[A] = items.headOption

    def size: Int = items.size

  object Stack:
    val empty: Stack[Nothing] = new Stack(Nil)
