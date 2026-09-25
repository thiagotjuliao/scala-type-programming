package typeprog.ch05opaquetypes

import typeprog.core.evidence

/** Chapter 05 — opaque types & phantom newtypes.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The chapter covers five things, in order:
  *
  *   1. opaque types — an alias that is transparent inside its scope only;
  *   2. bounds — publishing part of the representation, in one direction;
  *   3. the runtime — what is left of an opaque type after compilation;
  *   4. phantom types — a type parameter no value ever has, carrying a fact;
  *   5. both at once — a zero-cost wrapper with a phantom parameter.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch05-opaque-types.md`.
  *
  * (Messages quoted below drop the `typeprog.ch05opaquetypes.` prefix the
  * compiler prints in front of every name.)
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. An opaque type is an alias with a boundary
  //
  // The boundary is the object that defines it. Inside `Accounts`, `AccountId`
  // *is* `Long`; everywhere else it is a type nobody knows anything about.
  // ---------------------------------------------------------------------------

  object Accounts:
    opaque type AccountId = Long

    object AccountId:
      /** Inside the scope, a `Long` simply is an `AccountId`. */
      def apply(raw: Long): AccountId = raw

    extension (id: AccountId)
      /** And an `AccountId` simply is a `Long`: `+` is `Long`'s. */
      def next: AccountId = id + 1
      def raw: Long = id

  import Accounts.*

  val first: AccountId = AccountId(1L)
  val second: AccountId = first.next
  val secondRaw: Long = second.raw

  // Outside the scope, none of that holds — in either direction:
  //
  //   val bad: AccountId = 42L
  //   // Found:    (42L : Long)
  //   // Required: Walkthrough.Accounts.AccountId
  //
  //   val alsoBad: Long = first
  //   // Found:    (Walkthrough.first : Walkthrough.Accounts.AccountId)
  //   // Required: Long
  //
  //   first + 1
  //   // value + is not a member of Walkthrough.Accounts.AccountId

  // `transfer` can no longer be called with its arguments in the wrong order,
  // which is the reason all of this exists:
  def transfer(from: AccountId, to: AccountId, cents: Long): String =
    s"${from.raw} -> ${to.raw}: $cents"

  val receipt: String = transfer(first, second, 1500L)

  // ---------------------------------------------------------------------------
  // 2. A bound decides what leaks
  //
  // An upper bound is visible outside the scope; the equation is not. So the
  // relation holds one way: every `Percent` is a `Double`, not every `Double` is
  // a `Percent`.
  // ---------------------------------------------------------------------------

  object Percentages:
    opaque type Percent <: Double = Double

    object Percent:
      def from(d: Double): Option[Percent] = Option.when(d >= 0 && d <= 100)(d)
      val Half: Percent = 50.0

  import Percentages.*

  evidence.subtypeOf[Percent, Double]

  val asDouble: Double = Percent.Half // allowed: the bound is public
  val doubled: Double = Percent.Half * 2 // `*` is `Double`'s, and so is the result

  //   val p: Percent = 50.0
  //   // Found:    (50.0d : Double)
  //   // Required: Walkthrough.Percentages.Percent

  // ---------------------------------------------------------------------------
  // 3. At runtime the boundary is gone
  //
  // There is no class `AccountId`. An `AccountId` is a `long`, an
  // `Array[AccountId]` is a `long[]`, and building one allocates nothing. That
  // is the difference from `final case class AccountId(value: Long)`, and the
  // reason opaque types exist.
  // ---------------------------------------------------------------------------

  val ids: Array[AccountId] = Array(first, second) // a long[] on the JVM

  // Because the boundary is gone at runtime, a runtime test would see straight
  // through it — `(first: Any).isInstanceOf[Long]` is `true`. Two checks keep
  // that from happening by accident:
  //
  // An opaque type is not `Matchable` (chapter 04), so under `-source:future`
  // matching on one is flagged:
  //
  //   // pattern selector should be an instance of Matchable,
  //   // but it has unmatchable type Walkthrough.Accounts.AccountId instead
  //
  // And comparing one with its representation is rejected — after type-checking,
  // by the multiversal equality check, so `compileErrors` does not see it:
  //
  //   first == 1L
  //   // Values of types Walkthrough.Accounts.AccountId and Long cannot be
  //   // compared with == or !=

  // ---------------------------------------------------------------------------
  // 4. Phantom types
  //
  // `Locked` and `Unlocked` are never instantiated. They exist to be written in
  // `Door[...]`, where the compiler can check them; at runtime every `Door` is
  // the same class.
  // ---------------------------------------------------------------------------

  sealed trait Lock
  sealed trait Locked extends Lock
  sealed trait Unlocked extends Lock

  final class Door[L <: Lock] private (val turns: Int)

  object Door:
    def locked: Door[Locked] = Door(0)

    // Each operation is declared for the one state it makes sense in:
    extension (d: Door[Locked]) def unlock: Door[Unlocked] = Door(d.turns + 1)
    extension (d: Door[Unlocked])
      def lock: Door[Locked] = Door(d.turns + 1)
      def walkThrough: Door[Unlocked] = d

  val usedDoor: Door[Locked] = Door.locked.unlock.walkThrough.lock

  //   Door.locked.walkThrough
  //   // value walkThrough is not a member of Walkthrough.Door[Walkthrough.Locked]
  //
  // The extensions live in `Door`'s companion, so that is the whole message.
  // Imported into scope instead, they would also be reported as "tried, but
  // could not be fully constructed", with the two `Door` types side by side.

  // The alternative keeps the method on the class and asks for evidence about
  // its parameter — chapter 01's `=:=`. Same safety, a shorter message.
  final class Gate[L <: Lock]:
    def pass(using L =:= Unlocked): Gate[L] = this

  val passed: Gate[Unlocked] = Gate[Unlocked]().pass

  //   Gate[Locked]().pass
  //   // Cannot prove that Walkthrough.Locked =:= Walkthrough.Unlocked.

  // The label is erased, so it cannot be tested at runtime either. A `case d:
  // Door[Unlocked]` is flagged — and would match a locked door too:
  //
  //   // the type test for Walkthrough.Door[Walkthrough.Unlocked] cannot be
  //   // checked at runtime because its type arguments can't be determined from Any

  // ---------------------------------------------------------------------------
  // 5. Both at once
  //
  // An opaque type with a phantom parameter: a `Money[EUR]` is a `Long` of
  // cents at runtime, and cannot be added to a `Money[USD]` at compile time.
  // ---------------------------------------------------------------------------

  object Currencies:
    sealed trait EUR
    sealed trait USD

    opaque type Money[C] = Long

    object Money:
      def cents[C](amount: Long): Money[C] = amount

    extension [C](m: Money[C])
      def +(other: Money[C]): Money[C] = m + other
      def inCents: Long = m

  import Currencies.*

  val lunch: Money[EUR] = Money.cents[EUR](1250) + Money.cents[EUR](300)
  val lunchCents: Long = lunch.inCents

  //   Money.cents[EUR](1250) + Money.cents[USD](300)
  //   // Found:    Walkthrough.Currencies.Money[Walkthrough.Currencies.USD]
  //   // Required: Walkthrough.Currencies.Money[Walkthrough.Currencies.EUR]

end Walkthrough
