package typeprog.ch26typedhttpapi

import scala.compiletime.{constValueTuple, erasedValue, error, summonFrom}
import scala.deriving.Mirror

/** The HTTP of this chapter: the part of it the types talk about, and nothing
  * else. A request is a method and a path; a response is a status and a text
  * body. There is no socket — a server is a function from one to the other,
  * which is all a client needs to call it and a test needs to check it.
  */
enum Method:
  case Get, Post

final case class Request(method: Method, path: String)

final case class Response(status: Int, body: String)

/** How a value captured from a path segment is read and written: `42` in
  * `/users/42`. Given, not an exercise — chapter 06's type classes, small.
  */
trait Segment[A]:
  /** `None` for a segment that is not an `A`: the path does not match. */
  def parse(segment: String): Option[A]
  def show(a: A): String

object Segment:

  def of[A](parse: String => Option[A], show: A => String): Segment[A] =
    val (p, s) = (parse, show)
    new Segment[A]:
      def parse(segment: String): Option[A] = p(segment)
      def show(a: A): String = s(a)

  given Segment[Int] = of(_.toIntOption, _.toString)
  given Segment[Long] = of(_.toLongOption, _.toString)
  given Segment[String] = of(s => Option.when(s.nonEmpty)(s), identity)

/** How a response body is written and read back: the minimal codec this
  * chapter needs, rebuilt rather than imported from chapter 24, so that this
  * chapter compiles whether or not that one is solved.
  */
trait Codec[A]:
  def encode(a: A): String

  /** `None` for a body that is not an `A`. */
  def decode(body: String): Option[A]

object Codec:

  def of[A](encode: A => String, decode: String => Option[A]): Codec[A] =
    val (e, d) = (encode, decode)
    new Codec[A]:
      def encode(a: A): String = e(a)
      def decode(body: String): Option[A] = d(body)

  given Codec[Int] = of(_.toString, _.toIntOption)
  given Codec[String] = of(identity, Some(_))
  given Codec[Boolean] = of(_.toString, _.toBooleanOption)

  /** Elements one per line; an element that contains a newline does not
    * survive the round trip.
    */
  given [A] => (c: Codec[A]) => Codec[List[A]] =
    of(
      _.map(c.encode).mkString("\n"),
      body =>
        if body.isEmpty then Some(Nil)
        else
          body
            .split("\n")
            .toList
            .foldRight(Option(List.empty[A])): (line, rest) =>
              for a <- c.decode(line); as <- rest yield a :: as
    )

  /** For an enum whose cases are all singletons (chapter 10): each case is
    * written as its name.
    */
  inline def derived[A](using m: Mirror.SumOf[A]): Codec[A] =
    cases(
      constValueTuple[m.MirroredElemLabels].toList.asInstanceOf[List[String]],
      values[m.MirroredElemTypes, A]
    )

  def cases[A](labels: List[String], values: List[A]): Codec[A] =
    val (byValue, byLabel) = (values.zip(labels).toMap, labels.zip(values).toMap)
    of(byValue, byLabel.get)

  inline def values[Ts <: Tuple, A]: List[A] = inline erasedValue[Ts] match
    case _: EmptyTuple => Nil
    case _: (h *: t) => value[h, A] :: values[t, A]

  // A separate method with a type parameter of its own: written inside the
  // `case` above, the search would be for the pattern variable `h`, and no
  // `ValueOf` is found for it (chapter 10's pitfalls).
  inline def value[H, A]: A = summonFrom {
    case v: ValueOf[H] => v.value.asInstanceOf[A]
    case _ => error("Codec.derived needs every case to be a singleton, with no parameters")
  }
end Codec
