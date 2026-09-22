package typeprog.core

/** Term-level witnesses for the type relations the compiler already knows.
  *
  * Each of these compiles if and only if the relation holds, which turns a
  * question about types into something a snippet can ask:
  *
  * {{{
  * assertTypeChecks("evidence.subtypeOf[Stack[Dog], Stack[Animal]]")
  * }}}
  *
  * Call them inside such a snippet rather than directly in a test body. Called
  * directly, a false claim is a compile error — which is correct, but it takes
  * the module with it instead of reporting a single failing test.
  */
object evidence:

  /** Witnesses that `A` and `B` are the same type, in both directions. */
  def sameType[A, B](using A =:= B): Unit = ()

  /** Witnesses that `A` conforms to `B`. */
  def subtypeOf[A, B](using A <:< B): Unit = ()

  /** Witnesses that `A` is inferred for `B`, i.e. that `B` is at least as wide.
    *
    * Useful when what is being checked is an *inference* result rather than a
    * declared relation: `inferredAs[stack.type, Stack[Animal]]`.
    */
  def inferredAs[A, B >: A]: Unit = ()
