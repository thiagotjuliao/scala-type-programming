package typeprog.ch06typeclassesgivens

/** Exercise 01 — a Show for everything built from showable parts.
  *
  * `Show` has instances for `Int` and `String`, and that is all. Extend it so
  * that, anywhere and with no import:
  *
  *   - a `List[A]` can be shown whenever an `A` can, as `[1, 2, 3]` — the
  *     elements rendered by *their* instance, separated by `", "`;
  *   - an `Option[A]` likewise, as `Some(…)` around the element's rendering,
  *     or `None`;
  *   - the two combine to any depth: a `List[Option[List[Int]]]` has one;
  *   - a list or option of something with no `Show` has none.
  *
  * Then give `describe` the signature it should have had: it accepts an `A`
  * only if there is a `Show[A]`, and renders it with that instance. As shipped
  * it accepts anything, and cannot do anything with it.
  *
  * Hint: an instance can itself have a `using` clause — it is then a rule that
  * builds one instance from another. Where the instances are declared decides
  * whether callers need an import.
  */
object Exercise01:

  trait Show[A]:
    def show(a: A): String

  object Show:
    given Show[Int] = i => i.toString
    given Show[String] = s => s"\"$s\""

    // TODO: instances for List[A] and Option[A], for an A that has a Show.

  /** TODO: only for an `A` that has a `Show`; render it with that instance. */
  def describe[A](a: A): String = ???
