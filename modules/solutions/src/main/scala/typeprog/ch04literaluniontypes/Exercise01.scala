package typeprog.ch04literaluniontypes

/** Exercise 01 — make a type out of three values. *Solved.*
  *
  * `200 | 404 | 500` is a union of three literal types, and a literal type has
  * exactly one inhabitant, so the union has exactly three. `describe(201)` is
  * rejected because `(201 : Int)` is none of them; `describe(code)` with
  * `code: Int` is rejected because an `Int` might be any of four billion
  * values, and the compiler only accepts what it can see is one of the three.
  *
  * The constants are the half that looks finished and is not. `val Ok = 200`
  * compiles and is an `Int`: inference widens a literal at every unannotated
  * `val`, so the constant was never usable as a `Status`. A `final val` member
  * keeps the literal type — so would `val Ok: 200 = 200`, or an `inline val`.
  * Ascribing `val Ok: Status = 200` is the near miss: it is usable as a
  * `Status`, but `Ok` is then *some* status, and `Ok.type <: 200` no longer
  * holds.
  *
  * The `match` in `describe` needs no default case: the compiler checks a
  * match on a union of literals for exhaustiveness, and a code left out is
  * reported — *"It would fail on pattern case: 500"*.
  */
object Exercise01:

  type Status = 200 | 404 | 500

  final val Ok = 200
  final val NotFound = 404
  final val ServerError = 500

  def describe(status: Status): String = status match
    case 200 => "OK"
    case 404 => "Not Found"
    case 500 => "Internal Server Error"
