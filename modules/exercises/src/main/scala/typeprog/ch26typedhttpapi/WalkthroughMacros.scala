package typeprog.ch26typedhttpapi

import scala.quoted.*

/** The macros `Walkthrough.scala` calls. They live in their own file because a
  * macro cannot be called from the file that defines it.
  */
object WalkthroughMacros:

  /** The arguments as a tuple, typed element by element. */
  transparent inline def pack(inline args: Any*): Tuple = ${ packImpl('args) }

  /** The same expansion, without `transparent`: the call is a `Tuple`. */
  inline def packLoose(inline args: Any*): Tuple = ${ packImpl('args) }

  def packImpl(args: Expr[Seq[Any]])(using Quotes): Expr[Tuple] =
    args match
      case Varargs(arguments) =>
        // Built from the right, one `*:` per argument. Each step binds the
        // argument's type `a` and the tuple's `t`, so `'{ $x *: $rest }` is a
        // `a *: t` — the precise type the call takes when it is transparent.
        arguments.foldRight[Expr[Tuple]]('{ EmptyTuple }): (argument, built) =>
          argument match
            case '{ $x: a } =>
              built match
                case '{ type t <: Tuple; $rest: `t` } => '{ $x *: $rest }
      case _ => quotes.reflect.report.errorAndAbort("pack needs its arguments written out", args)
end WalkthroughMacros
