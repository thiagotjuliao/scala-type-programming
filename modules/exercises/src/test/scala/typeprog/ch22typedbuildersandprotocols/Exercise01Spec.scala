package typeprog.ch22typedbuildersandprotocols

import typeprog.core.TypeLevelSuite

import Exercise01.*

class Exercise01Spec extends TypeLevelSuite:

  test("Lookup finds the type of a key's value, wherever the key is") {
    assertTypeChecks("summon[Lookup[Schema, \"name\"] =:= String]")
    assertTypeChecks("summon[Lookup[Schema, \"age\"] =:= Int]")
    assertTypeChecks("summon[Lookup[((1, Boolean), (2, Char)), 2] =:= Char]")
  }

  test("Contains says whether a type is among a tuple's elements") {
    assertTypeChecks("summon[Contains[(\"a\", \"b\"), \"b\"] =:= true]")
    assertTypeChecks("summon[Contains[(\"a\", \"b\"), \"c\"] =:= false]")
    assertTypeChecks("summon[Contains[EmptyTuple, \"a\"] =:= false]")
  }

  test("set takes the type the schema gives the key") {
    assertTypeError("Builder().set(\"age\")(\"old\")")
    assertTypeError("Builder().set(\"name\")(42)")
  }

  test("and no key the schema lacks") {
    assertTypeError("Builder().set(\"nmae\")(\"Ada\")")
  }

  test("each key is set once") {
    assertTypeError("Builder().set(\"name\")(\"Ada\").set(\"name\")(\"Bob\")")
    assertTypeError("Builder().set(\"age\")(1).set(\"name\")(\"Ada\").set(\"age\")(2)")
  }

  // Guards: they hold against the stub too. They catch a set that rejects
  // more than it should, or forgets which keys it has seen.
  test("keys can be set in any order, and the builder's type lists them") {
    assertTypeChecks("Builder().set(\"email\")(\"a@b\").set(\"name\")(\"Ada\").set(\"age\")(36)")
    assertTypeChecks(
      "val b: Builder[(\"email\", \"name\")] = Builder().set(\"name\")(\"Ada\").set(\"email\")(\"a@b\")"
    )
  }

  // A guard: the runtime side is given. It catches a solution that breaks it.
  test("the values are collected") {
    assertEquals(
      Builder().set("name")("Ada").set("age")(36).values,
      Map("name" -> "Ada", "age" -> 36)
    )
    assertEquals(Builder().values, Map.empty[String, Any])
  }
end Exercise01Spec
