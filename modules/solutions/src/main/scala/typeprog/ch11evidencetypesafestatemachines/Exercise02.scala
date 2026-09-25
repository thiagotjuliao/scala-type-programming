package typeprog.ch11evidencetypesafestatemachines

import scala.util.NotGiven

/** Exercise 02 — chapter 06's JSON rules, without a priority trick. *Solved.*
  *
  * `NotGiven[Numeric[A]]` has an instance exactly when a search for
  * `Numeric[A]` fails. As a premise of the quoted rule, it means the rule
  * applies only to types that are *not* numbers. For `Int`, the quoted rule's
  * premise fails and only the number rule remains; for `String`, the number
  * rule's premise fails and only the quoted rule remains. At most one rule
  * ever applies, so nothing needs ranking, and both live in the companion side
  * by side.
  *
  * The difference from chapter 06's answer is where the knowledge lives. The
  * low-priority trait makes one rule the fallback of the other, and keeps both
  * applicable. `NotGiven` makes the rules disjoint: the quoted rule states its
  * own limit, and would stay correct wherever the number rule was defined.
  *
  * The trap is that `NotGiven` negates *the search succeeding*: a search for
  * `Numeric[A]` that is ambiguous also fails, and `NotGiven` then holds.
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

  object Json:
    given numeric: [A: Numeric] => Json[A] = _.toString

    given quoted: [A: Show] => NotGiven[Numeric[A]] => Json[A] =
      a => "\"" + summon[Show[A]].show(a) + "\""

  def encode[A: Json](a: A): String = summon[Json[A]].encode(a)
