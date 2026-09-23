package typeprog.ch02higherkinds

/** Exercise 01 — declare each parameter at the kind its members need.
  *
  * Three traits, each shipped with a type parameter of kind `*` — a plain
  * type, which is the one kind none of them can use. Change each declaration so
  * the trait accepts exactly the arguments described, and add the member it
  * describes:
  *
  *   - `Mappable` is given a one-hole type constructor such as `List` or
  *     `Option`, and has `map`, which turns an `F` of `A`s into an `F` of `B`s
  *     given an `A => B`;
  *   - `BiMappable` is given a two-hole one such as `Either` or `Tuple2`, and
  *     has `bimap`, which changes both element types at once, one function for
  *     each side;
  *   - `Instances` is given something shaped like `Mappable` itself — a type
  *     constructor whose argument is a type constructor — and has `forList`,
  *     which hands back that thing applied to `List`.
  *
  * The spec also checks the rejections: a `Mappable[Int]`, a `Mappable[Either]`
  * or an `Instances[List]` must not compile.
  *
  * Hint: an underscore in a type parameter list stands for a parameter nobody
  * names. `F[_]` has one; count the underscores the other two need, and nest
  * them where the argument is itself a constructor.
  */
object Exercise01:

  /** TODO: kind, and `map`. */
  trait Mappable[F]

  /** TODO: kind, and `bimap`. */
  trait BiMappable[F]

  /** TODO: kind, and `forList`. */
  trait Instances[TC]
