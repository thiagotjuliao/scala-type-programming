package typeprog.ch26typedhttpapi

import typeprog.core.TypeLevelSuite

import Exercise04.*

object Exercise04Fixtures:
  val id = capture[Int]("id")
  val slug = capture[String]("slug")

import Exercise04Fixtures.*

class Exercise04Spec extends TypeLevelSuite:

  test("a path written as a string is the path the calls would build") {
    assertEquals(path"/users/$id/posts".render(Tuple1(42)), "/users/42/posts")
    assertEquals(path"/users/$id/posts/$slug".render((42, "hello")), "/users/42/posts/hello")
    assertEquals(path"/".render(EmptyTuple), "/")
    assertEquals[Any, Any](path"/users/$id/posts".parse("/users/7/posts"), Some(Tuple1(7)))
  }

  test("its type lists the captures, in order") {
    assertTypeChecks("val p: Path[EmptyTuple] = path\"/health\"")
    assertTypeChecks("val p: Path[Int *: EmptyTuple] = path\"/users/$id/posts\"")
    assertTypeChecks("val p: Path[(Int, String)] = path\"/users/$id/posts/$slug\"")
    assertTypeError("val p: Path[(String, Int)] = path\"/users/$id/posts/$slug\"")
    assertTypeError("path\"/users/$id\".render(Tuple1(\"x\"))")
  }

  test("a path that is not one does not compile, and says why") {
    assertTypeErrorStartsWith("path\"users/$id\"", "a path starts with /")
    assertTypeErrorStartsWith("path\"/users//posts\"", "a path has no empty segment")
    assertTypeErrorStartsWith("path\"/users/a b\"", "invalid segment: 'a b'")
    assertTypeErrorStartsWith("path\"/users/x$id\"", "a capture must be a whole segment")
    assertTypeErrorStartsWith("path\"/users/${id}x\"", "a capture must be a whole segment")
  }

  test("only a Capture can be interpolated") {
    assertTypeErrorStartsWith(
      "{ val n = 1; path\"/users/$n\" }",
      "only a Capture can be interpolated, not Int"
    )
  }
end Exercise04Spec
