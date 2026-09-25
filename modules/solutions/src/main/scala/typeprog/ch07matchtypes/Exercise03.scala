package typeprog.ch07matchtypes

/** Exercise 03 — a result type computed from the argument's. *Solved.*
  *
  * `First` is the table; `first` is the same table over values. The `match`
  * in the body has one typed pattern per case of `First`, with the same types
  * in the same order, and that is what lets the compiler check each branch
  * against the *reduced* type: `s.headOption` against `Option[Char]`,
  * `l.headOption` against `Option[a]`. Without that, every branch would be
  * checked against `First[X]` for an unknown `X` — a stuck match type nothing
  * conforms to — and rejected:
  *
  * {{{
  * Found:    Option[Char]
  * Required: First[X]
  * }}}
  *
  * The conditions are brittle on purpose. An extra `case _ => None`, added to
  * be safe, breaks every branch, not just the extra one; so does swapping two
  * cases. The value match and the type match have to be the same match.
  *
  * The bound `X <: String | List[?] | Option[?]` is what rejects `first(42)`.
  * Without it the call compiles — `First[Int]` is merely stuck, and a stuck
  * type is still a type — with a warning at best, *"Match type reduction
  * failed since selector Int matches none of the cases"*, and a `MatchError`
  * when it runs.
  *
  * This works because `String`, `List` and `Option` are classes a runtime
  * type test can tell apart. Cases distinguished only by type arguments —
  * `List[Int]` against `List[String]` — could be written as a match type, but
  * not as this `match`: the arguments are erased (see the walkthrough, section
  * 6).
  */
object Exercise03:

  type First[X] = X match
    case String => Option[Char]
    case List[a] => Option[a]
    case Option[a] => Option[a]

  def first[X <: String | List[?] | Option[?]](x: X): First[X] = x match
    case s: String => s.headOption
    case l: List[a] => l.headOption
    case o: Option[a] => o
