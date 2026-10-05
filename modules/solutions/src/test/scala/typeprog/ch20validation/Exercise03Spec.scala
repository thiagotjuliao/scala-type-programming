package typeprog.ch20validation

import scala.compiletime.summonFrom

import typeprog.core.TypeLevelSuite

import Exercise03.*
import Exercise03.Validated.{Invalid, Valid}

class Exercise03Spec extends TypeLevelSuite:

  type Checked[A] = Validated[List[String], A]

  // The behavioural tests need an instance as a value, and the stub has none:
  // a plain `summon` would not compile against it. `summonFrom` turns the
  // missing instance into a failing test instead.
  private inline def applicative[F[_]]: Applicative[F] = summonFrom {
    case instance: Applicative[F] => instance
    case _ => fail("no Applicative instance was found")
  }

  // Written once, against the abstraction; the tests below run it on both.
  private def pair[F[_], A, B](fa: F[A], fb: F[B])(using F: Applicative[F]): F[(A, B)] =
    F.map2(fa, fb)((_, _))

  test("Option is applicative, with no import") {
    assertTypeChecks("summon[Applicative[Option]]")
  }

  test("Validated is applicative once its error type is fixed, with no import") {
    assertTypeChecks("summon[Applicative[[A] =>> Validated[List[String], A]]]")
    assertTypeChecks("summon[Applicative[Checked]]")
    assertTypeChecks("summon[Applicative[[A] =>> Validated[Int, A]]]")
  }

  // Guards: they hold against the stub too. The first catches an instance
  // that forgot to ask for a Semigroup; the second is chapter 02's kind error.
  test("but only for an error type that can be combined") {
    assertTypeError("summon[Applicative[[A] =>> Validated[Boolean, A]]]")
  }

  test("and never for Validated itself, which takes two parameters") {
    assertTypeError("summon[Applicative[Validated]]")
  }

  test("Option: pure is Some, map2 is None as soon as either side is") {
    val F = applicative[Option]
    assertEquals(F.pure(1), Some(1))
    assertEquals(F.map2(Some(20), Some(1))(_ + _), Some(21))
    assertEquals(F.map2(Option.empty[Int], Some(1))(_ + _), None)
    assertEquals(F.map2(Some(20), Option.empty[Int])(_ + _), None)
  }

  test("Validated: pure is Valid, map2 keeps the errors of both sides") {
    val F = applicative[Checked]
    assertEquals(F.pure(1), Valid(1))
    assertEquals(F.map2(F.pure(20), F.pure(1))(_ + _), Valid(21))
    assertEquals(
      F.map2(Invalid(List("a")): Checked[Int], Invalid(List("b")): Checked[Int])(_ + _),
      Invalid(List("a", "b"))
    )
  }

  test("map comes with every applicative") {
    assertEquals(applicative[Option].map(Some(20))(_ + 1), Some(21))
    assertEquals(applicative[Option].map(Option.empty[Int])(_ + 1), None)
    assertEquals(applicative[Checked].map(Valid(20))(_ + 1), Valid(21))
    assertEquals(applicative[Checked].map(Invalid(List("a")))(_ => 0), Invalid(List("a")))
  }

  test("code written against the abstraction behaves as the instance decides") {
    assertEquals(
      pair(Option.empty[Int], Option.empty[String])(using applicative[Option]),
      None
    )
    assertEquals(
      pair(Invalid(List("a")): Checked[Int], Invalid(List("b")): Checked[String])(using
        applicative[Checked]
      ),
      Invalid(List("a", "b"))
    )
  }
end Exercise03Spec
