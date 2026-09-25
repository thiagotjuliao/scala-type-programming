package typeprog.ch05opaquetypes

/** Exercise 01 — a String on the inside, an Email on the outside.
  *
  * As shipped, `Email` is an ordinary alias: it *is* `String`, everywhere. Any
  * string can be passed as an email, an email can be passed as any string, and
  * `"not an email".domain` compiles. Make the type protect something:
  *
  *   - outside `Exercise01`, an `Email` is not a `String` and a `String` is not
  *     an `Email` — in either direction — and `String`'s own methods are not
  *     available on an `Email`;
  *   - the only way to get one is `Email.parse`, which accepts an address with
  *     exactly one `@` and at least one character on each side of it;
  *   - `domain` (what follows the `@`) and `value` (the whole address) work on
  *     an `Email`, and not on a `String`;
  *   - at runtime an `Email` is still just the `String` — no wrapper object.
  *
  * Hint: one keyword turns an alias into a boundary. Inside the object that
  * defines it, the alias is still transparent, which is where the bodies can
  * treat an `Email` as the `String` it is.
  */
object Exercise01:

  /** TODO: make it stop being a `String` outside this object. */
  opaque type Email = String

  private val EmailPattern = "([^@\\s]+)@([^@\\s]+\\.[^@\\s]+)".r

  object Email:

    /** TODO: implement. */
    def parse(raw: String): Option[Email] =
      raw match
        case EmailPattern(_, _) => Some(raw)
        case _ => None

  extension (email: Email)

    /** TODO: implement. */
    def domain: String =
      email match
        case EmailPattern(_, domain) => domain

    /** TODO: implement. */
    def value: String = email
end Exercise01
