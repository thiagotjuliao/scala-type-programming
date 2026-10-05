package typeprog.ch20validation

/** Chapter 20 — Validation.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The examples run beside the exercises rather than through them: the same
  * ideas on `Either`, `Option` and a count of errors, so that `Validated` is
  * left for you to build. In order:
  *
  *   1. why `Either` stops at the first error;
  *   2. combining errors, as a type class;
  *   3. `Applicative[F[_]]`, and the type lambda that fits a two-parameter
  *      type into it;
  *   4. code written once against the abstraction.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch20-validation.md`.
  *
  * (Messages quoted below drop the `typeprog.ch20validation.Walkthrough.`
  * prefix the compiler prints in front of every name.)
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. Why Either stops at the first error
  //
  // In a `for`, the second check is the body of a function of the first
  // value. When there is no first value, there is nothing to call it with.
  // ---------------------------------------------------------------------------

  def checkName(s: String): Either[String, String] =
    if s.isBlank then Left("name is empty") else Right(s)

  def checkAge(n: Int): Either[String, Int] =
    if n < 18 then Left("must be 18 or older") else Right(n)

  // `checkName("").flatMap(n => checkAge(12).map(a => (n, a)))`: checkAge is
  // never called. One error, of two.
  val firstOnly: Either[String, (String, Int)] =
    for
      n <- checkName("")
      a <- checkAge(12)
    yield (n, a) // Left("name is empty")

  // The two checks do not depend on each other, so both can run first, and be
  // combined after. Here by hand, for this one pair of types:
  val both: List[String] =
    List(checkName(""), checkAge(12)).collect { case Left(e) => e }
  // List("name is empty", "must be 18 or older")

  // ---------------------------------------------------------------------------
  // 2. Combining errors is a type class
  //
  // How two errors become one depends on their type. The exercises use lists;
  // here, a validation that only counts its failures.
  // ---------------------------------------------------------------------------

  trait Semigroup[A]:
    extension (x: A) def combine(y: A): A

  final case class Failures(count: Int)

  object Failures:
    val one: Failures = Failures(1)
    // In Failures' companion, so it is in the implicit scope of
    // Semigroup[Failures] and found with no import.
    given Semigroup[Failures]:
      extension (x: Failures) def combine(y: Failures): Failures = Failures(x.count + y.count)

  // A context bound puts the instance in scope, and with it the extension.
  def combineAll[E: Semigroup](first: E, rest: E*): E = rest.foldLeft(first)(_.combine(_))

  val three: Failures = combineAll(Failures.one, Failures.one, Failures.one) // Failures(3)

  // With no instance, the call does not compile:
  //
  //   combineAll(true, false)
  //   // No given instance of type Semigroup[Boolean] was found for a context
  //   // parameter of method combineAll in object Walkthrough

  // ---------------------------------------------------------------------------
  // 3. Applicative[F[_]], and fitting a two-parameter type into it
  // ---------------------------------------------------------------------------

  trait Applicative[F[_]]:
    def pure[A](a: A): F[A]
    def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C]

  object Applicative:
    // `Either` has two parameters and `F[_]` takes one. The type lambda fixes
    // the left one. `summon[Applicative[Either]]` is chapter 02's kind error:
    //
    //   Type argument Either does not have the same kind as its bound [_$3]
    //
    // (`_$3` is a fresh name; its number varies from one compilation to the
    // next.)
    //
    // This instance needs no condition on `E`: its map2 never combines two
    // errors — it keeps the first, which is the whole difference from the one
    // exercise 03 asks for.
    given [E] => Applicative[[A] =>> Either[E, A]]:
      def pure[A](a: A): Either[E, A] = Right(a)
      def map2[A, B, C](fa: Either[E, A], fb: Either[E, B])(f: (A, B) => C): Either[E, C] =
        for a <- fa; b <- fb yield f(a, b)

  // An alias does the same job as the lambda, and reads better at a use site.
  type Checked[A] = Either[String, A]

  val checkedInstance: Applicative[Checked] = summon[Applicative[Checked]]

  // ---------------------------------------------------------------------------
  // 4. Written once, against the abstraction
  //
  // `map3` knows nothing about Either or Option; it asks for an Applicative and
  // uses its two operations. Which errors survive is decided by the instance.
  // ---------------------------------------------------------------------------

  def map3[F[_], A, B, C, D](fa: F[A], fb: F[B], fc: F[C])(f: (A, B, C) => D)(using
      F: Applicative[F]
  ): F[D] =
    F.map2(F.map2(fa, fb)((_, _)), fc) { case ((a, b), c) => f(a, b, c) }

  // `F` is inferred from the arguments: `Either[String, Int]` is read as
  // `F[Int]` with `F = [A] =>> Either[String, A]`.
  val user: Either[String, (String, Int, Int)] =
    map3(checkName("Ada"), checkAge(36), Right(1): Either[String, Int])((n, a, i) => (n, a, i))
  // Right(("Ada", 36, 1))

  // Either's instance keeps the first error, so map3 does too — the same line
  // that, with an accumulating instance, would keep both.
  val stillFirstOnly: Either[String, (String, Int, Int)] =
    map3(checkName(""), checkAge(12), Right(1): Either[String, Int])((n, a, i) => (n, a, i))
  // Left("name is empty")
end Walkthrough
