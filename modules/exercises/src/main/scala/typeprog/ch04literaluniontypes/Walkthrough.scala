package typeprog.ch04literaluniontypes

import typeprog.core.evidence

/** Chapter 04 — literal, singleton, union & intersection types.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The chapter covers five things, in order:
  *
  *   1. literal types — `42` as a type, and where inference widens it away;
  *   2. singleton types — `x.type`, `this.type`, and the `Singleton` bound;
  *   3. union types — "one of these", with no common parent declared;
  *   4. intersection types — "both", with no combined trait declared;
  *   5. `Matchable` — what a generic `match` is allowed to look at.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch04-literal-union-types.md`.
  *
  * (Messages quoted below drop the `typeprog.ch04literaluniontypes.` prefix the
  * compiler prints in front of every name.)
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. Literal types
  //
  // Every literal is also a type, inhabited by that one value. The compiler
  // writes it `(42 : Int)`, and it is a subtype of `Int`.
  // ---------------------------------------------------------------------------

  val answer: 42 = 42
  val id: "user-id" = "user-id"
  val yes: true = true

  evidence.subtypeOf[42, Int]
  evidence.subtypeOf["user-id", String]

  //   val wrong: 42 = 43
  //   // Found:    (43 : Int)
  //   // Required: (42 : Int)

  // A union of literals is a finite set of values, written as a type:
  type Weekend = "Sat" | "Sun"

  def isLazy(day: Weekend): Boolean = true

  val lazySunday: Boolean = isLazy("Sun")

  //   isLazy("Mon")
  //   // Found:    ("Mon" : String)
  //   // Required: Walkthrough.Weekend
  //
  // (The message names the alias rather than spelling the union out.)

  // Inference widens a literal at every unannotated `val`. Only an ascription,
  // a `final val` member or an `inline val` keeps it:
  val widened = 200 // Int
  val ascribed: 200 = 200 // 200
  final val constant = 200 // 200
  inline val inlined = 200 // 200

  evidence.subtypeOf[constant.type, 200]
  evidence.subtypeOf[ascribed.type, 200]

  //   evidence.subtypeOf[widened.type, 200]
  //   // Cannot prove that (Walkthrough.widened : Int) <:< (200 : Int).

  // And the widening happens again at the next `val`: a copy of `ascribed` is
  // an `Int`, not a `200`.
  val copied = ascribed

  // A match over a union of literals is checked for exhaustiveness. Remove the
  // `"Sun"` case below and the compiler warns:
  //
  //   match may not be exhaustive.
  //   It would fail on pattern case: "Sun"
  def greeting(day: Weekend): String = day match
    case "Sat" => "Saturday"
    case "Sun" => "Sunday"

  // ---------------------------------------------------------------------------
  // 2. Singleton types
  //
  // Chapter 03 selected types *through* a stable path. The path also has a type
  // of its own, `x.type`, whose only value is `x`. Literal types are the
  // singleton types of literals.
  // ---------------------------------------------------------------------------

  object Registry

  val registry: Registry.type = Registry

  // `this.type` is the singleton type of the receiver. Declared as a result
  // type, it keeps whatever the caller had — subclass included.
  class Shape:
    private var label = ""
    def named(name: String): this.type =
      label = name
      this
    def name: String = label

  final class Circle extends Shape:
    private var r = 0.0
    def radius(value: Double): this.type =
      r = value
      this
    def area: Double = math.Pi * r * r

  // `named` is declared on `Shape`, and the chain still has a `Circle` to call
  // `radius` on:
  val unit: Circle = Circle().named("unit").radius(1.0)

  // Declared `: Shape` instead, `named` would end the chain:
  //
  //   // value radius is not a member of Shape

  // `Singleton` bounds a type parameter to singleton types, so it is inferred
  // as the literal instead of being widened. `ValueOf[T]` — the evidence that
  // hands back the one value of a singleton type — needs exactly that.
  def valueOfLiteral[T <: Singleton](t: T)(using v: ValueOf[T]): T = v.value

  val sat: "Sat" = valueOfLiteral("Sat")

  // Without the bound, `T` is inferred as `String`, which has no single value:
  //
  //   // No singleton value available for String; eligible singleton types for
  //   // `ValueOf` synthesis include literals and stable paths.

  // ---------------------------------------------------------------------------
  // 3. Union types
  //
  // `A | B` is a value that is an `A` or a `B`. Nothing has to be declared: the
  // members need no common parent.
  // ---------------------------------------------------------------------------

  final case class Timeout(millis: Long)
  final case class Refused(host: String)

  /** Two unrelated failures, and a success, in one result type. */
  def connect(host: String): String | Timeout | Refused =
    if host.isEmpty then Refused(host)
    else if host == "slow" then Timeout(5000)
    else s"connected to $host"

  // A union is taken apart by asking which member it is. The match is checked
  // for exhaustiveness exactly like one over a sealed hierarchy.
  def report(result: String | Timeout | Refused): String = result match
    case message: String => message
    case Timeout(millis) => s"gave up after $millis ms"
    case Refused(host) => s"$host refused"

  val reported: String = report(connect("example.com"))

  // Unions are commutative and associative, and they absorb their subtypes:
  evidence.sameType[Int | String, String | Int]
  evidence.sameType[(Int | String) | Timeout, Int | (String | Timeout)]
  evidence.sameType[Circle | Shape, Shape]

  // Inference keeps a union of *different* types...
  def flag: Boolean = true
  val mixed = if flag then 1 else "one"
  val mixedAsUnion: Int | String = mixed
  val listOfBoth: List[Int | String] = List(1, "one")

  // ...but a union of literals is widened, like any literal:
  val code = if flag then 200 else 404

  //   val c: 200 | 404 = code
  //   // Found:    (Walkthrough.code : Int)
  //   // Required: (200 : Int) | (404 : Int)

  // Given the expected type up front, the literals survive:
  val codeKept: 200 | 404 = if flag then 200 else 404

  // ---------------------------------------------------------------------------
  // 4. Intersection types
  //
  // `A & B` is a value that is an `A` and a `B`. It replaces Scala 2's
  // `A with B` in types, and it is commutative.
  // ---------------------------------------------------------------------------

  trait Named:
    def name: String

  trait Aged:
    def age: Int

  def introduce(who: Named & Aged): String = s"${who.name}, ${who.age}"

  // Anything that is both fits — nobody declared a `NamedAndAged` trait:
  final case class Person(name: String, age: Int) extends Named, Aged

  val ada: String = introduce(Person("Ada", 36))

  // An anonymous class can have both parents; `with` survives there, among a
  // class's parents, and only there:
  val anonymous: String = introduce(new Named with Aged:
    def name = "anon"
    def age = 0)

  //   val onlyNamed: Named = new Named { def name = "x" }
  //   introduce(onlyNamed)
  //   // Found:    (onlyNamed : Walkthrough.Named)
  //   // Required: Walkthrough.Named & Walkthrough.Aged

  evidence.sameType[Named & Aged, Aged & Named]
  evidence.subtypeOf[Named & Aged, Named]

  // When both sides have a member of the same name, the intersection has it at
  // the intersection of the two types:
  trait Exact:
    def id: Int
  trait Loose:
    def id: Any

  def idOf(x: Exact & Loose): Int = x.id

  // ---------------------------------------------------------------------------
  // 5. Matchable
  //
  // `Matchable` sits between `Any` and every class type. A `match` on a value
  // of an unconstrained type parameter cannot know that the value may be
  // inspected at runtime, and under `-source:future` the compiler says so:
  //
  //   def describe[T](t: T) = t match { case i: Int => ... }
  //   // pattern selector should be an instance of Matchable,
  //   // but it has unmatchable type T instead
  //
  // Bounding the parameter states the intent, and silences it:
  // ---------------------------------------------------------------------------

  def describe[T <: Matchable](t: T): String = t match
    case i: Int => s"the number $i"
    case s: String => s"the text $s"
    case _ => "something else"

  // This matters in chapter 05: an opaque type is not supposed to be looked
  // through at runtime, and `Matchable` is how a generic `match` is kept from
  // doing it.
end Walkthrough
