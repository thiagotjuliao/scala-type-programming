package typeprog.ch02higherkinds

/** Exercise 03 — a functor over the other side. *Solved.*
  *
  * The definition is the small change it looks like: the lambda's parameter
  * moves to the left of `Either`. The body swaps sides the same way, and
  * `Either#left.map` is the library's way of mapping a `Left`.
  *
  * What the exercise is really about is the spec's inference claims. The
  * functor is perfectly valid, and the compiler will check it against any `F`
  * it is given — but it will never *infer* this `F`. Handed an
  * `Either[Int, String]` where `F[Int]` is expected, it matches `Int` against
  * the last parameter, finds `String`, and stops: "Found: Either[Int, String],
  * Required: F[Int]". There is no search over other ways to cut the type in
  * two. Spelling `F` out, `inc[OnLeft[String]](...)`, is the only way to use
  * it.
  *
  * That is not an accident of this example. It is why libraries put the
  * parameter that varies last — `Either[E, A]`, `Kleisli[F, A, B]`, every
  * effect type with an error channel — and why an instance for any other hole
  * comes with a type annotation at every call site.
  */
object Exercise03:

  type OnLeft[E] = [X] =>> Either[X, E]

  def leftFunctor[E]: Functor[OnLeft[E]] = new Functor[OnLeft[E]]:
    def map[A, B](fa: Either[A, E])(f: A => B): Either[B, E] = fa.left.map(f)
