package typeprog.ch23typedquerybuilder

import typeprog.core.TypeLevelSuite

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  private val data = List(
    Map("id" -> 1, "name" -> "Ada", "email" -> "ada@example.com"),
    Map("id" -> 2, "name" -> "Alan", "email" -> "alan@example.com")
  )

  test("Value is the type of a column's values") {
    assertTypeChecks("summon[Users.Value[Users.Column[\"id\", Int]] =:= Int]")
    assertTypeChecks("summon[Users.Value[Users.Column[\"name\", String]] =:= String]")
  }

  test("Values is the row type of a tuple of columns, in its order") {
    assertTypeChecks(
      "summon[Users.Values[(Users.Column[\"id\", Int], Users.Column[\"name\", String])] =:= (Int, String)]"
    )
    assertTypeChecks(
      "summon[Users.Values[(Users.Column[\"name\", String], Users.Column[\"id\", Int])] =:= (String, Int)]"
    )
    assertTypeChecks("summon[Users.Values[EmptyTuple] =:= EmptyTuple]")
  }

  test("a select's rows have the type its columns give") {
    assertTypeChecks(
      "val rows: List[(Int, String)] = Users.select((Users.id, Users.name)).run(data)"
    )
    assertTypeChecks(
      "val rows: List[Tuple1[String]] = Users.select(Tuple1(Users.email)).run(data)"
    )
  }

  // A guard: it holds against the stub too. It catches a Values that loses
  // the order of the columns.
  test("and not another") {
    assertTypeError(
      "val rows: List[(String, Int)] = Users.select((Users.id, Users.name)).run(data)"
    )
  }

  // A guard: the runtime side is given. It catches a solution that breaks it.
  test("the rows hold the selected values, in the select's order") {
    assertEquals[Any, Any](
      Users.select((Users.name, Users.id)).run(data),
      List(("Ada", 1), ("Alan", 2))
    )
    assertEquals(Users.select((Users.id, Users.name)).sql, "select id, name from users")
  }
end Exercise01Spec
