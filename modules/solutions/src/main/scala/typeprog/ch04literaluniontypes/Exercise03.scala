package typeprog.ch04literaluniontypes

/** Exercise 03 — a result that lists every way it can fail. *Solved.*
  *
  * `Int | Missing | Malformed | OutOfRange` is the whole answer, and the
  * point is what it did not need: the three error classes share no parent, and
  * no `sealed trait PortError` had to be written and inherited to put them in
  * one type. Each helper already returned its own union — `String | Missing`,
  * `Int | Malformed` — and `port` passes their errors straight through, so its
  * result is the union of theirs plus the one failure it adds. With `Either`
  * the two helpers' error types would have had to be unified first, or nested.
  *
  * The `match`es ask which member a value is, and that is also how the
  * compiler checks `explain`: a `match` over a union is checked for
  * exhaustiveness, so an `explain` that forgot `OutOfRange` would be reported
  * as *"match may not be exhaustive"*, naming the missing case.
  *
  * `PortResult` is declared, and `port` is declared to return it, on purpose.
  * Without the declaration the compiler infers the union in the shape it was
  * assembled — nested, in the order the branches produced it — which is the
  * same type and a worse message. The near miss is a result type of `Any`: it
  * compiles, it passes every runtime test, and it lets `explain` accept a
  * `String`, which the spec rejects.
  */
object Exercise03:

  final case class Missing(key: String)
  final case class Malformed(key: String, raw: String)
  final case class OutOfRange(key: String, value: Int)

  def lookup(env: Map[String, String], key: String): String | Missing =
    env.getOrElse(key, Missing(key))

  def number(key: String, raw: String): Int | Malformed =
    raw.toIntOption.getOrElse(Malformed(key, raw))

  type PortResult = Int | Missing | Malformed | OutOfRange

  def port(env: Map[String, String]): PortResult =
    lookup(env, "port") match
      case missing: Missing => missing
      case raw: String =>
        number("port", raw) match
          case malformed: Malformed => malformed
          case n: Int => if n >= 1 && n <= 65535 then n else OutOfRange("port", n)

  def explain(result: PortResult): String = result match
    case n: Int => s"port $n"
    case Missing(key) => s"$key is not set"
    case Malformed(key, raw) => s"$key is not a number: $raw"
    case OutOfRange(key, value) => s"$key is out of range: $value"
end Exercise03
