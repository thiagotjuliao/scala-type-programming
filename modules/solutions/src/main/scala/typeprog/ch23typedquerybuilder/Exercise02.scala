package typeprog.ch23typedquerybuilder

/** Exercise 02 — take only the table's own columns. *Solved.*
  *
  * `where` needs no evidence. `Column` is declared inside `Table`, so inside
  * the table `Column[N, A]` means `Table.this.Column[N, A]`: for `Users` it is
  * `Users.Column`, and `Orders.id`, a `Orders.Column["id", Int]`, is a
  * different type (chapter 03) — a type mismatch at the argument. `A` is fixed
  * by the column, since `Column` is invariant in it, before the second
  * parameter list is looked at; so `value: A` is an `Int` for `Users.id`, and
  * `"one"` is rejected.
  *
  * `select` takes a whole tuple, and there is no parameter type that says
  * "a tuple of my columns". So it asks for the fact instead, as evidence
  * (chapter 11): `Tuple.Union[Cs] <:< Column[?, ?]`. `Tuple.Union` turns the
  * element types into one union type; it conforms to this table's
  * `Column[?, ?]` only if every member does. For an empty select it is
  * `Nothing`, which conforms to everything — and a select of no columns is
  * fine.
  *
  * The near miss is to leave `select` alone, because `Values` already has no
  * case for another table's column. It does not reject the call. `Values` of
  * `Tuple1[Orders.Column["id", Int]]` is stuck — the compiler cannot prove
  * `Orders.Column` disjoint from `Users.Column`, so it cannot move past the
  * case — and a stuck type in the *result* is just a type: `select` compiles,
  * and the error turns up only where the rows are used, as a mismatch against
  * an unreduced `Users.Values[...]`. A rejection belongs at the call, which
  * is what the evidence puts there.
  */
object Exercise02:

  abstract class Table(val tableName: String):

    final class Column[N <: String & Singleton, A] private[Table] (val name: N)

    protected def column[N <: String & Singleton, A](name: N): Column[N, A] = Column(name)

    type Value[C] = C match
      case Column[n, a] => a

    type Values[Cs <: Tuple] = Tuple.Map[Cs, Value]

    final class Query[Cs <: Tuple] private[Table] (
        val columns: Cs,
        filters: List[(String, Any)]
    ):

      def sql: String =
        val conditions =
          if filters.isEmpty then ""
          else filters.map((name, _) => s"$name = ?").mkString(" where ", " and ", "")
        s"select ${names(columns).mkString(", ")} from $tableName$conditions"

      def run(rows: List[Map[String, Any]]): List[Values[Cs]] =
        rows
          .filter(row => filters.forall((name, value) => row(name) == value))
          .map(row => Tuple.fromArray(names(columns).map(row).toArray).asInstanceOf[Values[Cs]])

      def where[N <: String & Singleton, A](column: Column[N, A])(value: A): Query[Cs] =
        Query(columns, filters :+ (column.name -> value))
    end Query

    def select[Cs <: Tuple](columns: Cs)(using Tuple.Union[Cs] <:< Column[?, ?]): Query[Cs] =
      Query(columns, Nil)

    private def names(columns: Tuple): List[String] =
      columns.toList.map(_.asInstanceOf[Column[?, ?]].name)
  end Table

  object Users extends Table("users"):
    val id: Column["id", Int] = column("id")
    val name: Column["name", String] = column("name")
    val email: Column["email", String] = column("email")

  object Orders extends Table("orders"):
    val id: Column["id", Int] = column("id")
    val total: Column["total", BigDecimal] = column("total")
end Exercise02
