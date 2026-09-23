package typeprog.ch02higherkinds

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise02.*

class Exercise02Spec extends TypeLevelSuite:

  /** The call every claim below is phrased through: generic in `F`, so the
    * compiler has to infer `F` from the argument before it can check the
    * functor against it.
    */
  def inc[F[_]](fa: F[Int])(F: Functor[F]): F[Int] = F.map(fa)(_ + 1)

  test("ErrorOr fixes the error type and leaves the value type open") {
    assertTypeChecks("evidence.sameType[ErrorOr[String][Int], Either[String, Int]]")
  }

  test("ErrorOr fixes the left side, not the right") {
    assertTypeError("evidence.sameType[ErrorOr[String][Int], Either[Int, String]]")
  }

  test("inference finds the functor's F from a plain Either") {
    assertTypeChecks(
      "val r: Either[String, Int] = inc(Right(1): Either[String, Int])(eitherFunctor[String])"
    )
  }

  test("the functor cannot be pointed at the left side") {
    assertTypeError("inc(Left(1): Either[Int, String])(eitherFunctor[String])")
  }

  // Against the stub `ErrorOr` is `Unsolved`, so no `Either` can be handed to
  // `map` as plain code. The cast is how these two reach the behaviour anyway:
  // it compiles against both, and against the stub it fails at runtime, which
  // is precisely a red test. `map`'s type arguments are spelled out for the
  // same reason: `Unsolved` gives inference no `A` to read off the argument.
  private def errorOr(e: Either[String, Int]): ErrorOr[String][Int] =
    e.asInstanceOf[ErrorOr[String][Int]]

  test("map transforms a Right") {
    assertEquals[Any, Any](eitherFunctor[String].map[Int, Int](errorOr(Right(1)))(_ + 1), Right(2))
  }

  test("map leaves a Left alone") {
    assertEquals[Any, Any](
      eitherFunctor[String].map[Int, Int](errorOr(Left("no")))(_ + 1),
      Left("no")
    )
  }
end Exercise02Spec
