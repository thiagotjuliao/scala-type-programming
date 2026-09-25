package typeprog.ch06typeclassesgivens

/** Exercise 04 — a result type computed by the search.
  *
  * `add` should add two values of *possibly different* types, with a result
  * type that depends on both — decided at compile time, by which instance of
  * `Add` is found:
  *
  *   - `Int` and `Int` make an `Int`;
  *   - `Int` and `Double`, either way round, and `Double` and `Double`, make a
  *     `Double`;
  *   - `String` and `String` make a `String`;
  *   - two pairs make a pair, adding component by component, each component
  *     with its own result type: `add((1, "a"), (2.5, "b"))` is a
  *     `(Double, String)`, statically, and pairs nest;
  *   - anything else — `add(1, "a")`, `add(true, false)`, a pair and a number
  *     — does not compile.
  *
  * The result type must be exact: `val i: Int = add(1, 2)` compiles, and
  * `val i: Int = add(1, 2.5)` does not. As shipped, `add` returns `Any`.
  *
  * `Add.Aux` and `Add.instance` are there to be used; the instances and the
  * signature of `add` are missing.
  *
  * Hint: the result type of `add` can mention its own `using` parameter
  * (chapter 03), and an instance only helps if its type still says what its
  * `Out` is (chapter 01). The pair instance needs two instances and two `Out`s.
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

    // TODO: the instances.

  /** TODO: the signature, and the body. */
  def add[A, B](a: A, b: B): Any = ???
