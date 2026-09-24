package typeprog.ch02higherkinds

import typeprog.core.Unsolved

/** Exercise 03 — a functor over the other side.
  *
  * Exercise 02 fixed the left side of `Either` and mapped over the right. Do
  * the mirror image: define `OnLeft` so that `OnLeft[E][A]` is `Either[A, E]` —
  * the error type now on the right, the mapped type on the left — and give
  * `leftFunctor` a body that applies the function to a `Left` and passes a
  * `Right` through.
  *
  * The type lambda is a one-line change from exercise 02. What changes more is
  * how it is used. Read the last two type-level assertions in `Exercise03Spec`
  * before starting: one calls the generic method with `F` spelled out, and one
  * without, and they are expected to disagree.
  *
  * Hint: the order of the arguments inside the lambda's body is the only thing
  * that differs from `ErrorOr`.
  */
object Exercise03:

  /** TODO: replace `Unsolved` with `Either`, `E` on the right. */
  type OnLeft[E] = [X] =>> Unsolved

  /** TODO: implement. */
  def leftFunctor[E]: Functor[OnLeft[E]] = ???
