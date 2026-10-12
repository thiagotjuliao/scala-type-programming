package typeprog.ch25macrocheckedinterpolator

import WalkthroughMacros.*

/** Chapter 25 — Macro-checked interpolator.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it. The interpolators it calls are in
  * `WalkthroughMacros.scala`, next to it — a macro cannot be called from the
  * file that defines it.
  *
  * The examples are interpolators of their own, so that `json"..."` is left
  * for you to build. In order:
  *
  *   1. what an interpolator receives: the parts, raw, and the arguments'
  *      types, narrower than declared;
  *   2. `hex"..."`: a literal read while compiling, with the error underlined
  *      inside it, and the result lifted into the program;
  *   3. `show"..."`: every argument written by an instance found for its type,
  *      and a compile error for a type with none.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch25-macro-checked-interpolator.md`.
  */
object Walkthrough:

  val n = 1
  val name = "Ada"

  // ---------------------------------------------------------------------------
  // 1. What an interpolator receives
  //
  // `received"a${n}b"` is `StringContext("a", "b").received(n)`: the literal,
  // split at the holes, and the arguments in order. The macro takes the call
  // apart with `'{ StringContext(${ Varargs(parts) }*) }`, and binds each
  // argument's type with `'{ $a: t }`.
  // ---------------------------------------------------------------------------

  // The type bound for `$n` is `n.type`, and for `${1}` the literal type `1`
  // (chapter 04). An instance for `Int` is not one for `n.type` — `widen` is
  // the type the argument was declared with.
  val holes: String = received"a${n}b${1}c${"s"}d"
  // "parts: a | b | c | d; types: typeprog.ch25macrocheckedinterpolator.Walkthrough.n.type widens to scala.Int,
  //  1 widens to scala.Int, "s" widens to java.lang.String"

  // The parts are raw: `\n` arrives as a backslash and an `n`, for the
  // interpolator to read as it sees fit.
  val raw: String = received"""x\ny"""
  // "parts: x\ny"

  // ---------------------------------------------------------------------------
  // 2. A literal read while compiling
  //
  // `hex` checks every character of its part, and the bytes are computed in
  // the compiler and lifted with `Expr(...)`: the program holds the list.
  // ---------------------------------------------------------------------------

  val bytes: List[Int] = hex"ff 0a  10" // List(255, 10, 16)

  // A part's position starts at its first character, so `pos.start + i` is
  // the character at index `i`, and the error is underlined there:
  //
  //   hex"ff 0g"
  //           ^
  //   not a hex digit: 'g'
  //
  // An argument has a position of its own:
  //
  //   hex"ff $n"
  //           ^
  //   hex takes no arguments
  //
  // And a `StringContext` held in a `val` is not an interpolation — its parts
  // are not in the call for the pattern to find:
  //
  //   val sc = StringContext("ff")
  //   sc.hex()
  //   ^^
  //   write the parts as a literal

  // ---------------------------------------------------------------------------
  // 3. Arguments, each checked by its type
  //
  // For every argument, `Expr.summon[Show[w]]` with `w` the widened type: the
  // instance is found while compiling, and the code left in the program is
  // its `show` applied to the argument.
  // ---------------------------------------------------------------------------

  val shown: String = show"$name is ${36}, tags ${List(1, 2)}." // "Ada is 36, tags [1, 2]."

  // A type with no instance is an error at the argument, with its type
  // printed short (chapter 12):
  //
  //   show"at ${java.time.Instant.EPOCH}"
  //             ^^^^^^^^^^^^^^^^^^^^^^^
  //   no Show for Instant

end Walkthrough
