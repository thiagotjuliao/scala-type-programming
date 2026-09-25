package typeprog.ch06typeclassesgivens

/** Exercise 04 — a result type computed by the search. *Solved.*
  *
  * `add` returns `c.Out`, a type that depends on the instance passed as `c`
  * (chapter 03). Which instance that is, the compiler decides from `A` and `B`;
  * what its `Out` is, the instance's type says. So the result type of a call
  * is computed at compile time by the search, and the body is just `c(a, b)`.
  *
  * For that to work, the instances must keep their `Out` in their type. Each
  * is declared as an `Aux[A, B, O]` — `Add[A, B] { type Out = O }`, a
  * refinement (chapter 01). The near miss declares a plain `Add[Int, Int]`: it
  * compiles, is found, and has forgotten what it produces, so every result is
  * only known as the instance's own `Out`:
  *
  * {{{
  * Found:    given_Add_Int_Int.Out
  * Required: Int
  * }}}
  *
  * The pair rule is where the search does real work. Its premises are two
  * `Aux` instances whose `O1` and `O2` are *unknown* when the search starts;
  * finding the instance for the first components fixes `O1`, the second fixes
  * `O2`, and only then is the conclusion's `Out`, `(O1, O2)`, a type. Nested
  * pairs recurse the same way. That is a function from types to types, run by
  * implicit search — what chapter 07 does directly with match types, and
  * chapter 09 does over tuples of any length.
  *
  * No rule matches `(Int, String)` or `(Boolean, Boolean)`, and no rule
  * matches a pair and a number, so those calls find no `Add` and do not
  * compile.
  */
object Exercise04:

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

    given pair: [A1, B1, O1, A2, B2, O2]
      => (first: Aux[A1, B1, O1], second: Aux[A2, B2, O2])
      => Aux[(A1, A2), (B1, B2), (O1, O2)] =
      instance((a, b) => (first(a._1, b._1), second(a._2, b._2)))

  def add[A, B](a: A, b: B)(using c: Add[A, B]): c.Out = c(a, b)
end Exercise04
