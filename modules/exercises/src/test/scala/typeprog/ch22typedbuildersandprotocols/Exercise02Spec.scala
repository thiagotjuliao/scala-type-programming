package typeprog.ch22typedbuildersandprotocols

import typeprog.core.TypeLevelSuite

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  test("AllIn holds when every element of the first tuple is in the second") {
    assertTypeChecks("summon[AllIn[(\"a\", \"b\"), (\"b\", \"c\", \"a\")] =:= true]")
    assertTypeChecks("summon[AllIn[EmptyTuple, EmptyTuple] =:= true]")
  }

  test("and fails when one is missing") {
    assertTypeChecks("summon[AllIn[(\"a\", \"b\"), (\"a\", \"c\")] =:= false]")
    assertTypeChecks("summon[AllIn[\"a\" *: EmptyTuple, EmptyTuple] =:= false]")
  }

  // A guard: it holds against the stub, whose build asks for nothing. It
  // catches evidence that rejects a complete builder.
  test("build compiles with every required field, in either order") {
    assertTypeChecks("Builder().set(\"name\")(\"Ada\").set(\"email\")(\"a@b\").build")
    assertTypeChecks(
      "Builder().set(\"email\")(\"a@b\").set(\"age\")(36).set(\"name\")(\"Ada\").build"
    )
  }

  test("and not without one, saying why") {
    assertTypeErrorStartsWith("Builder().set(\"name\")(\"Ada\").build", "cannot build a User")
    assertTypeErrorStartsWith("Builder().set(\"age\")(36).build", "cannot build a User")
    assertTypeErrorStartsWith("Builder().build", "cannot build a User")
  }

  test("build makes the user, with the age when it was set") {
    assertEquals(
      Builder().set("email")("ada@example.com").set("name")("Ada").build,
      User("Ada", "ada@example.com", None)
    )
    assertEquals(
      Builder().set("name")("Ada").set("age")(36).set("email")("ada@example.com").build,
      User("Ada", "ada@example.com", Some(36))
    )
  }
end Exercise02Spec
