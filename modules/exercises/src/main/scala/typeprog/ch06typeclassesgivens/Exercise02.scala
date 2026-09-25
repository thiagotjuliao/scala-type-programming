package typeprog.ch06typeclassesgivens

/** Exercise 02 — an ordering found everywhere, and one found on request.
  *
  * `Version` is ordered through the instance in its companion, which is the
  * right place: an instance there is in the implicit scope of
  * `Ordering[Version]` — and of `Ordering[Option[Version]]`, and of
  * `Ordering[(Version, String)]` — so `versions.sorted` works in any file,
  * with no import. What it does is wrong: it compares the spellings, and
  * `"1.10.0"` sorts before `"1.9.0"`.
  *
  *   - Order versions by number: major first, then minor, then patch.
  *   - `Version.NewestFirst` holds a second `Ordering[Version]`, the reverse
  *     one. It is used only where it is imported — `import
  *     Version.NewestFirst.given` — and there it wins over the companion's,
  *     without being ambiguous with it.
  *
  * Neither instance should compare anything by hand.
  *
  * Hint: the standard library already orders `(Int, Int, Int)`, and
  * `Ordering.by` turns an instance for one type into an instance for another.
  * As for the second instance, the compiler searches in two places, one after
  * the other.
  */
object Exercise02:

  final case class Version(major: Int, minor: Int, patch: Int):
    override def toString: String = s"$major.$minor.$patch"

  object Version:

    /** TODO: by number, not by spelling. */
    given Ordering[Version] = Ordering.by(v => (v.major, v.minor, v.patch))

    object NewestFirst:

      /** TODO: implement. */
      given Ordering[Version] = Ordering.by(v => (-v.major, -v.minor, -v.patch))
