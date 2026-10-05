package typeprog.ch20validation

/** Exercise 01 — a result that is a value or the errors that stopped it.
  * *Solved.*
  *
  * Two `+`s. `Valid(42)` mentions no error type, so the compiler infers the
  * narrowest one there is, `Nothing`: its type is `Validated[Nothing, Int]`.
  * Covariance in `E` is what turns `Nothing <: List[String]` into
  * `Validated[Nothing, Int] <: Validated[List[String], Int]`, and the same
  * argument in `A` lets an `Invalid` stand for a result of any value type. It
  * is the reasoning chapter 01 applied to `List`, where `Nil` is a
  * `List[Nothing]`.
  *
  * Invariant, `Valid(42)` still type-checks wherever the expected type is
  * written next to it — inference picks `E` from it — which is why the gap is
  * easy to miss: it shows up only once a result is stored first and used
  * later, and every such use needs an ascription.
  *
  * Both parameters are only ever produced, never consumed, so covariance is
  * sound. `map` takes `A => B`, where `A` sits in a parameter of a parameter —
  * a covariant position again — and `fold` likewise for both.
  */
object Exercise01:

  enum Validated[+E, +A]:
    case Valid(value: A)
    case Invalid(errors: E)

    def map[B](f: A => B): Validated[E, B] = this match
      case Valid(a) => Valid(f(a))
      case Invalid(e) => Invalid(e)

    def fold[C](onInvalid: E => C, onValid: A => C): C = this match
      case Valid(a) => onValid(a)
      case Invalid(e) => onInvalid(e)
