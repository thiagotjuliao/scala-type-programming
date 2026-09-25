package typeprog.ch07matchtypes

/** Exercise 01 — take one layer off a type. *Solved.*
  *
  * Each case binds the part it wants — `a`, lower-case, like a variable in a
  * value pattern — and returns it. The test is subtyping: `Some[String] <:
  * Option[a]` holds with `a = String`, so `Some` needs no case of its own, and
  * neither does `Right`.
  *
  * The catch-all goes last. First, it would match everything, and `Unwrap`
  * would be the identity. Last, it is reached only once the three cases before
  * it are ruled out, and for `Int` or `Vector[Int]` they are: `Int` and
  * `Vector` are classes that extend none of `Option`, `Either` or `List`, so
  * the compiler can prove them disjoint and move on.
  *
  * For a type it cannot prove anything about, it does not move on. Inside a
  * method `def f[A]`, `Unwrap[A]` stays `Unwrap[A]` — stuck at `case Option[a]`,
  * because `A` may yet turn out to be an `Option`, and the catch-all would then
  * have been the wrong answer.
  */
object Exercise01:

  type Unwrap[X] = X match
    case Option[a] => a
    case Either[?, a] => a
    case List[a] => a
    case _ => X
