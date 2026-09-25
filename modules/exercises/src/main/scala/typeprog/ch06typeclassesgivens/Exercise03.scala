package typeprog.ch06typeclassesgivens

/** Exercise 03 — two rules apply; say which one wins.
  *
  * `Json` has two rules, one for each way a value can be written:
  *
  *   - anything with a `Numeric` is a bare number: `42`, `2.5`;
  *   - anything with a `Show` is a quoted string: `"hi"`, `"true"`.
  *
  * An `Int` has both. As shipped, the string rule wins and `encode(42)` is
  * `"\"42\""`. Make the number rule win wherever both apply, and leave each
  * rule in charge where it is the only one:
  *
  *   - `encode(42)` is `42`; `encode(BigDecimal("1.5"))` is `1.5`, and
  *     `encode("hi")` is `"hi"` with its quotes;
  *   - a type with neither a `Numeric` nor a `Show` cannot be encoded;
  *   - both rules stay, as rules: no instance for `Int` in particular, and no
  *     `Show[Int]` or `Numeric[Int]` removed.
  *
  * Putting the two rules side by side in `Json`'s companion does not work, and
  * it is worth trying once to read what the compiler says.
  *
  * Hint: the two rules are equally specific, so their types cannot rank them.
  * Where each one is *defined* can.
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

  /** TODO: the rules are right; which one wins is not. */
  trait Fallbacks:
    given numeric: [A: Numeric] => Json[A] = _.toString

  object Json extends Fallbacks:
    given quoted: [A: Show] => Json[A] = a => "\"" + summon[Show[A]].show(a) + "\""

  def encode[A: Json](a: A): String = summon[Json[A]].encode(a)
