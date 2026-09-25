package typeprog.ch11evidencetypesafestatemachines

import typeprog.core.Unsolved

/** Exercise 04 — a door that only does what doors do.
  *
  * A door is `Opened`, `Closed` or `Locked`, and reacts to four events:
  *
  *   - `Close` takes an opened door to closed;
  *   - `Open` takes a closed door to opened;
  *   - `Lock` takes a closed door to locked;
  *   - `Unlock` takes a locked door to closed.
  *
  * `door.on[E]` is the door after event `E`, with its new state computed:
  * `Door.opened.on[Close]` is a `Door[Closed]`. Every other combination does
  * not compile, and the error names the event and the state it was tried in:
  * `Door.opened.on[Lock]` fails with `cannot Lock a door that is Opened`.
  *
  * The table is written once. The walkthrough's traffic light keeps it in a
  * type class, and its error message ends up naming the state as a path to an
  * instance's member; this door's message names the state itself.
  *
  * Hint: chapter 07 computes a type from a pair of types, and a catch-all case
  * can give the pairs with no row a result of their own. Whether an event is
  * allowed can then be derived from the table — by asking what it does *not*
  * say. The message for a missing instance is written on the type class.
  */
object Exercise04:

  sealed trait Opened
  sealed trait Closed
  sealed trait Locked

  sealed trait Open
  sealed trait Close
  sealed trait Lock
  sealed trait Unlock

  /** TODO: replace `Unsolved` with the transition table. */
  type Next[S, E] = Unsolved

  /** TODO: an instance for exactly the allowed pairs, and a message for the rest. */
  sealed trait Allowed[S, E]

  final class Door[S] private ():

    /** TODO: only for an allowed event, into the state the table gives. */
    def on[E]: Door[Unsolved] = ???

  object Door:
    def opened: Door[Opened] = new Door
