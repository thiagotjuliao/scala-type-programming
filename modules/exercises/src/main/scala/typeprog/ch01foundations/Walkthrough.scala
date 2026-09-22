package typeprog.ch01foundations

import typeprog.core.evidence

/** Chapter 01 — type system foundations.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it, because a claim it stopped checking would be a
  * claim that had quietly become false.
  *
  * The chapter covers four things, in order:
  *
  *   1. subtyping — the one relation everything else is phrased in terms of;
  *   2. variance — how that relation travels into a type constructor;
  *   3. bounds — how to constrain a type parameter without fixing it;
  *   4. type members — the other way to attach a type to a trait.
  *
  * Prose version, with the motivation and the pitfalls: `docs/theory/ch01-type-foundations.md`.
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. Subtyping
  //
  // `Dog <: Animal` is not a statement about dogs. It is a permission slip:
  // wherever an `Animal` is expected, a `Dog` may be handed over instead, and
  // nothing downstream is allowed to notice.
  // ---------------------------------------------------------------------------

  val rex: Dog = Dog("Rex")
  val someAnimal: Animal = rex // upcasting needs no ceremony: it is always safe

  // The other direction is not a permission the compiler can grant, because it
  // cannot know that this particular `Animal` is a `Dog`:
  //
  //   val backToDog: Dog = someAnimal   // does not compile

  // ---------------------------------------------------------------------------
  // 2. Variance
  //
  // Now put `Dog` inside something. Does `Box[Dog] <: Box[Animal]`?
  //
  // There is no universally right answer — it depends on what `Box` does with
  // its `A`, and the annotation is where that is declared.
  // ---------------------------------------------------------------------------

  /** Covariant: `A` only ever comes *out*. */
  trait Source[+A]:
    def emit(): A

  /** Contravariant: `A` only ever goes *in*. */
  trait Sink[-A]:
    def accept(a: A): Unit

  /** Invariant: `A` does both, so neither direction is safe. */
  trait Channel[A] extends Source[A], Sink[A]

  // A source of dogs is a source of animals: whoever asked for animals gets
  // dogs, which are animals. Nothing breaks.
  evidence.subtypeOf[Source[Dog], Source[Animal]]

  // A sink of animals is a sink of dogs — this is the direction that looks
  // backwards on first reading, and is not. Something that accepts *any*
  // animal is, in particular, something you may hand dogs to. The wider the
  // appetite, the more contexts it fits.
  evidence.subtypeOf[Sink[Animal], Sink[Dog]]

  // The get/put principle, which is all of the above in one line:
  //
  //   a type parameter you only GET may be covariant     (+A)
  //   a type parameter you only PUT may be contravariant (-A)
  //   a type parameter you do both to must be invariant  ( A)
  //
  // `Function1[-T, +R]` is the canonical witness: arguments go in, results
  // come out, and the annotations follow exactly.
  val handleAnyAnimal: Animal => String = _.name
  val handleDog: Dog => String = handleAnyAnimal // -T at work
  val anySource: Source[Animal] = new Source[Dog]:
    def emit() = rex // +A at work

  // Mutability is where invariance stops being pedantry. If `Array` were
  // covariant, this would type-check:
  //
  //   val dogs: Array[Dog]       = Array(rex)
  //   val animals: Array[Animal] = dogs        // pretend this compiles
  //   animals(0) = Cat("Tom")                  // and now `dogs(0)` is a Cat
  //
  // The array has a `get` and a `put`, so it gets neither annotation.

  // ---------------------------------------------------------------------------
  // 3. Bounds
  //
  // Variance says how `F[A]` relates to `F[B]`. Bounds say which `A`s are
  // admissible in the first place.
  // ---------------------------------------------------------------------------

  /** Upper bound: `A` must conform to `Animal`, so `name` is available. */
  def loudest[A <: Animal](as: List[A]): Option[String] =
    as.map(_.name.toUpperCase).sorted.lastOption

  /** Lower bound: the classic escape hatch for a covariant parameter.
    *
    * `prepend` wants to *put* an `A` into a covariant structure, which the
    * get/put principle forbids. Widening the method's own parameter to some
    * `B >: A` restores it: nothing is put into the original structure, a wider
    * one is returned instead.
    */
  def prepend[A, B >: A](a: B, as: List[A]): List[B] = a :: as

  val kennel: List[Dog] = List(rex)
  val mixed: List[Animal] = prepend(Cat("Tom"), kennel) // B is inferred as Animal

  // ---------------------------------------------------------------------------
  // 4. Type members
  //
  // A type parameter is not the only way to attach a type to a trait. A type
  // *member* is the same information moved from the trait's signature into its
  // body, where it can be left abstract and refined later.
  // ---------------------------------------------------------------------------

  trait Repo:
    type Id
    type Entity
    def find(id: Id): Option[Entity]

  /** A refinement fixes the members without naming a new trait. It is an
    * ordinary subtype of `Repo`, and the `find` it exposes is `(Long) =>
    * Option[Dog]` — the abstract members have been made concrete in the type
    * itself, not in an implementation.
    */
  type DogRepo = Repo { type Id = Long; type Entity = Dog }

  evidence.subtypeOf[DogRepo, Repo]

  val dogRepo: DogRepo = new Repo:
    type Id = Long
    type Entity = Dog
    def find(id: Long): Option[Dog] = Option.when(id == 1L)(rex)

  val found: Option[Dog] = dogRepo.find(1L)

  // Parameters vs members, the short version: a parameter is supplied by the
  // *caller* and appears in the type's name; a member is supplied by the
  // *implementation* and can stay abstract. Members are what make chapter 03's
  // path-dependent types possible, and what `Mirror` uses in chapter 10.

  // ---------------------------------------------------------------------------
  // 5. Evidence
  //
  // `=:=` and `<:<` turn a relation between types into a *value*, which is how
  // a method asks for a relation it cannot state in its own signature.
  // ---------------------------------------------------------------------------

  /** Only callable when the caller's `A` really is a `List`'s element type. */
  def sum[A](as: List[A])(using ev: A =:= Int): Int = as.map(ev.apply).sum

  val three: Int = sum(List(1, 2))

  //   sum(List("no"))   // no `String =:= Int` exists, so there is no call
  //
  // Both are ordinary classes with ordinary `given` instances — `=:=` extends
  // `<:<`, and `<:<` extends `A => B`, which is why `ev.apply` above converts.
  // Nothing about them is compiler magic except that the compiler knows how to
  // manufacture the instance.
end Walkthrough
