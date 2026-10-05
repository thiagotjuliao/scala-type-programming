package typeprog.ch20validation

/** Exercise 04 — `traverse`, written once, and a form that reports every
  * error.
  *
  *   - `traverse` applies a check to every element of a list and collects the
  *     results, in their order, into one result: for any `F` with an
  *     `Applicative`, using nothing but its operations. With `Validated`, it
  *     reports the errors of every element that failed, in their order; with
  *     `Option`, it is `None` as soon as one element is. An empty list is
  *     valid, and empty.
  *   - `signUp` validates the three fields of a form with the three checks
  *     below and builds a `User` when all pass. When some fail, it reports
  *     every failure, in the order of the fields — never only the first.
  *   - `signUpAll` does it for a list of forms, and reports the failures of
  *     every form.
  *
  * Everything from exercise 03 is given, solved.
  *
  * Hint: a list is empty, or an element in front of a list. `pure` has an
  * answer for the first, and `map2` puts a checked element in front of a
  * checked rest.
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

  /** TODO: implement. */
  def traverse[F[_]: Applicative, A, B](as: List[A])(f: A => F[B]): F[List[B]] =
    val ev = summon[Applicative[F]]

    as.foldRight(ev.pure(List.empty[B])): (a, acc) =>
      ev.map2(f(a), acc)(_ :: _)

  type Checked[A] = Validated[List[String], A]

  final case class User(name: String, age: Int, email: String)

  def checkName(s: String): Checked[String] =
    if s.isBlank then Invalid(List("name is empty")) else Valid(s)

  def checkAge(n: Int): Checked[Int] =
    if n < 18 then Invalid(List("must be 18 or older")) else Valid(n)

  def checkEmail(s: String): Checked[String] =
    if s.contains('@') then Valid(s) else Invalid(List("email has no @"))

  /** TODO: implement. */
  def signUp(name: String, age: Int, email: String)(using ev: Applicative[Checked]): Checked[User] =
    ev.map2(
      ev.map2(checkName(name), checkAge(age))((a, b) => (a, b)),
      checkEmail(email)
    ):
      case ((a, b), c) => User(a, b, c)

  /** TODO: implement. */
  def signUpAll(forms: List[(String, Int, String)])(using
      Applicative[Checked]
  ): Checked[List[User]] =
    traverse(forms)(signUp)

end Exercise04
