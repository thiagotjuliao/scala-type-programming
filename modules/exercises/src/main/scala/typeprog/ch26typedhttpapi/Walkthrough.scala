package typeprog.ch26typedhttpapi

import typeprog.core.evidence

import WalkthroughMacros.*

/** Chapter 26 — Typed HTTP API.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it. The macro it calls is in `WalkthroughMacros.scala`,
  * next to it — a macro cannot be called from the file that defines it.
  *
  * The examples are a typed log line and a message topic rather than an HTTP
  * path and an endpoint, so that the exercises are left to do. In order:
  *
  *   1. a type that grows as a value is built: a tuple, one append per call;
  *   2. a value that carries its types as members, and a method typed through
  *      it;
  *   3. a match type that turns a tuple into what a function wants, and its
  *      value counterpart;
  *   4. the one function type every function is;
  *   5. a macro whose call takes the type of its expansion.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch26-typed-http-api.md`.
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. A type that grows as the value is built
  //
  // A log line is text with typed fields in it. Each `+` of a field appends
  // the field's type to the line's tuple (chapter 09); text appends nothing.
  // ---------------------------------------------------------------------------

  final class Field[A](val name: String)

  final class Line[Fields <: Tuple](val parts: List[String | Field[?]]):
    def +(text: String): Line[Fields] = Line(parts :+ text)
    def +[A](field: Field[A]): Line[Tuple.Append[Fields, A]] = Line(parts :+ field)

  val login = Line[EmptyTuple](Nil) + "user " + Field[String]("user") + " logged in from " +
    Field[Int]("port")

  val loginFields = evidence.inferredAs[login.type, Line[(String, Int)]]

  // ---------------------------------------------------------------------------
  // 2. A value that carries its types as members
  //
  // A topic says what its messages are. As a type member rather than a
  // parameter, code that takes any topic can still talk about that type,
  // through the value: `publish` is a dependent method type (chapter 03).
  // ---------------------------------------------------------------------------

  trait Topic:
    type Msg
    def name: String

  object Topic:
    type Aux[M] = Topic { type Msg = M }
    def apply[M](n: String): Aux[M] = new Topic:
      type Msg = M
      val name = n

  val logins = Topic[(String, Int)]("logins")

  def publish(topic: Topic)(message: topic.Msg): String = s"${topic.name}: $message"

  val published: String = publish(logins)(("ada", 22)) // "logins: (ada,22)"

  //   publish(logins)("ada")
  //   // Found:    ("ada" : String)
  //   // Required: logins.Msg

  // ---------------------------------------------------------------------------
  // 3. A match type that shapes a function's argument, and its value
  //
  // A tuple of one element is rarely what a function wants. `Single` gives the
  // element instead; at a use, the tuple is concrete and it reduces.
  // ---------------------------------------------------------------------------

  type Single[T <: Tuple] = T match
    case a *: EmptyTuple => a
    case _ => T

  val oneIsItsElement = evidence.sameType[Single[Int *: EmptyTuple], Int]
  val twoStayATuple = evidence.sameType[Single[(Int, String)], (Int, String)]

  // Inside a generic method `T` is abstract and `Single[T]` does not reduce,
  // so the value is converted by a test that mirrors the match type, and a
  // cast says what the compiler cannot see.
  def single[T <: Tuple](t: T): Single[T] =
    val value = if t.productArity == 1 then t.productElement(0) else t
    value.asInstanceOf[Single[T]]

  val unwrapped: Int = single(Tuple1(42)) // 42

  // A two-parameter lambda is adapted to a function of a pair, and that still
  // works with the match type written out:
  val repeat: Single[(Int, String)] => String = (n, s) => s * n

  // It does not when the parameter is typed through a member of the receiver,
  // as a handler's is:
  trait Inbox:
    type In <: Tuple
    def on(f: Single[In] => String): String = "registered"

  val pairs = new Inbox:
    type In = (Int, String)

  //   pairs.on((n, s) => s * n)
  //   // Wrong number of parameters, expected: 1
  //
  // A pattern-matching function is accepted there:
  val registered: String = pairs.on { case (n, s) => s * n }

  // ---------------------------------------------------------------------------
  // 4. The one function type every function is
  //
  // `Function1[-A, +B]` (chapter 01): a function of a wider argument, or with
  // a narrower result, is a function of the narrower argument and the wider
  // result. At the bottom of the first and the top of the second, every
  // function is a `Nothing => Any` — and nothing can call it, since there is
  // no `Nothing` to pass.
  // ---------------------------------------------------------------------------

  val anything: Nothing => Any = (s: String) => s.length

  // ---------------------------------------------------------------------------
  // 5. A macro whose call takes the type of its expansion
  //
  // `pack` turns its arguments into a tuple. The macro can only declare
  // `Expr[Tuple]`; with `transparent` (chapter 08) the call has the type of
  // the code it expands to — each argument's own type, singletons included.
  // ---------------------------------------------------------------------------

  val n = 1

  val packed: (Int, String, Boolean) = pack(n, "a", true)
  val exact: (n.type, "a", true) = pack(n, "a", true)

  // The same expansion without `transparent` is only a `Tuple`:
  val loose: Tuple = packLoose(n, "a", true)

  //   val typed: (Int, String, Boolean) = packLoose(n, "a", true)
  //   // Found:    Tuple
  //   // Required: (Int, String, Boolean)

end Walkthrough
