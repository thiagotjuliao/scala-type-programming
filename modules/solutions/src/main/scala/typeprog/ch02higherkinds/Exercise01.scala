package typeprog.ch02higherkinds

/** Exercise 01 — declare each parameter at the kind its members need.
  * *Solved.*
  *
  * Each declaration is read straight off the member that uses it. `map` writes
  * `F[A]` and `F[B]`, so `F` is applied to one argument and has kind `* -> *`:
  * `F[_]`. `bimap` writes `F[A, B]`, so `F[_, _]`: `* -> * -> *`. And
  * `forList` writes `TC[List]` — `TC` is applied to a type *constructor* — so
  * `TC`'s own parameter is declared as one: `TC[_[_]]`, of kind
  * `(* -> *) -> *`.
  *
  * The rejections come for free, and that is the point. A kind is checked
  * before anything about the argument's members is: `Mappable[Int]` fails on
  * arity alone, with "Type argument Int does not have the same kind as its
  * bound", long before the compiler would have noticed that `Int` has no `map`.
  *
  * `Instances[Mappable]` is the line worth pausing on. `Mappable` is a type
  * constructor over type constructors, and `Instances` is a type constructor
  * over *those* — the kinds nest exactly as far as higher-order functions do,
  * and there is nothing special about any particular level.
  */
object Exercise01:

  trait Mappable[F[_]]:
    def map[A, B](fa: F[A])(f: A => B): F[B]

  trait BiMappable[F[_, _]]:
    def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D]

  trait Instances[TC[_[_]]]:
    def forList: TC[List]
