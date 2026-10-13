package typeprog.ch26typedhttpapi

import typeprog.core.TypeLevelSuite

import Exercise03.*

object Exercise03Fixtures:
  enum Role derives Codec:
    case Admin, Member

  val userName = Endpoint.get(Path.root / "users" / capture[Int]("id") / "name").returning[String]

  val postTitle = Endpoint
    .get(Path.root / "users" / capture[Int]("id") / "posts" / capture[String]("slug"))
    .returning[String]

  val roles = Endpoint.get(Path.root / "users" / capture[Int]("id") / "roles").returning[List[Role]]

  val health = Endpoint.get(Path.root / "health").returning[Boolean]

  val names = Map(1 -> "Ada", 2 -> "Alan")

  val router = Router(
    userName.handle((id: Int) => names(id)),
    postTitle.handle((in: (Int, String)) => s"${in._2} by ${names(in._1)}"),
    roles.handle((id: Int) => if id == 1 then List(Role.Admin, Role.Member) else List(Role.Member)),
    health.handle(_ => true)
  )

  val client = Client(router.serve)

import Exercise03Fixtures.*

class Exercise03Spec extends TypeLevelSuite:

  // Compared as `Any`: against the unsolved stub `call` returns an
  // `Either[String, Any]`.
  test("a client calls the server through the same endpoints") {
    assertEquals[Any, Any](client.call(userName)(1), Right("Ada"))
    assertEquals[Any, Any](client.call(postTitle)((2, "types")), Right("types by Alan"))
    assertEquals[Any, Any](client.call(roles)(1), Right(List(Role.Admin, Role.Member)))
    assertEquals[Any, Any](client.call(health)(()), Right(true))
  }

  test("the endpoint types the arguments and the result") {
    assertTypeChecks("val r: Either[String, String] = client.call(userName)(1)")
    assertTypeChecks("val r: Either[String, List[Role]] = client.call(roles)(1)")
    assertTypeError("val r: Either[String, Int] = client.call(userName)(1)")
    assertTypeError("client.call(userName)(\"1\")")
    assertTypeError("client.call(postTitle)(1)")
    assertTypeError("client.call(userName)((1, \"x\"))")
  }

  test("a failure on the other side is a Left that says what happened") {
    assertEquals[Any, Any](
      Client(Router().serve).call(userName)(1),
      Left("404 no route for Get /users/1/name")
    )
    assertEquals[Any, Any](
      Client(_ => Response(200, "maybe")).call(health)(()),
      Left("cannot read the body: maybe")
    )
  }
end Exercise03Spec
