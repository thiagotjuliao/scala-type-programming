package typeprog.ch07matchtypes

/** Exercise 04 — units that cancel. *Solved.*
  *
  * A match type can take a pair as its scrutinee, `(U, V) match`, and match on
  * both units at once. The first case catches a ratio on the left and binds
  * its numerator `n` and denominator `d`. Whether `V` *is* `d` is then a second
  * question, asked by a match inside the result: `V match { case d => n }`. In
  * the inner match, `d` is not a new binder — it is the type the outer pattern
  * bound, so the case reads *if `V` conforms to `d`*. The second case is the
  * same with the ratio on the right.
  *
  * Why not `case (n / d, d) => n`? A pattern cannot bind the same name twice:
  * *"duplicate pattern variable: d"*. The nested match is how two types are
  * compared.
  *
  * Every "otherwise" relies on disjointness. For `speed * distance`, the inner
  * match asks whether `Meters` matches `Seconds`; it does not, and — both being
  * sealed traits with no subtypes — the compiler can prove no type is both, so
  * it moves on to `U * V`. Were the units plain traits, it could prove nothing
  * (a class may extend both), and `Times[Meters / Seconds, Meters]` would stay
  * unreduced instead of becoming a product.
  *
  * The body is `q * other`: the amounts multiply whatever the units are. The
  * whole of the unit arithmetic happens in the signature, and at runtime a
  * quantity is still a bare `Double`.
  */
object Exercise04:

  sealed trait Meters
  sealed trait Seconds
  sealed trait Kilograms

  /** The unit of a ratio. Never instantiated: written infix, `Meters / Seconds`. */
  sealed trait /[A, B]

  /** The unit of a product. Never instantiated: written infix, `Meters * Seconds`. */
  sealed trait *[A, B]

  opaque type Quantity[U] = Double

  object Quantity:
    def of[U](amount: Double): Quantity[U] = amount

  extension [U](q: Quantity[U])
    def amount: Double = q
    def +(other: Quantity[U]): Quantity[U] = q + other
    def per[V](other: Quantity[V]): Quantity[U / V] = q / other
    def *[V](other: Quantity[V]): Quantity[Times[U, V]] = q * other

  type Times[U, V] = (U, V) match
    case (n / d, _) =>
      V match
        case d => n
        case _ => U * V
    case (_, n / d) =>
      U match
        case d => n
        case _ => U * V
    case _ => U * V
end Exercise04
