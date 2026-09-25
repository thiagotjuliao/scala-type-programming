package typeprog.ch11evidencetypesafestatemachines

import scala.annotation.implicitNotFound
import scala.util.NotGiven

/** Chapter 11 — evidence & type-safe state machines.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The chapter covers five things, in order:
  *
  *   1. evidence as a precondition on a method;
  *   2. state that changes with each call — a builder;
  *   3. transitions as data — a state machine;
  *   4. messages that say what went wrong;
  *   5. `NotGiven` — evidence that something is absent.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch11-evidence-type-safe-state-machines.md`.
  *
  * (Messages quoted below drop the `typeprog.ch11evidencetypesafestatemachines.`
  * prefix the compiler prints in front of every name.)
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. Evidence as a precondition
  //
  // A method that asks for evidence about the class's type parameter exists
  // only where the evidence can be found. `<:<` evidence is also a function.
  // ---------------------------------------------------------------------------

  final class Pairs[A](items: List[A]):
    def toMap[K, V](using ev: A <:< (K, V)): Map[K, V] = items.map(ev).toMap
    def total(using n: Numeric[A]): A = items.foldLeft(n.zero)(n.plus)

  val asMap: Map[Int, String] = Pairs(List(1 -> "a", 2 -> "b")).toMap
  val totalled: Int = Pairs(List(1, 2, 3)).total // 6

  //   Pairs(List(1, 2)).toMap
  //   // Cannot prove that Int <:< (K, V).

  // ---------------------------------------------------------------------------
  // 2. State that changes with each call
  //
  // One phantom parameter per fact, as a `Boolean` literal type. Each setter
  // demands its fact was `false` and returns the builder with it `true`.
  // ---------------------------------------------------------------------------

  final case class Pizza(size: Int, base: String)

  final class PizzaBuilder[HasSize <: Boolean, HasBase <: Boolean] private (
      size: Int,
      base: String
  ):
    def size(s: Int)(using HasSize =:= false): PizzaBuilder[true, HasBase] =
      new PizzaBuilder(s, base)
    def base(b: String)(using HasBase =:= false): PizzaBuilder[HasSize, true] =
      new PizzaBuilder(size, b)
    def bake(using HasSize =:= true, HasBase =:= true): Pizza = Pizza(size, base)

  object PizzaBuilder:
    // Private constructor, one way in: nothing set.
    def apply(): PizzaBuilder[false, false] = new PizzaBuilder(0, "")

  val margherita: Pizza = PizzaBuilder().base("thin").size(30).bake

  // Correct, and unreadable:
  //
  //   PizzaBuilder().size(30).bake
  //   // Cannot prove that (false : Boolean) =:= (true : Boolean).
  //
  //   PizzaBuilder().size(30).size(40)
  //   // Cannot prove that (true : Boolean) =:= (false : Boolean).

  // ---------------------------------------------------------------------------
  // 3. Transitions as data
  //
  // A row of the table is a given that says what `Next` is. A missing row is
  // a missing instance.
  // ---------------------------------------------------------------------------

  sealed trait Green
  sealed trait Amber
  sealed trait Red

  sealed trait Timer
  sealed trait Walk

  @implicitNotFound("a light that is ${S} does not react to ${E}")
  trait Transition[S, E]:
    type Next

  object Transition:
    type Aux[S, E, N] = Transition[S, E] { type Next = N }

    private def row[S, E, N]: Aux[S, E, N] = new Transition[S, E]:
      type Next = N

    given Aux[Green, Timer, Amber] = row
    given Aux[Amber, Timer, Red] = row
    given Aux[Red, Timer, Green] = row
    given Aux[Green, Walk, Amber] = row

  final class Light[S] private ():
    def on[E](using t: Transition[S, E]): Light[t.Next] = new Light

  object Light:
    def green: Light[Green] = new Light

  // The caller names the events; the table computes the state.
  val red: Light[Red] = Light.green.on[Timer].on[Timer]

  // ---------------------------------------------------------------------------
  // 4. Messages that say what went wrong
  //
  // `@implicitNotFound` on the type class, with `${S}` and `${E}` filled in
  // with the types of the failed call. The light's state after two events is
  // `t.Next` — a path to the member of the instance that was found — and that
  // path is what `${S}` prints:
  //
  //   Light.green.on[Timer].on[Timer].on[Walk]
  //   // a light that is Walkthrough.Transition.
  //   //   given_Aux_Amber_Timer_Red.Next does not react to Walkthrough.Walk
  //
  // Correct, and it names the state by accident of how the given is named.
  // A state computed by a match type (chapter 07) is the state type itself;
  // exercise 04 is that door.
  // ---------------------------------------------------------------------------

  // For a builder, one small evidence type per fact, with an instance only for
  // the value that satisfies it:
  @implicitNotFound("the pizza has no size yet")
  sealed trait Sized[B <: Boolean]

  object Sized:
    given Sized[true] = new Sized[true] {}

  def ready[S <: Boolean](using Sized[S]): String = "ready"

  val isReady: String = ready[true]

  //   ready[false]
  //   // the pizza has no size yet

  // ---------------------------------------------------------------------------
  // 5. `NotGiven`
  //
  // An instance exactly when the search for the type fails.
  // ---------------------------------------------------------------------------

  def distinct[A, B](a: A, b: B)(using NotGiven[A =:= B]): (A, B) = (a, b)

  val mixed: (Int, String) = distinct(1, "a")

  //   distinct(1, 2)
  //   // No given instance of type scala.util.NotGiven[Int =:= Int] was found …

  // With chapter 09's `Tuple.Contains`: a registry that holds at most one
  // value of each type.
  final class Registry[T <: Tuple] private (val values: List[Any]):
    def add[A](a: A)(using NotGiven[Tuple.Contains[T, A] =:= true]): Registry[A *: T] =
      new Registry(a :: values)

  object Registry:
    def empty: Registry[EmptyTuple] = new Registry(Nil)

  val registry: Registry[(String, Int)] = Registry.empty.add(1).add("a")

  //   registry.add(2)
  //   // No given instance of type scala.util.NotGiven[Tuple.Contains[(String, Int), Int]
  //   // =:= (true : Boolean)] was found for parameter x$2 of method add in class Registry

  // A search that fails as *ambiguous* is a failure too, so `NotGiven` holds:
  trait Clock

  object TwoClocks:
    given a: Clock = new Clock {}
    given b: Clock = new Clock {}

    // Compiles — although two clocks are in scope.
    val noClock: NotGiven[Clock] = summon[NotGiven[Clock]]

    //   summon[Clock]
    //   // Ambiguous given instances: both given instance a in object TwoClocks and
    //   // given instance b in object TwoClocks match type Walkthrough.Clock …

end Walkthrough
