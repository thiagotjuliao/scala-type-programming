package typeprog.ch02higherkinds

/** Exercise 02 — partially apply `Either` with a type lambda.
  *
  * `Either` has two holes and `Functor` wants one, so `Functor[Either]` is a
  * kind error. The fix is to fill one of them. Define `ErrorOr` so that
  * `ErrorOr[E]` is the one-hole constructor "`Either` with `E` on the left", and
  * `ErrorOr[String][Int]` is exactly `Either[String, Int]`.
  *
  * Then give `eitherFunctor` a body: `map` applies the function to a `Right`
  * and passes a `Left` through untouched.
  *
  * The spec cares about more than the definition. It calls a method generic in
  * `F[_]` with a plain `Either[String, Int]` and your functor, and expects the
  * compiler to infer `F` by itself — which it does only if `ErrorOr` leaves the
  * hole where inference looks for one.
  *
  * Hint: `[X] =>> ...` is a type constructor written inline, and an alias with
  * its own parameter list can return one.
  */
object Exercise02:

  /** TODO: replace `Unsolved` with `Either`, `E` on the left. */
  type ErrorOr[E] = [X] =>> Either[E, X]

  /** TODO: implement. */
  def eitherFunctor[E]: Functor[ErrorOr[E]] = new Functor[ErrorOr[E]]:
    def map[A, B](fa: ErrorOr[E][A])(f: A => B): ErrorOr[E][B] =
      fa match
        case Right(a) => Right(f(a))
        case Left(e) => Left(e)
