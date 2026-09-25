package typeprog.ch11evidencetypesafestatemachines

import typeprog.core.TypeLevelSuite

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  // `build` compiles against the stub in every state, so these run; against
  // `???` they fail, which is a red test.
  test("a URL and a method make a request, in either order") {
    assertEquals(RequestBuilder().url("/a").method("GET").build, Request("GET", "/a"))
    assertEquals(RequestBuilder().method("POST").url("/b").build, Request("POST", "/b"))
  }

  test("build without a URL does not compile, and says so") {
    assertTypeErrorContains("""RequestBuilder().method("GET").build""", "no URL")
  }

  test("build without a method does not compile, and says so") {
    assertTypeErrorContains("""RequestBuilder().url("/a").build""", "no method")
  }

  test("build with neither does not compile") {
    assertTypeError("RequestBuilder().build")
  }

  test("a field cannot be set twice") {
    assertTypeError("""RequestBuilder().url("/a").url("/b")""")
    assertTypeError("""RequestBuilder().method("GET").url("/a").method("POST")""")
  }
end Exercise03Spec
