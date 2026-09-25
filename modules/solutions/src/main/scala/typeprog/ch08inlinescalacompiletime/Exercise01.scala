package typeprog.ch08inlinescalacompiletime

import scala.compiletime.{constValue, error}

/** Exercise 01 — a port checked while compiling. *Solved.*
  *
  * `constValue[N]` is the value of the literal type `N` — `8080` for
  * `Port[8080]` — and it is a constant, so an `inline if` can decide on it
  * while the call is expanded. The branch that is not taken is discarded: for
  * a valid port, the whole method compiles to the number; for an invalid one,
  * the expansion reaches `error`, and the compilation stops with its message.
  * Nothing of the check is left at runtime.
  *
  * For a type that is not a literal, `constValue` has no value to read and
  * fails on its own — *"Int is not a constant type; cannot take
  * constValue"* — which is how `Port[Int]` and a generic `Port[N]` are
  * rejected without a line written for them.
  *
  * The near miss is a plain `if`: it compiles, and moves the check back to
  * runtime, where `error` cannot be called at all. The other is a method
  * without `inline`: `constValue` needs the call site's `N`, and outside an
  * inline method there is only the declaration's.
  */
object Exercise01:

  opaque type Port = Int

  object Port:
    inline def apply[N <: Int]: Port =
      inline if constValue[N] < 1 || constValue[N] > 65535 then
        error("a port is between 1 and 65535")
      else constValue[N]

  extension (p: Port) def number: Int = p
