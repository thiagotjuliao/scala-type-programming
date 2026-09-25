package typeprog.ch06typeclassesgivens

/** Exercise 02 — an ordering found everywhere, and one found on request.
  * *Solved.*
  *
  * `Ordering.by(v => (v.major, v.minor, v.patch))` asks the compiler for an
  * `Ordering[(Int, Int, Int)]`, which the standard library builds from three
  * `Ordering[Int]`s with a conditional instance of exactly the kind exercise 01
  * wrote — lexicographic on the components, numeric within each. Nothing is
  * compared by hand, so there is nothing to get wrong about `1.10` and `1.9`.
  *
  * The companion is the one place where an instance of a type class the reader
  * does not own (`Ordering`), for a type they do (`Version`), is found without
  * an import. It also serves every type `Version` is a *part* of: the implicit
  * scope of `Ordering[Option[Version]]` includes `Version`'s companion, so the
  * library's option rule finds this instance as its premise.
  *
  * `NewestFirst` works because of the order of the search, not because of any
  * priority between the two instances. The lexical scope — what is defined or
  * imported around the call — is searched first, and the implicit scope only
  * if that found nothing. Imported, `NewestFirst`'s instance is found in the
  * first place and the companion's is never looked at, so the two are never
  * compared and cannot be ambiguous. Declared as a second given in the
  * companion itself, the two would meet in the same search, and the result is
  * *"Ambiguous given instances"* at every `sorted` in the program.
  *
  * `.reverse` builds the second from the first, so the two can never disagree
  * about anything but the direction.
  */
object Exercise02:

  final case class Version(major: Int, minor: Int, patch: Int):
    override def toString: String = s"$major.$minor.$patch"

  object Version:

    given ascending: Ordering[Version] = Ordering.by(v => (v.major, v.minor, v.patch))

    object NewestFirst:
      given newestFirst: Ordering[Version] = ascending.reverse
