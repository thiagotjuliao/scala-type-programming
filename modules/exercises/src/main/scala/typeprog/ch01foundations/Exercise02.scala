package typeprog.ch01foundations

import typeprog.core.Unsolved

/** Exercise 02 — refine a trait's type members.
  *
  * `Repo` leaves both of its types abstract, so `find` on a bare `Repo` takes
  * an unknowable `Id` and returns an unknowable `Entity` — a signature no
  * caller can satisfy.
  *
  * Define `DogRepo` as the refinement of `Repo` whose `Id` is `Long` and whose
  * `Entity` is `Dog`, so that `find(1L)` type-checks and returns
  * `Option[Dog]`.
  *
  * Hint: a refinement is written `Trait { type Member = Concrete }`, and the
  * result is an ordinary subtype of `Trait`.
  */
object Exercise02:

  trait Repo:
    type Id
    type Entity
    def find(id: Id): Option[Entity]

  /** TODO: replace `Unsolved` with the refinement described above. */
  type DogRepo = Unsolved
