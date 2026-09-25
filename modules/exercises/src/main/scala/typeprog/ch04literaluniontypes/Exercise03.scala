package typeprog.ch04literaluniontypes

import typeprog.core.Unsolved

/** Exercise 03 — a result that lists every way it can fail.
  *
  * Reading a port from the environment can go wrong three ways, and each has
  * its own error class. They share no parent, and nothing here asks them to:
  *
  *   - the key is absent — `Missing`, which `lookup` already returns;
  *   - the value is not a number — `Malformed`, which `number` already returns;
  *   - the number is outside `1` to `65535` — `OutOfRange`.
  *
  * Define `PortResult` as the port or any one of the three, and implement:
  *
  *   - `port`, reading the key `"port"` through `lookup` and `number` and
  *     checking the range;
  *   - `explain`, which turns any `PortResult` into one line: `"port 8080"`,
  *     `"port is not set"`, `"port is not a number: x"`,
  *     `"port is out of range: 0"`.
  *
  * Hint: `A | B` needs no common supertype, and a `match` asks which member of
  * a union a value is. The errors `lookup` and `number` return are already in
  * the right shape to be passed straight through.
  */
object Exercise03:

  final case class Missing(key: String)
  final case class Malformed(key: String, raw: String)
  final case class OutOfRange(key: String, value: Int)

  def lookup(env: Map[String, String], key: String): String | Missing =
    env.getOrElse(key, Missing(key))

  def number(key: String, raw: String): Int | Malformed =
    raw.toIntOption.getOrElse(Malformed(key, raw))

  /** TODO: replace `Unsolved` with the port, or each way of not getting one. */
  type PortResult = Int | Missing | Malformed | OutOfRange

  /** TODO: implement. */
  def port(env: Map[String, String]): PortResult =
    lookup(env, "port") match
      case raw: String =>
        number("port", raw) match
          case p: Int =>
            if p < 1 | p > 65535 then OutOfRange("port", p) else p
          case m: Malformed => m
      case m: Missing => m

  /** TODO: implement. */
  def explain(result: PortResult): String =
    result match
      case p: Int => s"port $p"
      case _: Missing => s"port is not set"
      case Malformed(_, r) => s"port is not a number: $r"
      case OutOfRange(_, v) => s"port is out of range: $v"

end Exercise03
