package typeprog.ch05opaquetypes

/** Exercise 01 — a String on the inside, an Email on the outside. *Solved.*
  *
  * `opaque` is the whole type-level answer. Inside `Exercise01` the alias is
  * still transparent — `parse` returns the very `String` it checked, and
  * `domain` calls `String`'s `substring` on an `Email` — while outside, `Email`
  * is an abstract type with no relation to `String` in either direction. None
  * of `String`'s methods cross the boundary; only what this object publishes
  * does: the constructor in the companion and the two extensions.
  *
  * The near miss is the one the stub shipped: `type Email = String`. It
  * compiles every line of the solution and every line the spec expects to be
  * rejected, because a transparent alias *is* the type it names — including
  * letting `"anything".domain` through, since an extension on `Email` is then an
  * extension on every `String`.
  *
  * The runtime cost is nothing. There is no class `Email`; an `Email` is the
  * `String`, stored and passed as one, and `Email.parse` allocates exactly the
  * `Some` it returns. The validation lives in the only way to build a value, so
  * every `Email` anywhere else was checked once, here.
  */
object Exercise01:

  opaque type Email = String

  object Email:

    def parse(raw: String): Option[Email] =
      raw.split("@", -1) match
        case Array(user, host) if user.nonEmpty && host.nonEmpty => Some(raw)
        case _ => None

  extension (email: Email)
    def domain: String = email.substring(email.indexOf('@') + 1)
    def value: String = email
