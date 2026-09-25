package typeprog.ch08inlinescalacompiletime

/** Exercise 02 — a default value for every type that has one.
  *
  * `default[T]` is the value a `T` starts from:
  *
  *   - `0` for an `Int`, `0L` for a `Long`, `0.0` for a `Double`;
  *   - `false` for a `Boolean` and `""` for a `String`;
  *   - `None` for any `Option`, and `Nil` for any `List`, whatever they hold;
  *   - for a pair, the pair of the two components' defaults — and pairs nest:
  *     `default[(Boolean, (Long, Option[Int]))]` is `(false, (0L, None))`.
  *
  * Any other type — `Char`, a class of your own, a pair containing one — does
  * not compile, and the error says `no default value`.
  *
  * There is no value of `T` to look at, only the type.
  *
  * Hint: a match can be made on a type through a stand-in value that is never
  * evaluated, and its patterns can name the components of a pair.
  */
object Exercise02:

  /** TODO: the default for `T`, or a compile error. */
  inline def default[T]: T = ???
