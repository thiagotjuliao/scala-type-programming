package typeprog.ch02higherkinds

/** Exercise 04 — compose two type constructors. *Solved.*
  *
  * `Compose` is a type-level function whose arguments are type-level
  * functions: it takes two of kind `* -> *` and returns a third. Written out,
  * its kind is `(* -> *) -> (* -> *) -> (* -> *)`, the same shape as `compose`
  * on ordinary functions, one level up. The order inside the lambda is the
  * whole definition — `F[G[X]]`, not `G[F[X]]` — and the spec checks that the
  * other order is rejected, because the two are different types with different
  * functors.
  *
  * `composed` is the payoff for having the abstraction at all. It is written
  * once, for every pair of functors, and it never learns what `F` and `G` are:
  * the outer `map` visits each `G[A]`, the inner `map` visits each `A`. A
  * `List` of `Option`s, an `Option` of `Either`s, a function returning a list —
  * each gets its functor from this one definition.
  *
  * Note the value parameters named after the type parameters. `F: Functor[F]`
  * is the usual convention, and harmless: types and terms live in separate
  * namespaces, so `F.map` is the value and `F[A]` is the type.
  */
object Exercise04:

  type Compose[F[_], G[_]] = [X] =>> F[G[X]]

  def composed[F[_], G[_]](F: Functor[F], G: Functor[G]): Functor[Compose[F, G]] =
    new Functor[Compose[F, G]]:
      def map[A, B](fga: F[G[A]])(f: A => B): F[G[B]] = F.map(fga)(ga => G.map(ga)(f))
