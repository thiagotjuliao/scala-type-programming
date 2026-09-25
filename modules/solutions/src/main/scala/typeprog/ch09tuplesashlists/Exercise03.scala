package typeprog.ch09tuplesashlists

/** Exercise 03 — chapter 06's `add`, for tuples of any length. *Solved.*
  *
  * Two rules replace every arity. The empty tuples add to the empty tuple. A
  * head-and-tail adds to a head-and-tail when the heads add, to some `O1`, and
  * the tails add, to some `O2` — and the result is `O1 *: O2`. The tails are
  * tuples themselves, so the second premise is the same rule again, one element
  * shorter, until the tails are `EmptyTuple` and the first rule ends it.
  *
  * The heads can be anything with an `Add` — a number, a string, or a tuple,
  * which is how nesting works with no rule for it.
  *
  * Every instance is declared as an `Aux`, and so are both premises: the
  * conclusion's `Out` is built from `O1` and `O2`, and the search can only
  * compute them if each instance's type still says what its `Out` is (chapter
  * 06). A premise asked for as a plain `Add[H1, H2]` has an `Out` nobody knows.
  *
  * Tuples of different lengths reach a point where one side is `EmptyTuple` and
  * the other is not; no rule matches that, and the whole search fails — which
  * is the rejection the spec asks for.
  */
object Exercise03:

  trait Add[A, B]:
    type Out
    def apply(a: A, b: B): Out

  object Add:

    /** An `Add` that says what it produces. */
    type Aux[A, B, O] = Add[A, B] { type Out = O }

    def instance[A, B, O](f: (A, B) => O): Aux[A, B, O] = new Add[A, B]:
      type Out = O
      def apply(a: A, b: B): O = f(a, b)

    given Aux[Int, Int, Int] = instance(_ + _)
    given Aux[Int, Double, Double] = instance(_ + _)
    given Aux[Double, Int, Double] = instance(_ + _)
    given Aux[Double, Double, Double] = instance(_ + _)
    given Aux[String, String, String] = instance(_ + _)

    given Aux[EmptyTuple, EmptyTuple, EmptyTuple] = instance((_, _) => EmptyTuple)

    given cons: [H1, T1 <: Tuple, H2, T2 <: Tuple, O1, O2 <: Tuple]
      => (head: Aux[H1, H2, O1], tail: Aux[T1, T2, O2])
      => Aux[H1 *: T1, H2 *: T2, O1 *: O2] =
      instance((a, b) => head(a.head, b.head) *: tail(a.tail, b.tail))

  def add[A, B](a: A, b: B)(using c: Add[A, B]): c.Out = c(a, b)
end Exercise03
