package typeprog.ch20validation

/** Exercise 03 — the abstraction: `Applicative`, for `Option` and for
  * `Validated`. *Solved.*
  *
  * `Applicative[F[_]]` wants a type constructor of one parameter, and
  * `Validated` has two: `Applicative[Validated]` is chapter 02's kind error.
  * The type lambda `[A] =>> Validated[E, A]` fixes the error type and leaves
  * the value type open, which is the shape `F[_]` asks for.
  *
  * Which `E`? Any that has a `Semigroup`, and no other — so the instance is
  * conditional, `given [E: Semigroup] => ...`. Searching for
  * `Applicative[[A] =>> Validated[List[String], A]]`, the compiler matches the
  * lambda against the instance's, finds `E = List[String]`, and then searches
  * for the premise, `Semigroup[List[String]]`. With `Boolean` the premise is
  * missing and so is the instance. An instance for a fixed `List[String]`
  * would have passed the first spec and failed the one for `Int`.
  *
  * Both instances are in `Applicative`'s companion, the one place in the
  * implicit scope of both searches that this code owns. `Option`'s companion
  * is in the scope too, but it is the standard library's.
  *
  * `map` follows from the other two, for every instance at once: pair the
  * value with a `pure(())` that carries nothing, and ignore the unit. That is
  * the sense in which an applicative is "more" than a functor — it is a
  * functor for free. `Option`'s `map2` could be a `match`, but it is also
  * `zip` followed by a `map`, which is what `map2` means for `Option`.
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

    def map[A, B](fa: F[A])(f: A => B): F[B] = map2(fa, pure(()))((a, _) => f(a))

  object Applicative:
    given Applicative[Option]:
      def pure[A](a: A): Option[A] = Some(a)
      def map2[A, B, C](fa: Option[A], fb: Option[B])(f: (A, B) => C): Option[C] =
        fa.zip(fb).map(f.tupled)

    given [E: Semigroup] => Applicative[[A] =>> Validated[E, A]]:
      def pure[A](a: A): Validated[E, A] = Valid(a)
      def map2[A, B, C](fa: Validated[E, A], fb: Validated[E, B])(
          f: (A, B) => C
      ): Validated[E, C] = Exercise03.map2(fa, fb)(f)
end Exercise03
