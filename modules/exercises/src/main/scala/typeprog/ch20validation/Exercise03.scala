package typeprog.ch20validation

/** Exercise 03 — the abstraction: `Applicative`, for `Option` and for
  * `Validated`.
  *
  * `map2` and `pure` are not specific to `Validated`, and code written against
  * the two operations works for every type that has them. Give two types an
  * `Applicative`:
  *
  *   - `Option`, where `map2` is `None` as soon as either side is;
  *   - `Validated`, for every error type that has a `Semigroup` and for no
  *     other, where `map2` keeps the errors of both sides, as in exercise 02.
  *
  * Both must be found with no import: `summon[Applicative[Option]]` compiles
  * anywhere, and so does the summon of `Validated`'s instance for
  * `List[String]` errors.
  *
  * And one operation for every applicative at once: an `Applicative` is a
  * `Functor`, and its `map` can be written from `pure` and `map2` alone.
  *
  * `Validated`, `Semigroup` and the `map2` of exercise 02 are given, solved.
  *
  * Hint: chapter 02 — `Applicative` wants a type constructor of one parameter,
  * and `Validated` has two. Chapter 06 — an instance can have conditions.
  */
object Exercise03:

  enum Validated[+E, +A]:
    case Valid(value: A)
    case Invalid(errors: E)

    def map[B](f: A => B): Validated[E, B] = this match
      case Valid(a) => Valid(f(a))
      case Invalid(e) => Invalid(e)

    def fold[C](onInvalid: E => C, onValid: A => C): C = this match
      case Valid(a) => onValid(a)
      case Invalid(e) => onInvalid(e)

  import Validated.*

  trait Semigroup[A]:
    extension (x: A) def combine(y: A): A

  object Semigroup:
    given [A] => Semigroup[List[A]]:
      extension (x: List[A]) def combine(y: List[A]): List[A] = x ++ y
    given Semigroup[Int]:
      extension (x: Int) def combine(y: Int): Int = x + y

  def map2[E: Semigroup, A, B, C](va: Validated[E, A], vb: Validated[E, B])(
      f: (A, B) => C
  ): Validated[E, C] = (va, vb) match
    case (Valid(a), Valid(b)) => Valid(f(a, b))
    case (Invalid(e1), Invalid(e2)) => Invalid(e1.combine(e2))
    case (Invalid(e), _) => Invalid(e)
    case (_, Invalid(e)) => Invalid(e)

  trait Functor[F[_]]:
    def map[A, B](fa: F[A])(f: A => B): F[B]

  trait Applicative[F[_]] extends Functor[F]:
    def pure[A](a: A): F[A]
    def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C]

    /** TODO: implement, from `pure` and `map2` only. */
    def map[A, B](fa: F[A])(f: A => B): F[B] = ???

  /** TODO: the instances. */
  object Applicative
end Exercise03
