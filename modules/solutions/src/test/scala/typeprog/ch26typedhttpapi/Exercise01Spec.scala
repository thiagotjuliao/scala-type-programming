package typeprog.ch26typedhttpapi

import typeprog.core.TypeLevelSuite

import Exercise01.*

object Exercise01Fixtures:
  val userPosts = Path.root / "users" / capture[Int]("id") / "posts"
  val post = Path.root / "users" / capture[Int]("id") / "posts" / capture[String]("slug")

import Exercise01Fixtures.*

class Exercise01Spec extends TypeLevelSuite:

  test("a capture adds its type to the path's; a literal adds nothing") {
    assertTypeChecks("val p: Path[EmptyTuple] = Path.root / \"users\"")
    assertTypeChecks("val p: Path[Int *: EmptyTuple] = userPosts")
    assertTypeChecks("val p: Path[(Int, String)] = post")
    assertTypeError("val p: Path[EmptyTuple] = userPosts")
    assertTypeError("val p: Path[(String, Int)] = post")
  }

  test("a path renders exactly the values its captures read") {
    assertEquals(userPosts.render(Tuple1(42)), "/users/42/posts")
    assertEquals(post.render((42, "hello")), "/users/42/posts/hello")
    assertEquals(Path.root.render(EmptyTuple), "/")
    assertTypeError("userPosts.render(Tuple1(\"42\"))")
    assertTypeError("post.render(Tuple1(42))")
  }

  // Compared as `Any`: against the unsolved stub `parse` returns a tuple of
  // unknown shape, and a typed comparison would not compile.
  test("a path parses back what it renders, and nothing else") {
    assertEquals[Any, Any](userPosts.parse("/users/42/posts"), Some(Tuple1(42)))
    assertEquals[Any, Any](post.parse("/users/42/posts/hello"), Some((42, "hello")))
    assertEquals[Any, Any](Path.root.parse("/"), Some(EmptyTuple))
    assertEquals[Any, Any](userPosts.parse("/users/x/posts"), None)
    assertEquals[Any, Any](userPosts.parse("/users/42/comments"), None)
    assertEquals[Any, Any](userPosts.parse("/users/42"), None)
  }

  test("parse is typed by the path") {
    assertTypeChecks("val p: Option[Int *: EmptyTuple] = userPosts.parse(\"/users/1/posts\")")
    assertTypeError("val p: Option[String *: EmptyTuple] = userPosts.parse(\"/users/1/posts\")")
  }
end Exercise01Spec
