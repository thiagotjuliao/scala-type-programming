package typeprog.ch06typeclassesgivens

/** Exercise 01 — a Show for everything built from showable parts. *Solved.*
  *
  * Each new instance is a rule with a premise: `[A: Show] => Show[List[A]]`
  * reads *if there is a `Show[A]`, there is a `Show[List[A]]`*. Asked for a
  * `Show[List[Option[List[Int]]]]`, the compiler applies the list rule, then
  * the option rule, then the list rule again, and stops at `Show[Int]`: an
  * instance for a type nobody wrote one for, assembled at compile time from
  * three that were written. When a premise fails — `List[Widget]` — the whole
  * rule does not apply, which is what makes the `Widget` specs reject.
  *
  * The near miss is an unconditional rule, `[A] => Show[List[A]]` rendering the
  * elements with `toString`. It compiles, it is found for every list, and it
  * accepts `List(Widget())` — the premise is what carries the element's
  * instance *and* what keeps unrenderable lists out.
  *
  * The rules live in `Show`'s companion, which is in the implicit scope of
  * every `Show[…]` search, so callers find them with no import. Declared at the
  * top of `Exercise01` instead, they would need `import Exercise01.given` —
  * `import Exercise01.*`, which is what the spec does, does not bring givens in.
  *
  * `describe` takes a context bound, `[A: Show as s]`: the same as a `using
  * s: Show[A]` clause, with the demand written where the type parameter is. It
  * is what makes `describe(Widget())` a compile error instead of a runtime one.
  */
object Exercise01:

  trait Show[A]:
    def show(a: A): String

  object Show:
    given Show[Int] = i => i.toString
    given Show[String] = s => s"\"$s\""

    given list: [A: Show as element] => Show[List[A]] =
      xs => xs.map(element.show).mkString("[", ", ", "]")

    given option: [A: Show as element] => Show[Option[A]] =
      case Some(a) => s"Some(${element.show(a)})"
      case None => "None"

  def describe[A: Show as s](a: A): String = s.show(a)
