package typeprog.ch20validation

/** Exercise 04 — `traverse`, written once, and a form that reports every
  * error. *Solved.*
  *
  * `traverse` is a `foldRight` over the list: the empty list is `pure(Nil)`,
  * and each element is checked and put in front of the checked rest with
  * `map2`. Nothing in it names `Option` or `Validated`; what happens when a
  * check fails is decided by the `map2` of the instance the compiler found for
  * `F`. With `Option` the first `None` makes the whole result `None`; with
  * `Validated` the errors are combined, so every failure is kept — the same
  * line of code, two behaviours.
  *
  * Folding from the right keeps both orders. Each `map2` has the element on the
  * left, so its errors come before those of the elements after it, and the
  * values are consed in their original order. A `foldLeft` that appends to an
  * accumulator would work too, at the price of an `:+` per element; one that
  * prepends would reverse the list and every error in it.
  *
  * `signUp` is two `map2`s, because a form of three fields is a pair of a pair
  * and a field. The three checks run regardless of each other — that is the
  * point — and the `User` is built only when the nested pair is all valid.
  *
  * `signUpAll` is `traverse` with `signUp` as the check, and needs no type
  * argument: `signUp` returns a `Checked[User]`, so `F` is inferred as
  * `Checked`, and the conditional instance of exercise 03 is found for it with
  * `E = List[String]`.
  */
object Exercise04:

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
      ): Validated[E, C] = (fa, fb) match
        case (Valid(a), Valid(b)) => Valid(f(a, b))
        case (Invalid(e1), Invalid(e2)) => Invalid(e1.combine(e2))
        case (Invalid(e), _) => Invalid(e)
        case (_, Invalid(e)) => Invalid(e)

  def traverse[F[_]: Applicative, A, B](as: List[A])(f: A => F[B]): F[List[B]] =
    val F = summon[Applicative[F]]
    as.foldRight(F.pure(List.empty[B]))((a, rest) => F.map2(f(a), rest)(_ :: _))

  type Checked[A] = Validated[List[String], A]

  final case class User(name: String, age: Int, email: String)

  def checkName(s: String): Checked[String] =
    if s.isBlank then Invalid(List("name is empty")) else Valid(s)

  def checkAge(n: Int): Checked[Int] =
    if n < 18 then Invalid(List("must be 18 or older")) else Valid(n)

  def checkEmail(s: String): Checked[String] =
    if s.contains('@') then Valid(s) else Invalid(List("email has no @"))

  def signUp(name: String, age: Int, email: String): Checked[User] =
    val F = summon[Applicative[Checked]]
    F.map2(F.map2(checkName(name), checkAge(age))((_, _)), checkEmail(email)) { case ((n, a), e) =>
      User(n, a, e)
    }

  def signUpAll(forms: List[(String, Int, String)]): Checked[List[User]] =
    traverse(forms)(signUp.tupled)
end Exercise04
