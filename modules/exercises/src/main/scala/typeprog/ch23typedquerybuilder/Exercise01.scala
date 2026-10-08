package typeprog.ch23typedquerybuilder

/** Exercise 01 — compute the type of a select's rows from its columns.
  *
  * A `Table` declares its columns. A `Column[N, A]` has the column's name as
  * the literal type `N` and the type of its values as `A`: `Users.id` is a
  * `Users.Column["id", Int]`. `select` takes a tuple of columns and keeps it
  * in its type — `Users.select((Users.id, Users.name))` is a
  * `Query[(Users.Column["id", Int], Users.Column["name", String])]` — and
  * `run` returns one row per record, as a `Values` of those columns.
  *
  *   - `Value[C]` is the type of the values of the column type `C`: `Int` for
  *     a `Column["id", Int]`.
  *   - `Values[Cs]` is the tuple of the value types of the columns in `Cs`,
  *     in the same order: `(Int, String)` for the select above, and
  *     `EmptyTuple` for no columns. `run` then returns a `List[(Int,
  *     String)]`, with no type written at the call.
  *
  * The runtime side is already here and is not the exercise: `run` reads each
  * selected column's value out of a `Map` by its name.
  *
  * `Value` is `Any` and `Values` is `Tuple` in the stub only so that it
  * compiles: they are the wrong answers that let every row through untyped.
  *
  * Hint: chapter 07 for a match type whose pattern binds a type argument,
  * `case Column[n, a] =>`. Chapter 09 for doing that to every element of a
  * tuple without writing the recursion: the standard library has it.
  */
object Exercise01:

  abstract class Table(val tableName: String):

    final class Column[N <: String & Singleton, A] private[Table] (val name: N)

    protected def column[N <: String & Singleton, A](name: N): Column[N, A] = Column(name)

    type Value[C] = C match
      case Column[?, a] => a

    type Values[Cs <: Tuple] = Cs match
      case EmptyTuple => EmptyTuple
      case h *: t => Value[h] *: Values[t]

    final class Query[Cs <: Tuple] private[Table] (val columns: Cs):

      def sql: String = s"select ${names(columns).mkString(", ")} from $tableName"

      def run(rows: List[Map[String, Any]]): List[Values[Cs]] =
        rows.map(row => Tuple.fromArray(names(columns).map(row).toArray).asInstanceOf[Values[Cs]])

    def select[Cs <: Tuple](columns: Cs): Query[Cs] = Query(columns)

    private def names(columns: Tuple): List[String] =
      columns.toList.map(_.asInstanceOf[Column[?, ?]].name)
  end Table

  object Users extends Table("users"):
    val id: Column["id", Int] = column("id")
    val name: Column["name", String] = column("name")
    val email: Column["email", String] = column("email")
end Exercise01
