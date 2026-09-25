package typeprog.ch04literaluniontypes

/** Exercise 04 — ask for two capabilities at once.
  *
  * `stamp` writes a message with the current time in front — `"[42] up"` at
  * time `42` — so it needs something to log with and something to tell the
  * time. As shipped it asks for a `Logger` only, and has no clock to call.
  *
  *   - `stamp` should accept anything that is both a `Logger` and a `Clock`,
  *     and nothing that is only one of them — without a new trait that extends
  *     the two, which values declared before it would not extend;
  *   - `combine(l, c)` should make one value that is both, logging through `l`
  *     and telling the time through `c`.
  *
  * Hint: `&` makes one type out of two. A value of such a type can be written
  * inline, as an anonymous class with both parents.
  */
object Exercise04:

  trait Logger:
    def log(line: String): Unit

  trait Clock:
    def now(): Long

  /** TODO: the type of `env`, and the body. */
  def stamp(env: Logger & Clock)(message: String): Unit =
    env.log(s"[${env.now()}] $message")

  /** TODO: the result type, and the body. */
  def combine(logger: Logger, clock: Clock): Logger & Clock = new Logger with Clock:
    override def log(line: String): Unit = logger.log(line)
    override def now(): Long = clock.now()
