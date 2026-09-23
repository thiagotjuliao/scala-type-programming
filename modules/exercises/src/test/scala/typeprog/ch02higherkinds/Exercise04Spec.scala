package typeprog.ch02higherkinds

import typeprog.core.{TypeLevelSuite, evidence}

import Exercise04.*

class Exercise04Spec extends TypeLevelSuite:

  val listFunctor: Functor[List] = new Functor[List]:
    def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)

  val optionFunctor: Functor[Option] = new Functor[Option]:
    def map[A, B](fa: Option[A])(f: A => B): Option[B] = fa.map(f)

  test("Compose nests the second constructor inside the first") {
    assertTypeChecks("evidence.sameType[Compose[List, Option][Int], List[Option[Int]]]")
  }

  test("the order of composition matters") {
    assertTypeError("evidence.sameType[Compose[List, Option][Int], Option[List[Int]]]")
  }

  test("Compose only accepts type constructors") {
    assertTypeError("type Nope = Compose[List, Int]")
  }

  test("the composed functor maps through both layers at once") {
    assertTypeChecks(
      """val r: List[Option[String]] =
           composed(listFunctor, optionFunctor).map(List(Some(1), None))(_.toString)"""
    )
  }

  test("the composed functor is a Functor like any other") {
    assertTypeChecks(
      "val f: Functor[[X] =>> List[Option[X]]] = composed(listFunctor, optionFunctor)"
    )
  }

  // See Exercise02Spec: the cast lets a behavioural test compile against the
  // stub, where it then fails at runtime.
  private def nested(xs: List[Option[Int]]): Compose[List, Option][Int] =
    xs.asInstanceOf[Compose[List, Option][Int]]

  test("map reaches every element and keeps every empty layer") {
    val listOfOptions = composed(listFunctor, optionFunctor)
    assertEquals[Any, Any](
      listOfOptions.map[Int, Int](nested(List(Some(1), None, Some(3))))(_ * 10),
      List(Some(10), None, Some(30))
    )
  }
end Exercise04Spec
