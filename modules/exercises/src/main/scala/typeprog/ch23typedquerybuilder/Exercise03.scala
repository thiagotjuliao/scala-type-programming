package typeprog.ch23typedquerybuilder

/** Exercise 03 — read a row by its column.
  *
  * `run` now returns `Row`s. A row holds the selected values as a tuple, in
  * the select's order, and is read by column: `row(Users.name)`.
  *
  *   - `IndexOf[T, X]` is the position of the type `X` in the tuple `T`,
  *     counting from `0`, as a literal type: `IndexOf[("a", "b", "c"), "c"]`
  *     is `2`. For an `X` that `T` does not hold it has no value at all.
  *   - `row(column)` compiles only for a column the select took, and returns
  *     a value of that column's type: `row(Users.id)` is an `Int`. For a
  *     column that was not selected, or another table's, it does not compile.
  *   - It returns that column's value — which takes the position of the
  *     column itself, not of the first column of the same type: `name` and
  *     `email` are both `String` columns.
  *
  * Exercises 01 and 02 are given, solved. As shipped, `IndexOf` is `Int`, and
  * `apply` takes any of this table's columns and returns `Any`.
  *
  * Hint: chapter 09 for `IndexOf`, and `compiletime.ops.int` for counting.
  * Chapter 04 for the given that turns a literal type into its value — which
  * exists only when there is a literal to turn.
  */
object Exercise03:

  /** TODO: the position of `X` in `T`. */
  type IndexOf[T <: Tuple, X] = Int

  abstract class Table(val tableName: String):

    final class Column[N <: String & Singleton, A] private[Table] (val name: N)

    protected def column[N <: String & Singleton, A](name: N): Column[N, A] = Column(name)

    type Value[C] = C match
      case Column[n, a] => a

    type Values[Cs <: Tuple] = Tuple.Map[Cs, Value]

    final class Row[Cs <: Tuple] private[Table] (val values: Values[Cs]):

      /** TODO: only a selected column, read at its own position. */
      def apply(column: Column[?, ?]): Any = ???

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
