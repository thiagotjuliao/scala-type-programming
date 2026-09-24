package typeprog.ch03pathdependenttypes

import typeprog.core.evidence

/** Chapter 03 — path-dependent & dependent function types.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The chapter covers five things, in order:
  *
  *   1. types selected through a value — `roads.Stop` is not `rivers.Stop`;
  *   2. stable paths — which expressions may stand in front of the dot;
  *   3. dependent method types — a signature that selects through a parameter;
  *   4. dependent function types — the same dependency, kept in a value;
  *   5. type avoidance — what happens to a path that outlives its scope.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch03-path-dependent-types.md`.
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. A type selected through a value
  //
  // A class declared inside a class is a member of each *instance*. Every
  // `Network` below has its own `Stop`, and `connect` — which inside the class
  // reads as `this.Stop` — only takes stops of the network it is called on.
  // ---------------------------------------------------------------------------

  final class Network(val name: String):
    final class Stop(val label: String)

    private var lines = List.empty[(Stop, Stop)]

    def stop(label: String): Stop = Stop(label)
    def connect(from: Stop, to: Stop): Unit = lines = (from, to) :: lines
    def size: Int = lines.size

  val roads: Network = Network("roads")
  val rivers: Network = Network("rivers")

  val lisbon: roads.Stop = roads.stop("Lisbon")
  val porto: roads.Stop = roads.stop("Porto")
  val tagus: rivers.Stop = rivers.stop("Tagus")

  roads.connect(lisbon, porto)

  // Same class, same constructor, two different types — the path is part of
  // the type, and two paths are only equal when they are the same path:
  //
  //   roads.connect(lisbon, tagus)
  //   // Found:    (Walkthrough.tagus : Walkthrough.rivers.Stop)
  //   // Required: Walkthrough.roads.Stop
  //
  // (Messages in this file drop the `typeprog.ch03pathdependenttypes.` prefix
  // the compiler prints in front of every name.)

  // Every network's `Stop` conforms to one shared supertype, the projection
  // `Network#Stop` — "a stop of *some* network". It is the type to collect
  // stops under. It is not a type any `connect` accepts.
  val everywhere: List[Network#Stop] = List(lisbon, tagus)

  evidence.subtypeOf[roads.Stop, Network#Stop]
  evidence.subtypeOf[rivers.Stop, Network#Stop]

  // ---------------------------------------------------------------------------
  // 2. Only a stable path can carry a type
  //
  // `roads` works in front of `.Stop` because it is a `val`: it names the same
  // network every time. So does an `object`, a method parameter, or `this`.
  // A `var` can be reassigned under the type, and a `def` can return a new
  // network on every call, so neither is a path:
  //
  //   var current: Network = roads
  //   val s: current.Stop = ...
  //   // (current : Walkthrough.Network) is not a valid type prefix, since it is not an immutable path
  // ---------------------------------------------------------------------------

  def freshNetwork(): Network = Network("fresh")

  // Calling through a `def` is fine; the result simply has no path. The
  // compiler widens its type to the projection, which no network's `connect`
  // will accept:
  val orphan: Network#Stop = freshNetwork().stop("nowhere")

  //   roads.connect(lisbon, orphan)
  //   // Found:    (Walkthrough.orphan : Walkthrough.Network#Stop)
  //   // Required: Walkthrough.roads.Stop

  // Binding the result to a `val` is what gives it a path back:
  val bound: Network = freshNetwork()
  bound.connect(bound.stop("a"), bound.stop("b"))

  // ---------------------------------------------------------------------------
  // 3. Dependent method types
  //
  // A parameter is a stable path, so the rest of the signature can select
  // through it. The compiler substitutes the argument at each call.
  // ---------------------------------------------------------------------------

  /** Takes two stops of whichever network it is given, and no others. */
  def roundTrip(n: Network)(a: n.Stop, b: n.Stop): Unit =
    n.connect(a, b)
    n.connect(b, a)

  roundTrip(roads)(lisbon, porto)

  //   roundTrip(roads)(lisbon, tagus)
  //   // Found:    (Walkthrough.tagus : Walkthrough.rivers.Stop)
  //   // Required: Walkthrough.roads.Stop

  // The result type can depend on a parameter too, and with an abstract type
  // member that is how a result type gets decided by an *argument*. `Key`
  // (next to this file) has `type Value`, and `Key.Aux[V]` is the refinement
  // that fixes it — chapter 01's `Repo { type Id = Long }`, named.
  val port: Key.Aux[Int] = Key[Int]("port")(_.toIntOption)
  val host: Key.Aux[String] = Key[String]("host")(Some(_))

  /** The result type is read off the key: `Option[Int]` for `port`. */
  def valueOf(k: Key)(raw: String): Option[k.Value] = k.parse(raw)

  val portValue: Option[Int] = valueOf(port)("8080")
  val hostValue: Option[String] = valueOf(host)("localhost")

  evidence.sameType[port.Value, Int]

  // The refinement lives in the key's *declared* type. Ascribe the same key as
  // a plain `Key` and its `Value` is abstract again — still a type, still
  // unique to that key, but no longer known to be `Int`:
  val forgetful: Key = Key[Int]("port")(_.toIntOption)
  val forgotten: Option[forgetful.Value] = valueOf(forgetful)("8080")

  //   val n: Option[Int] = valueOf(forgetful)("8080")
  //   // Found:    Option[Walkthrough.forgetful.Value]
  //   // Required: Option[Int]

  // An abstract `k.Value` is still good for something: whatever is read with
  // `k` can be handed back to anything that wants a `k.Value`, without anybody
  // ever learning what the type is.
  def reparse(k: Key)(raw: String)(check: k.Value => Boolean): Boolean =
    valueOf(k)(raw).exists(check)

  // ---------------------------------------------------------------------------
  // 4. Dependent function types
  //
  // Before Scala 3 a method could be dependent and a function value could not:
  // turning `valueOf` into a value gave `Key => String => Option[Any]`. Scala 3
  // names the parameter in the function type, so the result can mention it.
  // ---------------------------------------------------------------------------

  val reader: (k: Key) => String => Option[k.Value] = valueOf

  val portAgain: Option[Int] = reader(port)("8080")

  // A lambda whose body selects through its parameter is inferred as dependent,
  // with no annotation at all:
  val inferred = (k: Key) => k.parse("8080")
  val portOnceMore: Option[Int] = inferred(port)

  // The conformance runs one way only. A dependent function is also a plain
  // one, since `Option[k.Value]` is an `Option[Any]`. The reverse promises
  // nothing about which `Option` comes back, and is rejected.
  evidence.subtypeOf[(k: Key) => Option[k.Value], Key => Option[Any]]

  //   val lossy: Key => String => Option[Any] = valueOf
  //   val back: (k: Key) => String => Option[k.Value] = lossy
  //   // Found:    (lossy : Key => String => Option[Any])
  //   // Required: (k: Key) => String => Option[k.Value]

  // Because it is a type, it can be a parameter. This one refuses any reader
  // that answers some key at the wrong type — `(k: Key) => port.parse` among
  // them, since `port.Value` is not `k.Value` for every `k`.
  def readBoth(read: (k: Key) => String => Option[k.Value]): (Option[Int], Option[String]) =
    (read(port)("8080"), read(host)("localhost"))

  val both: (Option[Int], Option[String]) = readBoth(valueOf)

  // ---------------------------------------------------------------------------
  // 5. A path that escapes its scope
  //
  // A type that names a local value cannot outlive the value. The compiler
  // widens it until it no longer mentions the value — *type avoidance* — and
  // the code compiles with less information than it had a line earlier.
  // ---------------------------------------------------------------------------

  // `local.Stop` means nothing after the block, so this is a `Network#Stop`:
  val escaped: Network#Stop =
    val local = Network("local")
    local.stop("x")

  // An abstract member widens to its bound. `k.Value` has none, so `Any`:
  val vague: Option[Any] =
    val k: Key = Key[Int]("port")(_.toIntOption)
    k.parse("8080")

  // Scala 3 treats wildcards the same way. A `Cell[?]` in hand is a value whose
  // element type is unknown, and the compiler models that unknown as a type
  // selected through the value — `c.T`. What was read out of `c` can go back
  // into `c`, because it is the same path:
  final class Cell[T](init: T):
    private var current = init
    def get: T = current
    def set(t: T): Unit = current = t

  def refill(c: Cell[?]): Unit = c.set(c.get)

  // And two wildcards are two paths, exactly like two networks:
  //
  //   def mix(a: Cell[?], b: Cell[?]): Unit = a.set(b.get)
  //   // Found:    b.T
  //   // Required: a.T
  //
  // A type parameter and a type member are two spellings of one idea; the
  // member is the one a value can carry.
end Walkthrough
