package typeprog.ch20validation

/** Exercise 02 — combine two results, and keep the errors of both. *Solved.*
  *
  * The instances live in `Semigroup`'s companion. A
  * `Semigroup[List[String]]` is looked for first in the lexical scope and then
  * in the implicit scope of the type — the companions of `Semigroup`, of
  * `List` and of `String` — and of those, `Semigroup`'s is the only one this
  * code owns. Put anywhere else, they would need an import at every use.
  *
  * The list instance is a conditional one in its simplest form: it takes a
  * type parameter, so a single `given` covers `List[String]`, `List[Int]` and
  * every other list.
  *
  * `map2` asks for `E: Semigroup` and that is the whole of the type-level
  * answer. Inside, the context bound puts an instance in the lexical scope, so
  * `e1.combine(e2)` finds the extension method there. At the call site, it
  * turns an error type with no instance — `Boolean` — into a compile error,
  * instead of letting a `map2` exist that has no way of keeping both errors.
  *
  * The `match` on the pair is where the difference from `Either` lives: the
  * case with two `Invalid`s is reached only because both sides were computed
  * before being looked at. Its order matters too — `e1` before `e2` is what
  * keeps errors in the order of the checks that produced them.
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
end Exercise02
