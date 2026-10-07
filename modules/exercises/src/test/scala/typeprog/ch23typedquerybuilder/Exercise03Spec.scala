package typeprog.ch23typedquerybuilder

import typeprog.core.TypeLevelSuite

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  private val data = List(Map("id" -> 1, "name" -> "Ada", "email" -> "ada@example.com"))

  private val row = Users.select((Users.id, Users.name)).run(data).head
  private val full = Users.select((Users.id, Users.name, Users.email)).run(data).head

  test("IndexOf is the position of a type in a tuple") {
    assertTypeChecks("summon[IndexOf[(\"a\", \"b\", \"c\"), \"a\"] =:= 0]")
    assertTypeChecks("summon[IndexOf[(\"a\", \"b\", \"c\"), \"c\"] =:= 2]")
  }

  // A guard: there is no ValueOf for the stub's Int either. It catches an
  // IndexOf that answers for a type the tuple does not hold.
  test("and has no value for a type the tuple does not hold") {
    assertTypeError("summon[ValueOf[IndexOf[(\"a\", \"b\"), \"c\"]]]")
  }

  test("a row read by a selected column has that column's type") {
    assertTypeChecks("val id: Int = row(Users.id)")
    assertTypeChecks("val name: String = row(Users.name)")
  }

  test("a column that was not selected does not compile") {
    assertTypeError("row(Users.email)")
  }

  // A guard: the stub's apply already takes only Users columns.
  test("nor does another table's") {
    assertTypeError("row(Orders.id)")
  }

  // `name` and `email` are both String columns; only the name in the
  // column's type tells their positions apart.
  test("each column reads its own value") {
    assertEquals[Any, Any](full(Users.id), 1)
    assertEquals[Any, Any](full(Users.name), "Ada")
    assertEquals[Any, Any](full(Users.email), "ada@example.com")
  }
end Exercise03Spec
