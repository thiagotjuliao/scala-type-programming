package typeprog.ch22typedbuildersandprotocols

import scala.annotation.implicitNotFound

/** Chapter 22 — Typed builders and protocols.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The examples run beside the exercises rather than through them — a
  * checklist and a deck of cards — so that the builder and the connection are
  * left for you to build. In order:
  *
  *   1. a literal type that stays literal;
  *   2. a tuple of types as state, updated by a match type;
  *   3. evidence that the state is final, with a message of its own;
  *   4. a class per instance: path-dependent types as ownership.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch22-typed-builders-and-protocols.md`.
  *
  * (Messages quoted below drop the `typeprog.ch22typedbuildersandprotocols.`
  * prefix the compiler prints in front of every name.)
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. A literal type that stays literal
  //
  // A type parameter bounded by `String` alone infers `String`; adding
  // `Singleton` asks for the narrowest type, the literal.
  // ---------------------------------------------------------------------------

  def literal[K <: String & Singleton](key: K): K = key
  def widened[K <: String](key: K): K = key

  val kept: "milk" = literal("milk")
  val lost: String = widened("milk") // its type is String: "milk" is gone

  // ---------------------------------------------------------------------------
  // 2. A tuple of types as state
  //
  // A checklist whose items can be ticked in any order. `Left` is the tuple of
  // items still to do; ticking one removes it, with a match type that walks
  // the tuple. The `EmptyTuple` case is not optional: without it, removing
  // from an empty list is stuck instead of empty.
  // ---------------------------------------------------------------------------

  type Remove[T <: Tuple, K] <: Tuple = T match
    case EmptyTuple => EmptyTuple
    case K *: rest => rest
    case h *: rest => h *: Remove[rest, K]

  val removed = summon[Remove[("milk", "eggs", "bread"), "eggs"] =:= ("milk", "bread")]

  // ---------------------------------------------------------------------------
  // 3. Evidence that the state is final, with a message of its own
  //
  // `finish` needs nothing left to do. `Left =:= EmptyTuple` says exactly that,
  // and fails with "Cannot prove that ... =:= EmptyTuple". A class with an
  // `@implicitNotFound` message, whose only given asks for that same `=:=`,
  // fails with a sentence instead.
  // ---------------------------------------------------------------------------

  @implicitNotFound("not done yet: still to do ${Left}")
  final class AllTicked[Left <: Tuple] private ()

  object AllTicked:
    given [Left <: Tuple] => (Left =:= EmptyTuple) => AllTicked[Left] = new AllTicked

  final class Checklist[Left <: Tuple] private ():
    def tick[K <: String & Singleton](item: K): Checklist[Remove[Left, K]] = new Checklist
    def finish(using AllTicked[Left]): String = "all done"

  object Checklist:
    def apply[Items <: Tuple](): Checklist[Items] = new Checklist

  val shopping: String =
    Checklist[("milk", "eggs")]().tick("eggs").tick("milk").finish

  // `Checklist[("milk", "eggs")]().tick("eggs").finish` does not compile:
  //
  //   not done yet: still to do ("milk" : String) *: EmptyTuple.type.

  // ---------------------------------------------------------------------------
  // 4. A class per instance
  //
  // `Card` is declared inside `Deck`, so each deck has its own `deck.Card`
  // (chapter 03). A method of the deck that takes a `Card` takes only its own.
  // ---------------------------------------------------------------------------

  final class Deck:
    final class Card private[Deck] (val name: String)
    def draw(name: String): Card = Card(name)
    def discard(card: Card): String = card.name

  val red = Deck()
  val blue = Deck()

  val discarded: String = red.discard(red.draw("ace"))

  // `blue.discard(red.draw("ace"))` does not compile:
  //
  //   Found:    Walkthrough.red.Card
  //   Required: Walkthrough.blue.Card
end Walkthrough
