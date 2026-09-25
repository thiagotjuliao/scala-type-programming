package typeprog.ch04literaluniontypes

/** Exercise 01 — make a type out of three values.
  *
  * The service answers with three status codes, and `describe` names them.
  * Taking an `Int` would accept four billion codes it has no name for, so:
  *
  *   - `Status` is exactly the codes `200`, `404` and `500` — no other `Int`,
  *     and not an `Int` whose value is only known at runtime;
  *   - `Ok`, `NotFound` and `ServerError` are usable wherever a `Status` is,
  *     and each is its own code: `Ok` is a `200`, not a `Status` in general;
  *   - `describe` answers `"OK"`, `"Not Found"` and `"Internal Server Error"`.
  *
  * As shipped, the constants compile and are useless: each one is an `Int`.
  *
  * Hint: a literal is a type as well as a value, and `|` makes one type out of
  * several. An unannotated `val` widens its literal; there are two ways to make
  * a member keep it.
  */
object Exercise01:

  /** TODO: replace `Unsolved` with the three codes. */
  type Status = 200 | 404 | 500

  /** TODO: each must keep its literal type. */
  val Ok: 200 = 200
  val NotFound: 404 = 404
  val ServerError: 500 = 500

  /** TODO: implement. */
  def describe(status: Status): String =
    status match
      case Ok => "OK"
      case NotFound => "Not Found"
      case ServerError => "Internal Server Error"
