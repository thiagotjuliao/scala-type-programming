package typeprog.ch04literaluniontypes

/** Exercise 04 — ask for two capabilities at once. *Solved.*
  *
  * `Logger & Clock` is the type of values that are both, and that is all
  * `stamp` needs to say. Anything that happens to be both fits — an `object`
  * extending the two traits, an anonymous class, the result of `combine` —
  * and nothing had to be declared in advance as "a logger with a clock". The
  * near miss is exactly that declaration: `trait LoggerWithClock extends
  * Logger, Clock` works for values created after it, and rejects every value
  * that is both but was declared without it, `System` in the spec among them.
  *
  * `combine` builds the intersection as an anonymous class with both parents,
  * each member delegating to the value that provides it. Its result type is
  * the intersection and nothing more specific: the anonymous class has no name
  * a caller could use anyway, and `Logger & Clock` is what it is for.
  *
  * Intersections are commutative — `Logger & Clock` and `Clock & Logger` are
  * the same type — which is the difference from Scala 2's `Logger with Clock`
  * that this replaces.
  */
object Exercise04:

  trait Logger:
    def log(line: String): Unit

  trait Clock:
    def now(): Long

  def stamp(env: Logger & Clock)(message: String): Unit =
    env.log(s"[${env.now()}] $message")

  def combine(logger: Logger, clock: Clock): Logger & Clock = new Logger with Clock:
    def log(line: String): Unit = logger.log(line)
    def now(): Long = clock.now()
