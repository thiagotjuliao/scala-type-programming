package typeprog.ch06typeclassesgivens

/** Exercise 03 — two rules apply; say which one wins. *Solved.*
  *
  * Both rules are `[A: …] => Json[A]`: the same shape, differing only in their
  * premise. For `Json[Int]` both premises hold, and the types give the
  * compiler nothing to prefer one by. Side by side in one object they are
  * ambiguous:
  *
  * {{{
  * Ambiguous given instances: both given instance quoted in object Json and
  * given instance numeric in object Json match type Json[Int] …
  * }}}
  *
  * — and swapping their order in the source changes nothing.
  *
  * Where an instance is *defined* is a tie-breaker the compiler does use: a
  * given in an object wins over one in a class or trait that object extends.
  * So the rule that should lose goes into a parent trait of the companion —
  * the *low-priority trait* — and the rule that should win into the companion
  * itself. `Int` then encodes as a number; `String` and `Boolean` still reach
  * the quoted rule, because the number rule's premise fails for them and it
  * is not a candidate at all; `BigDecimal` reaches the number rule by the same
  * argument the other way round.
  *
  * The priority only settles ties. It does not make the fallback unreachable,
  * and it does not need either rule to know about the other — which is what
  * the obvious alternative, a `NotGiven[Numeric[A]]` premise on the quoted
  * rule (chapter 11), would.
  */
object Exercise03:

  trait Show[A]:
    def show(a: A): String

  object Show:
    given Show[Int] = _.toString
    given Show[String] = s => s
    given Show[Boolean] = _.toString

  trait Json[A]:
    def encode(a: A): String

  trait Fallbacks:
    given quoted: [A: Show] => Json[A] = a => "\"" + summon[Show[A]].show(a) + "\""

  object Json extends Fallbacks:
    given numeric: [A: Numeric] => Json[A] = _.toString

  def encode[A: Json](a: A): String = summon[Json[A]].encode(a)
