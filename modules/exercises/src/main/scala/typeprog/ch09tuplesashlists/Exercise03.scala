package typeprog.ch09tuplesashlists

/** Exercise 03 — chapter 06's `add`, for tuples of any length.
  *
  * Chapter 06 added numbers, strings and pairs, with an instance per shape.
  * Extend `Add` so that two tuples of the same length add element by element,
  * each element with its own result type, whatever the length:
  *
  *   - `add((1, "a", 2.5), (2.5, "b", 1))` is a `(Double, String, Double)`,
  *     statically, and `(3.5, "ab", 3.5)`;
  *   - an empty tuple, a one-element tuple, a five-element tuple — all work;
  *   - tuples nest: `add((1, (1, "x")), (2, (0.5, "y")))` is a `(Int, (Double,
  *     String))`;
  *   - tuples of different lengths, or with an element that does not add, do
  *     not compile.
  *
  * Two new instances are enough, and no instance mentions a length.
  *
  * `add`'s signature is chapter 06's answer: as shipped it accepts anything and
  * returns `Any`, so that is to be restored too.
  *
  * Hint: a tuple is a head and a tail, `H *: T`, down to `EmptyTuple`. The rule
  * for a head and a tail needs the instance for the heads and the one for the
  * tails — and both their `Out`s.
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

    // TODO: an instance for the empty tuple, and one for a head and a tail.
    given Aux[EmptyTuple, EmptyTuple, EmptyTuple] = instance((_, _) => EmptyTuple)

    given [H1, T1 <: Tuple, O1, H2, T2 <: Tuple, O2 <: Tuple]
      => (head: Aux[H1, H2, O1], tail: Aux[T1, T2, O2]) => Aux[H1 *: T1, H2 *: T2, O1 *: O2] =
      instance:
        case (h1 *: t1, h2 *: t2) => head(h1, h2) *: tail(t1, t2)

  /** TODO: chapter 06's signature, and the body. */
  def add[A, B](a: A, b: B)(using ev: Add[A, B]): ev.Out = ev(a, b)
end Exercise03
