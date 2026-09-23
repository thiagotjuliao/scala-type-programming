package typeprog.ch02higherkinds

/** The one higher-kinded abstraction this chapter's exercises are stated
  * against, from exercise 02 on.
  *
  * Kept deliberately bare: no laws, no instances, no syntax. Everything the
  * chapter claims is a claim about the *shape* of `F`, and `map` is the
  * smallest member that needs `F` to be applied twice — once to `A`, once to
  * `B` — which is what forces it to be a type constructor rather than a type.
  */
trait Functor[F[_]]:
  def map[A, B](fa: F[A])(f: A => B): F[B]
