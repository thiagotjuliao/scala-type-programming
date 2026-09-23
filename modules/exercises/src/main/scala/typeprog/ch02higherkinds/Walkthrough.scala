package typeprog.ch02higherkinds

import typeprog.core.evidence

/** Chapter 02 — higher-kinded types & type lambdas.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The chapter covers four things, in order:
  *
  *   1. kinds — the types of types, and why `List` alone is not one;
  *   2. higher-kinded parameters — abstracting over `List` rather than
  *      `List[Int]`;
  *   3. type lambdas — making a one-hole constructor out of a two-hole one;
  *   4. inference — which hole the compiler finds by itself, and which it
  *      never will.
  *
  * Prose version, with the motivation and the pitfalls: `docs/theory/ch02-higher-kinds.md`.
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. Kinds
  //
  // A value has a type. A type has a *kind*, which says how many type arguments
  // it still needs before anything can have it:
  //
  //   Int, List[Int]          *               values live here
  //   List, Option            * -> *          one argument short
  //   Either, Map             * -> * -> *     two arguments short
  // ---------------------------------------------------------------------------

  val numbers: List[Int] = List(1, 2, 3) // `List[Int]` is `*`: it has values

  // `List` alone is not a type anything can have. It is a function at the type
  // level, waiting to be applied:
  //
  //   val xs: List = numbers   // Missing type parameter for List

  // ---------------------------------------------------------------------------
  // 2. Higher-kinded parameters
  //
  // Three methods that add one to every element differ only in their container.
  // A type parameter `C` for `List[Int]` cannot merge them: `C` is finished,
  // and there is no way to say "`C` but with `String` inside". What they share
  // is the constructor, so that is what gets abstracted.
  // ---------------------------------------------------------------------------

  // `Functor[F[_]]` is declared in `Functor.scala`, next to this file. The
  // `F[_]` says: `F` takes one type argument. Its own kind is therefore
  // `(* -> *) -> *` — a type constructor over type constructors, exactly as
  // `map` is a function over functions.

  val listFunctor: Functor[List] = new Functor[List]:
    def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)

  val optionFunctor: Functor[Option] = new Functor[Option]:
    def map[A, B](fa: Option[A])(f: A => B): Option[B] = fa.map(f)

  /** Written once, for every `F` that has a `Functor`. The body never learns
    * what `F` is — it can only go through `F.map`.
    */
  def inc[F[_]](fa: F[Int])(F: Functor[F]): F[Int] = F.map(fa)(_ + 1)

  val incremented: List[Int] = inc(numbers)(listFunctor)
  val incrementedOption: Option[Int] = inc(Option(41))(optionFunctor)

  // The kind is checked before anything else — no member lookup, just arity:
  //
  //   Functor[Int]      // Type argument Int does not have the same kind as its bound [_$1]
  //   Functor[Either]   // the same message: one parameter too many

  // ---------------------------------------------------------------------------
  // 3. Type lambdas
  //
  // `Map[String, Int]` is `*` and `Map` is `* -> * -> *`: neither is the
  // `* -> *` a `Functor` wants. Fixing the key type gives one, and Scala 3
  // writes that as a function at the type level.
  // ---------------------------------------------------------------------------

  /** Read `[X] =>> ...` exactly like `x => ...`: a parameter, an arrow, a body. */
  val mapValuesFunctor: Functor[[X] =>> Map[String, X]] = new Functor[[X] =>> Map[String, X]]:
    def map[A, B](fa: Map[String, A])(f: A => B): Map[String, B] = fa.view.mapValues(f).toMap

  // Named, a type lambda is an ordinary alias. With a parameter list of its own
  // it becomes *curried*: `Keyed[K]` is itself a one-hole constructor, which
  // is what makes it partially applicable. A two-parameter alias,
  // `type Keyed2[K, V] = Map[K, V]`, could only ever be applied to both.
  type Keyed[K] = [V] =>> Map[K, V]

  evidence.sameType[Keyed[String][Int], Map[String, Int]]
  evidence.sameType[Functor[Keyed[String]], Functor[[X] =>> Map[String, X]]]

  // A type lambda can also repeat its parameter, which no named constructor in
  // the standard library does:
  type Twice = [X] =>> (X, X)

  val pairFunctor: Functor[Twice] = new Functor[Twice]:
    def map[A, B](fa: (A, A))(f: A => B): (B, B) = (f(fa._1), f(fa._2))

  val bothIncremented: (Int, Int) = inc[Twice]((1, 2))(pairFunctor)

  // Beware the Scala 2 habit. In Scala 3 an underscore in argument position is
  // a *wildcard*, not a hole, so `Map[String, _]` is a proper type:
  //
  //   Functor[Map[String, _]]
  //   // Type argument Map[String, ?] does not have the same kind as its bound [_$1]

  // ---------------------------------------------------------------------------
  // 4. Inference fills the hole from the right
  //
  // Asked to solve `F[Int] = Map[String, Int]`, the compiler lines `Int` up
  // with the *last* type argument and takes everything before it as `F`.
  // ---------------------------------------------------------------------------

  val scores: Map[String, Int] = Map("ada" -> 1, "alan" -> 2)

  // No `F` written anywhere: it is inferred as `[X] =>> Map[String, X]`, and
  // the functor is checked against that.
  val bumped: Map[String, Int] = inc(scores)(mapValuesFunctor)

  // The same rule sees a function `String => Int` as `F[Int]`, with
  // `F = [X] =>> String => X` — the result type is its last parameter.
  val readerFunctor: Functor[[X] =>> String => X] = new Functor[[X] =>> String => X]:
    def map[A, B](fa: String => A)(f: A => B): String => B = fa.andThen(f)

  val lengthPlusOne: String => Int = inc((s: String) => s.length)(readerFunctor)

  // The rule is also the limit. A tuple whose *first* component varies is a
  // perfectly good constructor, but inference will never cut `(Int, String)`
  // that way — it matches `Int` against `String`, the last argument, and stops:
  //
  //   inc(one)(firstFunctor)
  //   // Found:    (one : (Int, String))
  //   // Required: F[Int]
  //
  // Spelling `F` out is the only way to use it.
  type First[B] = [A] =>> (A, B)

  val firstFunctor: Functor[First[String]] = new Functor[First[String]]:
    def map[A, C](fa: (A, String))(f: A => C): (C, String) = (f(fa._1), fa._2)

  val one: (Int, String) = (1, "one")
  val firstIncremented: (Int, String) = inc[First[String]](one)(firstFunctor)

  // `Twice` in section 3 was spelled out for a related reason. Inference *does*
  // find an `F` for `(1, 2)` — the last-argument rule gives `[X] =>> (Int, X)`
  // — but it is the wrong one, and `pairFunctor` is then rejected against it:
  //
  //   inc((1, 2))(pairFunctor)
  //   // Found:    (pairFunctor : Functor[Twice])
  //   // Required: Functor[[T2] =>> (Int, T2)]

  // This is why libraries put the parameter that varies last: `Either[E, A]`,
  // `Map[K, V]`, `A => B`. It is a convention the compiler's inference rule
  // turned into a requirement.
end Walkthrough
