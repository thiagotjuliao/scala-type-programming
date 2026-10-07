package typeprog.ch20validation

/** Exercise 02 — combine two results, and keep the errors of both.
  *
  * `map2` takes two results computed independently and a function of their
  * two values. When both are valid it applies the function; when either is
  * invalid, the result carries every error there was — those of `va` first,
  * then those of `vb`.
  *
  * How two error values become one depends on their type, so it is a type
  * class: `Semigroup`.
  *
  *   - Give lists a `Semigroup` that concatenates them, and `Int` one that
  *     adds — the error type of a validation that only counts its failures.
  *     Both must be found with no import, wherever `Semigroup` is used.
  *   - `map2` combines errors with whatever `Semigroup` the error type has, and
  *     an error type with none, `Boolean` say, does not compile.
  *
  * `Validated` is exercise 01's, already solved, so this one does not wait on
  * it.
  *
  * Hint: chapter 06 — where the compiler looks for a `Semigroup[List[String]]`
  * when nothing is imported, and how a method asks for one.
  */
object Exercise02:

  enum Validated[+E, +A]:
    case Valid(value: A)
    case Invalid(errors: E)

    def map[B](f: A => B): Validated[E, B] = this match
      case Valid(a) => Valid(f(a))
      case Invalid(e) => Invalid(e)

    def fold[C](onInvalid: E => C, onValid: A => C): C = this match
      case Valid(a) => onValid(a)
      case Invalid(e) => onInvalid(e)

  trait Semigroup[A]:
    extension (x: A) def combine(y: A): A

  object Semigroup:
    given [T] => Semigroup[List[T]] = new Semigroup[List[T]]:
      extension (xs: List[T]) override def combine(ys: List[T]): List[T] = xs ++ ys

    given Semigroup[Int] = new Semigroup[Int]:
      extension (x: Int) override def combine(y: Int): Int = x + y

  import Validated.*

  def map2[E: Semigroup, A, B, C](va: Validated[E, A], vb: Validated[E, B])(
      f: (A, B) => C
  ): Validated[E, C] =
    (va, vb) match
      case (Valid(a), Valid(b)) => Valid(f(a, b))
      case (Invalid(e1), Invalid(e2)) => Invalid(e1.combine(e2))
      case (_, Invalid(e)) => Invalid(e)
      case (Invalid(e), _) => Invalid(e)

end Exercise02
