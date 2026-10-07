package typeprog.ch23typedquerybuilder

import scala.compiletime.ops.int.S

/** Exercise 03 — read a row by its column. *Solved.*
  *
  * `IndexOf` is chapter 09's walk, counting: `0` when the head is `X`, one
  * more than the position in the tail otherwise. `S` is the successor from
  * `compiletime.ops.int`, and on a literal it reduces — `S[S[0]]` is `2` — so
  * the result is a literal type. There is no case for `EmptyTuple`: a type the
  * tuple does not hold leaves `IndexOf` stuck, which is the point.
  *
  * `apply` asks for `ValueOf[IndexOf[Cs, Column[N, A]]]`. `ValueOf` exists for
  * a literal type (chapter 04), and its `value` is the literal — the
  * position, computed by the compiler, handed to the runtime to index with.
  * For a column that was not selected `IndexOf` is stuck, a stuck type is no
  * literal, and there is no `ValueOf`: the call is rejected, with *"No
  * singleton value available for IndexOf[...]"*. Another table's column is
  * rejected before that, at the parameter: `Column[N, A]` is this table's.
  *
  * The result's type is the column's own `A`, read off the argument. The row
  * type could say it too — `Tuple.Elem[Values[Cs], IndexOf[Cs, C]]` — but a
  * signature that takes any `C` and computes its result from it makes the
  * compiler reduce `IndexOf` before `C` is inferred: it tries `IndexOf[Cs,
  * Nothing]`, and a perfectly good call comes with a warning that it could
  * not. Taking a `Column[N, A]` fixes `N` and `A` from the argument first.
  *
  * `N` is what makes the position the right one. `name` and `email` are both
  * `Column[?, String]`; with only the value type in a column's type they would
  * be the same type, `IndexOf` would find the first of them, and
  * `row(Users.email)` would compile and return the name. The cast is safe
  * because the position is that of this very column, whose values are `A`s.
  */
object Exercise03:

  type IndexOf[T <: Tuple, X] <: Int = T match
    case X *: _ => 0
    case _ *: rest => S[IndexOf[rest, X]]

  abstract class Table(val tableName: String):

    final class Column[N <: String & Singleton, A] private[Table] (val name: N)

    protected def column[N <: String & Singleton, A](name: N): Column[N, A] = Column(name)

    type Value[C] = C match
      case Column[n, a] => a

    type Values[Cs <: Tuple] = Tuple.Map[Cs, Value]

    final class Row[Cs <: Tuple] private[Table] (val values: Values[Cs]):

      def apply[N <: String & Singleton, A](column: Column[N, A])(using
          position: ValueOf[IndexOf[Cs, Column[N, A]]]
      ): A =
        values.productElement(position.value).asInstanceOf[A]

    final class Query[Cs <: Tuple] private[Table] (val columns: Cs):

      def run(rows: List[Map[String, Any]]): List[Row[Cs]] =
        rows.map(row =>
          Row(Tuple.fromArray(names(columns).map(row).toArray).asInstanceOf[Values[Cs]])
        )

    def select[Cs <: Tuple](columns: Cs)(using Tuple.Union[Cs] <:< Column[?, ?]): Query[Cs] =
      Query(columns)

    private def names(columns: Tuple): List[String] =
      columns.toList.map(_.asInstanceOf[Column[?, ?]].name)
  end Table

  object Users extends Table("users"):
    val id: Column["id", Int] = column("id")
    val name: Column["name", String] = column("name")
    val email: Column["email", String] = column("email")

  object Orders extends Table("orders"):
    val id: Column["id", Int] = column("id")
end Exercise03
