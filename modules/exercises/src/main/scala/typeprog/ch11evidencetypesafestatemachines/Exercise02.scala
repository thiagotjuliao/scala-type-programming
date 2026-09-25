package typeprog.ch11evidencetypesafestatemachines

/** Exercise 02 — chapter 06's JSON rules, without a priority trick.
  *
  * `Json` has two rules: anything with a `Numeric` is a bare number, anything
  * with a `Show` is a quoted string. An `Int` has both, and must be a number.
  * As shipped, the rules are ranked the wrong way round with chapter 06's
  * low-priority trait, and `encode(42)` is `"\"42\""`.
  *
  * Put both rules in `Json`'s companion — no parent trait, no ranking — and
  * make each apply only where it should:
  *
  *   - `encode(42)`, `encode(2.5)`, `encode(BigDecimal("1.5"))` are bare
  *     numbers;
  *   - `encode("hi")` and `encode(true)` are quoted;
  *   - a type with neither cannot be encoded, and no type is ambiguous.
  *
  * Chapter 06's answer — move the losing rule into the parent — also passes
  * the spec. It is not the answer here.
  *
  * Hint: the string rule's premise can say what the type must *not* have.
  */
object Exercise02:

  trait Show[A]:
    def show(a: A): String

  object Show:
    given Show[Int] = _.toString
    given Show[String] = s => s
    given Show[Boolean] = _.toString

  trait Json[A]:
    def encode(a: A): String

  /** TODO: this parent trait goes; its rule moves into the companion. */
  trait Fallbacks:
    given numeric: [A: Numeric] => Json[A] = _.toString

  object Json extends Fallbacks:
    given quoted: [A: Show] => Json[A] = a => "\"" + summon[Show[A]].show(a) + "\""

  def encode[A: Json](a: A): String = summon[Json[A]].encode(a)
