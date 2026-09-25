package typeprog.ch08inlinescalacompiletime

/** Exercise 01 — a port checked while compiling.
  *
  * Chapter 05's ports were validated at runtime, and handed back as an
  * `Option` even for a number written in the source. Make `Port[N]` take the
  * number as a literal *type* and check it while compiling:
  *
  *   - `Port[8080]` is a `Port` whose `number` is `8080` — no `Option`;
  *   - `1` and `65535` are ports, the ends of the range;
  *   - `Port[0]`, `Port[70000]`, `Port[-1]` do not compile, and the error says
  *     `a port is between 1 and 65535`;
  *   - a type that is not a literal — `Port[Int]`, or an `N` a generic method
  *     does not know — does not compile either.
  *
  * `Port` stays opaque: it is not an `Int` outside this object.
  *
  * Hint: a literal type can be read back as a value while compiling, and a
  * condition on it can be decided there too. The error message is a string the
  * compiler prints as it is.
  */
object Exercise01:

  opaque type Port = Int

  object Port:
    /** TODO: the port numbered `N`, or a compile error. */
    inline def apply[N <: Int]: Port = ???

  extension (p: Port) def number: Int = p
