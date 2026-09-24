package typeprog.ch03pathdependenttypes

/** Exercise 03 — keep the dependency in a function value.
  *
  * A method can return `Option[k.Value]`. A function *value* can too, but only
  * if its type says so, and as shipped these two say `Option[Any]`:
  *
  *   - `parser` takes a key and returns that key's own parser, from raw text to
  *     an `Option` of the key's value type — `parser(port)` is a
  *     `String => Option[Int]`;
  *   - `loadWith(raw)(read)` looks a key's name up in `raw`, parses what it
  *     finds with `read`, and hands back a function from keys to their parsed
  *     values — `loadWith(raw)(parser)(port)` is an `Option[Int]`, and a key
  *     that is missing from `raw` or does not parse gives `None`.
  *
  * The spec also hands `loadWith` a reader that parses every key as a port. Its
  * type has to refuse that: a reader must answer each key at *that* key's type.
  *
  * Hint: the parameter of a function type can be named, and its result can
  * mention it. The same syntax goes in a `val`'s type, a parameter's type and a
  * result type.
  */
object Exercise03:

  /** TODO: the type, and the value. */
  val parser: Key => String => Option[Any] = _ => _ => ???

  /** TODO: the type of `read`, the result type, and the body. */
  def loadWith(raw: Map[String, String])(read: Key => String => Option[Any]): Key => Option[Any] =
    ???
