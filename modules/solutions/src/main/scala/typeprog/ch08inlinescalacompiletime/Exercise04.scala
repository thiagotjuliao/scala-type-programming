package typeprog.ch08inlinescalacompiletime

import scala.compiletime.{error, summonFrom}

/** Exercise 04 — use an instance if there is one. *Solved.*
  *
  * `summonFrom` is a match whose cases are instance searches, tried in order,
  * at the call site where `describe` is expanded. The first case that finds an
  * instance is kept — so the order of the cases *is* the priority: a `Show`
  * first, then a `Numeric`, then the last resort. An `Int` has both instances,
  * and the first case takes it.
  *
  * Because the search runs at the expansion, it sees what the caller sees: a
  * `given Show[Double]` in scope at the call wins over `Double`'s `Numeric`.
  * And the last resort is not an instance but an `error`, which chapter 06's
  * ranking could not express — a low-priority rule that fails is simply not a
  * candidate, and the caller gets the generic *"No given instance"*.
  *
  * The near miss is a plain `summon[Show[A]]` inside the body: it is resolved
  * where `describe` is written, for an unknown `A`, and does not compile.
  */
object Exercise04:

  trait Show[A]:
    def show(a: A): String

  object Show:
    given Show[Int] = i => s"#$i"
    given Show[Boolean] = b => if b then "yes" else "no"

  inline def describe[A](a: A): String = summonFrom {
    case s: Show[A] => s.show(a)
    case _: Numeric[A] => s"number $a"
    case _ => error("this type has neither a Show nor a Numeric")
  }
