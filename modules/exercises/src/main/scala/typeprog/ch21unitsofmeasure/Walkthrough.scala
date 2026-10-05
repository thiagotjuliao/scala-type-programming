package typeprog.ch21unitsofmeasure

import scala.compiletime.ops.int.{+, -, >}

/** Chapter 21 — Units of measure.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The examples run beside the exercises rather than through them — a stack
  * of plates instead of units — so that `Mul`, `Div` and `sqrt` are left for
  * you to build. In order:
  *
  *   1. arithmetic on literal types, and tuples of them;
  *   2. a match type over a pair, and why it declares a bound;
  *   3. a phantom count on an opaque type, and the scope where it is not
  *      opaque;
  *   4. an operation that only sometimes makes sense, guarded by evidence.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch21-units-of-measure.md`.
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. Arithmetic on literal types
  //
  // `compiletime.ops.int.+` is a type of two types. When both are literals it
  // reduces to the literal of their sum, while the code is being type-checked.
  // ---------------------------------------------------------------------------

  val five: 2 + 3 = 5

  // A tuple of literal types is a type, and two of them are equal when their
  // elements are — whatever arithmetic produced the elements.
  val sameTuple = summon[(1 + 1, 3 - 3) =:= (2, 0)]

  // ---------------------------------------------------------------------------
  // 2. A match type over a pair
  //
  // One case takes both tuples apart and builds the result from their parts.
  // ---------------------------------------------------------------------------

  type Pair = (Int, Int)

  type Plus[A <: Pair, B <: Pair] <: Pair = (A, B) match
    case ((a1, a2), (b1, b2)) => (a1 + b1, a2 + b2)

  val plus = summon[Plus[(1, 2), (10, -20)] =:= (11, -18)]

  // The `<: Pair` after the parameters is what lets `Plus[A, B]` be used where
  // a `Pair` is required while `A` and `B` are still abstract. Without it,
  // `combined` below does not compile — *"Found: Box[P] Required:
  // Box[Plus[A, B]]"*, with a note that the match type could not be reduced:
  // nothing else says the result is a pair, and with abstract arguments it is
  // never reduced.
  final case class Box[P <: Pair]()

  def combined[A <: Pair, B <: Pair](a: Box[A], b: Box[B]): Box[Plus[A, B]] = Box()

  // ---------------------------------------------------------------------------
  // 3. A phantom count on an opaque type
  //
  // `Plates[N]` is a List[String] at runtime; the count N exists only in the
  // type.
  // ---------------------------------------------------------------------------

  object kitchen:
    opaque type Plates[N <: Int] = List[String]

    def empty: Plates[0] = Nil

    extension [N <: Int](p: Plates[N])
      def push(plate: String): Plates[N + 1] = plate :: p
      def plates: List[String] = p

    // In here, Plates[N] is List[String] and N is not checked: this compiles,
    // and lies. Code that relies on the count belongs outside `kitchen`.
    def lying: Plates[100] = Nil
  end kitchen

  import kitchen.*

  // Outside, the count is tracked through every push.
  val two: Plates[2] = empty.push("blue").push("white")

  // ---------------------------------------------------------------------------
  // 4. An operation that only sometimes makes sense
  //
  // Taking the top plate needs a plate. A match type with a case only for the
  // good inputs is not enough: when no case matches, the compiler *warns* and
  // leaves the type unreduced, and the call compiles. Evidence makes it an
  // error. `N > 0` is a type that reduces to `true` or `false`, and the method
  // asks for proof that it is `true`.
  // ---------------------------------------------------------------------------

  extension [N <: Int](p: Plates[N]) def top(using (N > 0) =:= true): String = p.plates.head

  val topPlate: String = two.top // "white"

  // `empty.top` does not compile:
  //
  //   Cannot prove that (0 : Int) > (0 : Int) =:= (true : Boolean).
  //
  // nor does `top` on a count nobody knows yet, `def f[N <: Int](p:
  // Plates[N]) = p.top` — unless `f` asks its own caller for the same proof.
end Walkthrough
