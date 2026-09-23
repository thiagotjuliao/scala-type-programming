package typeprog.ch02higherkinds

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise03.*

class Exercise03Spec extends TypeLevelSuite:

  def inc[F[_]](fa: F[Int])(F: Functor[F]): F[Int] = F.map(fa)(_ + 1)

  test("OnLeft leaves the left side open") {
    assertTypeChecks("evidence.sameType[OnLeft[String][Int], Either[Int, String]]")
  }

  test("OnLeft is not ErrorOr under another name") {
    assertTypeError("evidence.sameType[OnLeft[String][Int], Either[String, Int]]")
  }

  test("with F spelled out, the functor maps the left side") {
    assertTypeChecks(
      "val r: Either[Int, String] = inc[OnLeft[String]](Left(1))(leftFunctor[String])"
    )
  }

  // Not a claim about the exercise so much as about the compiler: inference
  // lines the argument up with the *last* parameter, so it reads this
  // `Either[Int, String]` as some `F[String]` and never finds `F[Int]` at all.
  // It holds against the stub too. It is here so that the solved exercise
  // keeps demonstrating it.
  test("without F spelled out, inference cannot find the left side") {
    assertTypeError("inc(Left(1): Either[Int, String])(leftFunctor[String])")
  }

  // See Exercise02Spec: the cast lets a behavioural test compile against the
  // stub, where it then fails at runtime.
  private def onLeft(e: Either[Int, String]): OnLeft[String][Int] =
    e.asInstanceOf[OnLeft[String][Int]]

  test("map transforms a Left") {
    assertEquals[Any, Any](leftFunctor[String].map[Int, Int](onLeft(Left(1)))(_ + 1), Left(2))
  }

  test("map leaves a Right alone") {
    assertEquals[Any, Any](
      leftFunctor[String].map[Int, Int](onLeft(Right("ok")))(_ + 1),
      Right("ok")
    )
  }
end Exercise03Spec
