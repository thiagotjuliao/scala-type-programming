package typeprog.ch23typedquerybuilder

/** Exercise 01 — compute the type of a select's rows from its columns.
  * *Solved.*
  *
  * `Value` is one case of a match type whose pattern binds the column's type
  * arguments: `Column[n, a]` matches any of this table's columns, and the
  * result is the `a` it bound. There is no second case. Nothing else is a
  * column, and a `Value` of something else is meant to stay unreduced — it is
  * an error wherever it is used.
  *
  * `Values` is `Value` applied to every element, which is what `Tuple.Map`
  * does (chapter 09): `Tuple.Map[(C1, C2), Value]` is `(Value[C1],
  * Value[C2])`, and the compiler reduces each element. The recursion is the
  * standard library's — `case h *: t => Value[h] *: Values[t]`, with
  * `EmptyTuple` for the end, is the same thing written out.
  *
  * Nothing at the call names the row type. `select((Users.id, Users.name))`
  * infers `Cs` from its argument, and `run` returns `List[Values[Cs]]`, which
  * the compiler reduces to `List[(Int, String)]` while type-checking the call.
  * That is the whole point: the result type is computed from the select, so
  * it cannot drift from it.
  *
  * `Column` in the pattern is this table's: inside `Users`, `Value` matches
  * `Users.Column[n, a]`. Another table's column is a different class (chapter
  * 03), and does not match — that is exercise 02.
  *
  * The cast in `run` is what is left of the border with the untyped rows, and
  * it is safe for the reason above: each value is read by its column's name,
  * in the order of `Cs`.
  */
object Exercise01:

  abstract class Table(val tableName: String):

    final class Column[N <: String & Singleton, A] private[Table] (val name: N)

    protected def column[N <: String & Singleton, A](name: N): Column[N, A] = Column(name)

    type Value[C] = C match
      case Column[n, a] => a

    type Values[Cs <: Tuple] = Tuple.Map[Cs, Value]

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
