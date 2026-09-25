package typeprog.ch10mirrorgenericderivation

import scala.compiletime.{constValue, constValueTuple, erasedValue, summonFrom}
import scala.deriving.Mirror

import typeprog.core.evidence

/** Chapter 10 — `Mirror` & generic derivation.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The chapter covers five things, in order:
  *
  *   1. a product's mirror — its fields as types;
  *   2. a sum's mirror — its cases as types;
  *   3. reading the shape while compiling;
  *   4. deriving an instance, for products and sums, and `derives`;
  *   5. recursive types, and why the element instances are lazy.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch10-mirror-generic-derivation.md`.
  *
  * (Messages quoted below drop the `typeprog.ch10mirrorgenericderivation.`
  * prefix the compiler prints in front of every name.)
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. A product's mirror
  //
  // Synthesised by the compiler for every case class. The shape is in its
  // type members.
  // ---------------------------------------------------------------------------

  final case class Person(name: String, age: Int)

  val personMirror = summon[Mirror.ProductOf[Person]]

  evidence.sameType[personMirror.MirroredElemTypes, (String, Int)]
  evidence.sameType[personMirror.MirroredElemLabels, ("name", "age")]
  evidence.sameType[personMirror.MirroredLabel, "Person"]

  // And the way back: from a tuple of field values to an instance.
  val ann: Person = personMirror.fromProduct(("Ann", 3))

  // No mirror for a class that is not a case class:
  //
  //   final class Plain(val x: Int)
  //   summon[Mirror.ProductOf[Plain]]
  //   // … Failed to synthesize an instance of type
  //   // scala.deriving.Mirror.ProductOf[Plain]: class Plain is not a generic
  //   // product because it is not a case class

  // ---------------------------------------------------------------------------
  // 2. A sum's mirror
  //
  // For an enum or a sealed trait: the cases, in order, and `ordinal` to say
  // which case a value is.
  // ---------------------------------------------------------------------------

  enum Shape:
    case Circle(radius: Double)
    case Square(side: Double)

  val shapeMirror = summon[Mirror.SumOf[Shape]]

  evidence.sameType[shapeMirror.MirroredElemTypes, (Shape.Circle, Shape.Square)]

  val squareOrdinal: Int = shapeMirror.ordinal(Shape.Square(1)) // 1

  // A case with no parameters has a singleton type, and a product mirror with
  // no elements.
  case object Nobody

  val nobodyMirror = summon[Mirror.ProductOf[Nobody.type]]

  evidence.sameType[nobodyMirror.MirroredElemTypes, EmptyTuple]

  // ---------------------------------------------------------------------------
  // 3. Reading the shape while compiling
  //
  // Chapter 08's tools read literal types as values, and chapter 09's apply to
  // the tuple of field types.
  // ---------------------------------------------------------------------------

  inline def labels[T](using m: Mirror.ProductOf[T]): List[String] =
    constValueTuple[m.MirroredElemLabels].toList.asInstanceOf[List[String]]

  inline def arity[T](using m: Mirror.ProductOf[T]): Int =
    constValue[Tuple.Size[m.MirroredElemTypes]]

  val personLabels: List[String] = labels[Person] // List("name", "age")
  val personArity: Int = arity[Person] // 2

  // ---------------------------------------------------------------------------
  // 4. Deriving
  //
  // For every element type, an instance — found if one exists, derived if
  // not — and a way to combine them. The instance itself is built in an
  // ordinary method: an anonymous class inside the `inline` body would be
  // copied into every derivation ("New anonymous class definition will be
  // duplicated at each inline site").
  // ---------------------------------------------------------------------------

  trait Eq[A]:
    def eqv(x: A, y: A): Boolean

  object Eq:
    given Eq[Int] = _ == _
    given Eq[Double] = _ == _
    given Eq[String] = _ == _

    inline def derived[A](using m: Mirror.Of[A]): Eq[A] = inline m match
      case s: Mirror.SumOf[A] => sum(s, instances[m.MirroredElemTypes])
      case _: Mirror.ProductOf[A] => product(instances[m.MirroredElemTypes])

    inline def instances[T <: Tuple]: List[Eq[Any]] = inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t) => instanceFor[h].asInstanceOf[Eq[Any]] :: instances[t]

    /** The instance that exists, or one derived from the type's own mirror. */
    inline def instanceFor[H]: Eq[H] = summonFrom {
      case e: Eq[H] => e
      case m: Mirror.Of[H] => derived[H](using m)
    }

    /** Same case, and that case's instance says equal. */
    def sum[A](s: Mirror.SumOf[A], elems: => List[Eq[Any]]): Eq[A] =
      lazy val cases = elems
      (x, y) =>
        val i = s.ordinal(x)
        i == s.ordinal(y) && cases(i).eqv(x, y)

    /** Every field equal. */
    def product[A](elems: => List[Eq[Any]]): Eq[A] =
      lazy val fields = elems
      (x, y) =>
        val xs = x.asInstanceOf[Product].productIterator
        val ys = y.asInstanceOf[Product].productIterator
        xs.zip(ys).zip(fields).forall { case ((a, b), e) => e.eqv(a, b) }
  end Eq

  // `derives Eq` puts `given Eq[Point] = Eq.derived` in `Point`'s companion.
  final case class Point(x: Int, y: Int) derives Eq

  val samePoint: Boolean = summon[Eq[Point]].eqv(Point(1, 2), Point(1, 2)) // true

  // A sum: the cases are case classes nobody derived anything for, and are
  // derived on the way.
  enum Figure derives Eq:
    case Dot(at: Int)
    case Line(from: Int, to: Int)

  val differentCases: Boolean = summon[Eq[Figure]].eqv(Figure.Dot(1), Figure.Line(1, 2)) // false

  // ---------------------------------------------------------------------------
  // 5. Recursive types
  //
  // `Eq[Tree]` needs `Eq[Node]`, which needs `Eq[Tree]` — the instance being
  // defined, already in the companion. Passed by name and cached lazily, it
  // is read on first use, when it exists. Evaluated eagerly while the
  // instance is built, it is read before it exists, and the compiler warns:
  // "Infinite loop in function body".
  // ---------------------------------------------------------------------------

  enum Tree derives Eq:
    case Leaf(value: Int)
    case Node(left: Tree, right: Tree)

  val sameTree: Boolean =
    import Tree.*
    summon[Eq[Tree]].eqv(Node(Leaf(1), Leaf(2)), Node(Leaf(1), Leaf(2))) // true

end Walkthrough
