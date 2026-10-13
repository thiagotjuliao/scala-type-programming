package typeprog.ch26typedhttpapi

import typeprog.core.TypeLevelSuite

import Exercise02.*

object Exercise02Fixtures:
  val userName = Endpoint.get(Path.root / "users" / capture[Int]("id") / "name").returning[String]

  val postTitle = Endpoint
    .get(Path.root / "users" / capture[Int]("id") / "posts" / capture[String]("slug"))
    .returning[String]

  val health = Endpoint.get(Path.root / "health").returning[Boolean]

  val names = Map(1 -> "Ada", 2 -> "Alan")

import Exercise02Fixtures.*

class Exercise02Spec extends TypeLevelSuite:

  test("a handler receives no capture as Unit, one as its value, more as a tuple") {
    assertTypeChecks("summon[Args[EmptyTuple] =:= Unit]")
    assertTypeChecks("summon[Args[Int *: EmptyTuple] =:= Int]")
    assertTypeChecks("summon[Args[(Int, String)] =:= (Int, String)]")
  }

  test("a router serves each endpoint with its handler") {
    val router = Router(
      userName.handle((id: Int) => names.getOrElse(id, "nobody")),
      postTitle.handle((in: (Int, String)) => s"${in._2} by ${names(in._1)}"),
      health.handle(_ => true)
    )
    assertEquals(router.serve(Request(Method.Get, "/users/1/name")), Response(200, "Ada"))
    assertEquals(
      router.serve(Request(Method.Get, "/users/2/posts/types")),
      Response(200, "types by Alan")
    )
    assertEquals(router.serve(Request(Method.Get, "/health")), Response(200, "true"))
  }

  test("a request no route matches is a 404") {
    val router = Router(userName.handle((id: Int) => names(id)))
    assertEquals(router.serve(Request(Method.Get, "/users/x/name")).status, 404)
    assertEquals(router.serve(Request(Method.Post, "/users/1/name")).status, 404)
    assertEquals(router.serve(Request(Method.Get, "/nowhere")).status, 404)
  }

  test("a handler of the wrong shape does not compile") {
    assertTypeError("userName.handle((id: String) => id)")
    assertTypeError("userName.handle((id: Int) => id)")
    assertTypeError("userName.handle((in: Tuple1[Int]) => \"Ada\")")
    assertTypeError("postTitle.handle((id: Int) => \"title\")")
    assertTypeError("health.handle(_ => \"yes\")")
  }
end Exercise02Spec
