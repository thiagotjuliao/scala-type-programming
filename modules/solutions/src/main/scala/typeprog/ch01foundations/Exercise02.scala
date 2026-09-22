package typeprog.ch01foundations

/** Exercise 02 — refine a trait's type members. *Solved.*
  *
  * The refinement fixes both abstract members in the type itself, without
  * declaring a new trait and without writing an implementation. `find` on a
  * `DogRepo` is therefore `(Long) => Option[Dog]`, checked at every call site.
  *
  * Worth noticing: `DogRepo <: Repo` holds for free. Refinement is subtyping —
  * the refined type is the same trait with strictly more known about it, so it
  * conforms wherever the unrefined one does.
  */
object Exercise02:

  trait Repo:
    type Id
    type Entity
    def find(id: Id): Option[Entity]

  type DogRepo = Repo { type Id = Long; type Entity = Dog }
