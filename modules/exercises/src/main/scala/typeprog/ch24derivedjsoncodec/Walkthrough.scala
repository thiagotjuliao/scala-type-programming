package typeprog.ch24derivedjsoncodec

import scala.annotation.implicitNotFound
import scala.compiletime.{constValue, constValueTuple, erasedValue, error, summonFrom}
import scala.deriving.Mirror

/** Chapter 24 — Derived JSON codec.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The examples run beside the exercises rather than through them: `Describe`
  * writes out a type's *shape* — `User{name: string, age: number}` — and
  * needs no value to do it, so the walk over a mirror is here and the codec is
  * left for you to build. In order:
  *
  *   1. a type class whose missing instance says what to do;
  *   2. a case class: its labels, walked beside its field types;
  *   3. an enum: its case names, and each case described on the way;
  *   4. what cannot be described: a compile error that names the field.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch24-derived-json-codec.md`.
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. A type class whose missing instance says what to do
  //
  // `@implicitNotFound` is what a plain search reports when it finds nothing
  // (chapter 06). `summon[Describe[java.time.Instant]]` does not compile:
  //
  //   no Describe for java.time.Instant
  // ---------------------------------------------------------------------------

  @implicitNotFound("no Describe for ${A}")
  trait Describe[A]:
    def describe: String

  object Describe:

    def of[A](text: String): Describe[A] = new Describe[A]:
      def describe: String = text

    given Describe[Int] = of("number")
    given Describe[String] = of("string")
    given [A] => (d: Describe[A]) => Describe[Option[A]] = of(s"${d.describe}?")

    // -------------------------------------------------------------------------
    // 2. A case class: its labels, walked beside its field types
    //
    // The mirror has both as tuples of types. `constValueTuple` turns the
    // labels into values; the types are walked with `erasedValue`, one
    // instance each — found if it exists, derived from the element's own
    // mirror if not (chapter 10). The labels travel with the types, so the
    // place that looks an instance up knows which field it is for.
    // -------------------------------------------------------------------------

    inline def derived[A](using m: Mirror.Of[A]): Describe[A] = inline m match
      case p: Mirror.ProductOf[A] =>
        val fields = labels[p.MirroredElemLabels]
          .zip(instances[p.MirroredLabel, p.MirroredElemTypes, p.MirroredElemLabels])
          .map((label, d) => s"$label: ${d.describe}")
        of(constValue[p.MirroredLabel] + fields.mkString("{", ", ", "}"))

      // -----------------------------------------------------------------------
      // 3. An enum: its case names, and each case described on the way
      //
      // A sum's labels are its cases' names, and its element types are the
      // cases — which have no instances of their own, and are derived here.
      // -----------------------------------------------------------------------

      case s: Mirror.SumOf[A] =>
        val cases = instances[s.MirroredLabel, s.MirroredElemTypes, s.MirroredElemLabels]
        of(constValue[s.MirroredLabel] + " = " + cases.map(_.describe).mkString(" | "))

    inline def labels[Ls <: Tuple]: List[String] =
      constValueTuple[Ls].toList.asInstanceOf[List[String]]

    inline def instances[T <: String, Ts <: Tuple, Ls <: Tuple]: List[Describe[?]] =
      inline erasedValue[(Ts, Ls)] match
        case _: (EmptyTuple, EmptyTuple) => Nil
        case _: (h *: ts, l *: ls) => instance[T, h, l] :: instances[T, ts, ls]

    // -------------------------------------------------------------------------
    // 4. What cannot be described
    //
    // The last case of the `summonFrom` is reached only when there is neither
    // an instance nor a mirror. `error` needs a constant message; `constValue`
    // of a literal type is one, and the concatenation is folded before the
    // message is reported.
    // -------------------------------------------------------------------------

    inline def instance[T <: String, H, L]: Describe[?] = summonFrom {
      case d: Describe[H] => d
      case m: Mirror.Of[H] => derived[H](using m)
      case _ =>
        error("cannot describe " + constValue[T] + ": field " + constValue[L] + " has no Describe")
    }
  end Describe

  final case class Address(city: String)
  final case class User(name: String, age: Int, address: Address, nick: Option[String])
      derives Describe

  enum Shape derives Describe:
    case Circle(radius: Int)
    case Dot

  val user: String = summon[Describe[User]].describe
  // "User{name: string, age: number, address: Address{city: string}, nick: string?}"

  val shape: String = summon[Describe[Shape]].describe
  // "Shape = Circle{radius: number} | Dot{}"

  final case class Event(name: String, at: java.time.Instant)

  // `Describe.derived[Event]` does not compile:
  //
  //   cannot describe Event: field at has no Describe
end Walkthrough
