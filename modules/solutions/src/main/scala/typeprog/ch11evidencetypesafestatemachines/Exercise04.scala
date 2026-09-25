package typeprog.ch11evidencetypesafestatemachines

import scala.annotation.implicitNotFound
import scala.util.NotGiven

/** Exercise 04 — a door that only does what doors do. *Solved.*
  *
  * The table is a match type on the pair `(state, event)` (chapter 07): four
  * rows, and a catch-all that sends every other pair to `Nowhere`. `on[E]`
  * returns `Door[Next[S, E]]`, which reduces at each call — `S` and `E` being
  * concrete there — to the state the row gives: `Door.opened.on[Close]` is a
  * `Door[Closed]`, and the chain carries on from `Closed`.
  *
  * The permission is derived from the same table rather than written again.
  * `Allowed[S, E]` has a single given, whose premise is `NotGiven[Next[S, E]
  * =:= Nowhere]`: it exists exactly when the table has a row for the pair.
  * With no instance, the call fails with `Allowed`'s `@implicitNotFound`, in
  * which `${S}` is the state *type* — `Locked` — because that is what the
  * previous call returned.
  *
  * The type class version, as in the walkthrough's traffic light, is the near
  * miss. It works, and it returns `Door[t.Next]`: a path to the found
  * instance's member. The next failure's message then names the state as that
  * path — *"a light that is ….given_Aux_Amber_Timer_Red.Next"* — and a check
  * for the word `Locked` in it would pass only because of how the compiler
  * happened to name the given.
  */
object Exercise04:

  sealed trait Opened
  sealed trait Closed
  sealed trait Locked

  sealed trait Open
  sealed trait Close
  sealed trait Lock
  sealed trait Unlock

  /** What every pair without a row leads to. */
  sealed trait Nowhere

  type Next[S, E] = (S, E) match
    case (Opened, Close) => Closed
    case (Closed, Open) => Opened
    case (Closed, Lock) => Locked
    case (Locked, Unlock) => Closed
    case _ => Nowhere

  @implicitNotFound("cannot ${E} a door that is ${S}")
  sealed trait Allowed[S, E]

  object Allowed:
    given [S, E] => NotGiven[Next[S, E] =:= Nowhere] => Allowed[S, E] = new Allowed[S, E] {}

  final class Door[S] private ():
    def on[E](using Allowed[S, E]): Door[Next[S, E]] = new Door

  object Door:
    def opened: Door[Opened] = new Door
end Exercise04
