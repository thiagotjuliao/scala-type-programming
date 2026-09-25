package typeprog.ch11evidencetypesafestatemachines

/** Exercise 01 — methods for some stacks only. *Solved.*
  *
  * Each method asks for a fact about `A` in a `using` clause, and exists only
  * where the fact can be proved. `sum` asks for a `Numeric[A]` — an ordinary
  * type class, which `Int` and `Double` have and `String` does not.
  *
  * `flatten` and `unzip` ask for `<:<` evidence: `A <:< Stack[B]` holds for a
  * `Stack[Stack[Int]]` with `B = Int`, and the compiler infers `B` while
  * proving it. The evidence is more than a permission — `A <:< B` extends
  * `A => B` — so it is also what turns each element into a stack, or a pair,
  * inside the body, with no cast.
  *
  * For a `Stack[String]`, the proof fails at the call. The class never needed
  * to know which element types would have these methods — that is decided at
  * each call, from the call's `A`.
  */
object Exercise01:

  final case class Stack[A](items: List[A]):

    def sum(using n: Numeric[A]): A = items.foldLeft(n.zero)(n.plus)

    def flatten[B](using ev: A <:< Stack[B]): Stack[B] = Stack(items.flatMap(a => ev(a).items))

    def unzip[X, Y](using ev: A <:< (X, Y)): (Stack[X], Stack[Y]) =
      val (xs, ys) = items.map(ev).unzip
      (Stack(xs), Stack(ys))
