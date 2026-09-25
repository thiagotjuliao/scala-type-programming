package typeprog.ch05opaquetypes

/** Exercise 02 — let a Port be read as an Int, and not made from one.
  *
  * `Port` is opaque already, and too opaque: outside `Exercise02` a port
  * cannot even be used as the number it is, so `Port.Http + 1` and
  * `val n: Int = Port.Http` are both rejected. The relation wanted goes one way:
  *
  *   - a `Port` can be used wherever an `Int` is expected, and arithmetic on
  *     one gives an `Int`;
  *   - an `Int` is still not a `Port` — the only ways to get one are the named
  *     ports and `Port.from`, which accepts `1` to `65535`.
  *
  * Hint: an opaque type can declare an upper bound, and the bound is the part
  * of the representation that is public.
  */
object Exercise02:

  /** TODO: every port should be usable as an `Int`. */
  opaque type Port <: Int = Int

  object Port:
    val Http: Port = 80
    val Https: Port = 443

    /** TODO: implement. */
    def from(n: Int): Option[Port] =
      Option.when(n >= 1 && n <= 65535)(n)
