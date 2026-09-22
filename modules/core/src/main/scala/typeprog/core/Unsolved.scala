package typeprog.core

/** The answer an exercise has not been given yet.
  *
  * Type-level exercises have an awkward property: a wrong answer is usually a
  * *compile* error, and a compile error in one file takes the whole module
  * down with it. A repository full of unsolved exercises would therefore not
  * build at all, and the one thing it could never tell you is which exercise
  * you got wrong.
  *
  * `Unsolved` is how that is avoided. A stub answers with this type:
  *
  * {{{
  * type Flip[P] = Unsolved // TODO: swap the two components of the pair
  * }}}
  *
  * which compiles perfectly well — `Unsolved` is a type like any other — and
  * satisfies nothing. Every assertion about `Flip` then fails as a red test
  * with a readable message, and the rest of the repository keeps building.
  *
  * It is uninhabited (`sealed` with no implementations, and no public
  * constructor), so it cannot leak into a solution by accident: the moment an
  * exercise is genuinely solved, this type stops appearing in it.
  */
sealed trait Unsolved
