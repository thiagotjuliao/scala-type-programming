package typeprog.ch20validation

/** Exercise 01 — a result that is a value or the errors that stopped it.
  *
  * `Validated[E, A]` is what a check returns: `Valid(a)` when it passed,
  * `Invalid(errors)` when it did not. It is `Either` with a different promise,
  * which the next exercises keep; this one is about the type itself.
  *
  *   - `Valid(42)` names no error type, and should not have to: it must be
  *     usable as a `Validated[List[String], Int]`, or as one with any other
  *     error type, without an ascription. Likewise `Invalid(errors)` with any
  *     value type.
  *   - Results of different validations widen to a common type, the way two
  *     lists do.
  *   - `map` transforms the value of a `Valid`, and leaves an `Invalid` as it
  *     is, without calling the function.
  *   - `fold` turns either case into a `C`, with one function per case.
  *
  * Hint: chapter 01. What should a `Valid`, which has no errors at all, be a
  * subtype of?
  */
object Exercise01:

  enum Validated[+E, +A]:
    case Valid(value: A)
    case Invalid(errors: E)

    /** TODO: implement. */
    def map[B](f: A => B): Validated[E, B] =
      this match
        case Valid(a) => Valid(f(a))
        case Invalid(e) => Invalid(e)

    /** TODO: implement. */
    def fold[C](onInvalid: E => C, onValid: A => C): C =
      this match
        case Valid(a) => onValid(a)
        case Invalid(e) => onInvalid(e)
