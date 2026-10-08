package typeprog.ch23typedquerybuilder

/** Exercise 02 — take only the table's own columns.
  *
  * `Users` and `Orders` both have an `"id"` column of `Int`s. A query on
  * `Users` must not be handed an `Orders` column, however alike the two look.
  *
  *   - `select` compiles only when every column in the tuple is one of this
  *     table's: `Users.select((Users.name, Orders.id))` does not.
  *   - `where(column)(value)` adds a condition, `column = value`. It takes only
  *     one of this table's columns, and only a value of that column's type:
  *     `where(Users.id)(1)` compiles, `where(Users.id)("one")` and
  *     `where(Orders.id)(1)` do not.
  *
  * Exercise 01's `Values` is given, solved. The runtime side — the SQL and the
  * filtering — is already here and is not the exercise. As shipped, `select`
  * takes any tuple and `where` takes anything.
  *
  * Hint: chapter 03 — where `Column` is declared already decides whose it is,
  * so a parameter of type `Column[N, A]` says the rest. `select` takes a whole
  * tuple, which no parameter type can describe that way; chapter 11 for
  * demanding a fact about its type instead, and chapter 09 for the
  * `scala.Tuple` operation that turns a tuple's element types into one type.
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

      def where[N <: String & Singleton, A](column: Column[N, A])(
          value: A
      ): Query[Cs] =
        Query(columns, filters :+ (column.asInstanceOf[Column[?, ?]].name -> value))
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
