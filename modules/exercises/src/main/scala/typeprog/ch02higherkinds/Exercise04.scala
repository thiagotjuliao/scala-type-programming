package typeprog.ch02higherkinds

import typeprog.core.Unsolved

/** Exercise 04 — compose two type constructors.
  *
  * A `List[Option[Int]]` has two layers, and mapping the `Int`s means mapping
  * twice: `xs.map(_.map(f))`. The two layers are also a single type
  * constructor, `List` of `Option` of a hole, and that constructor has a
  * `Functor` built from the two it is made of.
  *
  * Define `Compose` so that `Compose[F, G][A]` is `F[G[A]]` — `F` outside, `G`
  * inside. Then implement `composed`, which builds the functor for
  * `Compose[F, G]` from a functor for each layer, using nothing but their
  * `map`s.
  *
  * Hint: `Compose` takes two type constructors and returns a third, so its
  * right-hand side is a type lambda with a single parameter. `composed`'s `map`
  * is one call to `F.map` whose function argument is a call to `G.map`.
  */
object Exercise04:

  /** TODO: replace `Unsolved` with `F` applied to `G` applied to the hole. */
  type Compose[F[_], G[_]] = [X] =>> Unsolved

  /** TODO: implement. */
  def composed[F[_], G[_]](F: Functor[F], G: Functor[G]): Functor[Compose[F, G]] = ???
