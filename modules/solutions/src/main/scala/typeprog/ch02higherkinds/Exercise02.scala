package typeprog.ch02higherkinds

/** Exercise 02 — partially apply `Either` with a type lambda. *Solved.*
  *
  * `ErrorOr` is curried on purpose. Its own parameter list takes `E`; its
  * right-hand side is a type lambda that takes the rest. So `ErrorOr[String]`
  * is a complete, one-hole type constructor — the `* -> *` that `Functor`
  * asks for. The near miss is `type ErrorOr[E, X] = Either[E, X]`: same
  * meaning when fully applied, but it can only ever *be* fully applied, and
  * `Functor[ErrorOr[String]]` is then rejected for "Not enough type arguments".
  *
  * The inference claim in the spec is where the placement of the hole pays
  * off. Given `Either[String, Int]` where `F[Int]` is expected, the compiler
  * lines `Int` up with the *last* parameter and takes everything before it as
  * the constructor: `F = [X] =>> Either[String, X]`. That is `ErrorOr[String]`
  * — type lambdas are compared structurally, not by name — so the functor
  * fits without the caller ever mentioning `F`.
  *
  * The body is ordinary. `Either` is right-biased, so its own `map` already
  * does exactly this, and delegating to it is the whole implementation.
  */
object Exercise02:

  type ErrorOr[E] = [X] =>> Either[E, X]

  def eitherFunctor[E]: Functor[ErrorOr[E]] = new Functor[ErrorOr[E]]:
    def map[A, B](fa: Either[E, A])(f: A => B): Either[E, B] = fa.map(f)
