package typeprog.ch23typedquerybuilder

import typeprog.core.TypeLevelSuite

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  private val data = List(
    Map("id" -> 1, "name" -> "Ada", "email" -> "ada@example.com"),
    Map("id" -> 2, "name" -> "Alan", "email" -> "alan@example.com")
  )

  // A guard: it holds against the stub, whose select takes anything. It
  // catches evidence that rejects a table's own columns.
  test("select takes the table's own columns") {
    assertTypeChecks("Users.select((Users.id, Users.name))")
    assertTypeChecks("Users.select(Tuple1(Users.email))")
  }

  test("and no other table's, even one with the same name and type") {
    assertTypeError("Users.select(Tuple1(Orders.id))")
    assertTypeError("Users.select((Users.name, Orders.id))")
  }

  test("where takes one of the table's columns and a value of its type") {
    assertTypeChecks("Users.select(Tuple1(Users.name)).where(Users.id)(1)")
    assertTypeChecks("Users.select(Tuple1(Users.id)).where(Users.name)(\"Ada\")")
    assertTypeError("Users.select(Tuple1(Users.name)).where(Users.id)(\"one\")")
    assertTypeError("Users.select(Tuple1(Users.name)).where(Users.name)(1)")
  }

  test("and no other table's column") {
    assertTypeError("Users.select(Tuple1(Users.name)).where(Orders.id)(1)")
  }

  // A guard: the runtime side is given. It catches a solution that breaks it.
  test("where filters the rows, and shows in the SQL") {
    val query = Users.select(Tuple1(Users.name)).where(Users.id)(2)
    assertEquals[Any, Any](query.run(data), List(Tuple1("Alan")))
    assertEquals(query.sql, "select name from users where id = ?")
  }
end Exercise02Spec
