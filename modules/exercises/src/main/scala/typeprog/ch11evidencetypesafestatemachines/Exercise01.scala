package typeprog.ch11evidencetypesafestatemachines

/** Exercise 01 — methods for some stacks only.
  *
  * `Stack[A]` is a list with a name. Give it three methods that only make
  * sense for some `A`, and make them exist only there:
  *
  *   - `sum` adds up a stack of numbers — any numeric type, and `0` for an
  *     empty stack — and does not compile for a stack of strings;
  *   - `flatten` turns a stack of stacks into one stack, in order, and does
  *     not compile for a stack of anything else;
  *   - `unzip` turns a stack of pairs into a pair of stacks, and does not
  *     compile for a stack of anything else.
  *
  * The class keeps its one type parameter: no subclass per element type.
  *
  * Hint: a method can ask for evidence about the class's type parameter, and
  * evidence that `A <:< B` is also a function from `A` to `B`.
  */
object Exercise01:

  final case class Stack[A](items: List[A]):

    /** TODO: numbers only. */
    def sum: A = ???

    /** TODO: stacks of stacks only. */
    def flatten[B]: Stack[B] = ???

    /** TODO: stacks of pairs only. */
    def unzip[X, Y]: (Stack[X], Stack[Y]) = ???
