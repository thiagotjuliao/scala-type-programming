package typeprog.ch04literaluniontypes

import scala.collection.mutable.ListBuffer

import typeprog.core.TypeLevelSuite

import Exercise04.*

class Exercise04Spec extends TypeLevelSuite:

  final class Recorder extends Logger:
    val lines: ListBuffer[String] = ListBuffer.empty
    def log(line: String): Unit = lines += line

  final class Fixed(time: Long) extends Clock:
    def now(): Long = time

  /** One value that is both, declared as both — no trait "LoggerWithClock". */
  object System extends Logger, Clock:
    def log(line: String): Unit = ()
    def now(): Long = 0L

  // A guard rather than a claim: `System` is a `Logger`, so the stub takes it
  // too. It catches a solution that rejects too much.
  test("stamp takes a value that is both a Logger and a Clock") {
    assertTypeChecks("stamp(System)(\"up\")")
  }

  test("stamp rejects a value that is only one of the two") {
    assertTypeError("stamp(Recorder())(\"up\")")
    assertTypeError("stamp(Fixed(0L))(\"up\")")
  }

  test("combine makes one value that is both") {
    assertTypeChecks("val both: Logger & Clock = combine(Recorder(), Fixed(0L))")
    assertTypeChecks("stamp(combine(Recorder(), Fixed(0L)))(\"up\")")
  }

  test("combine's result is not a more specific type than it promises") {
    assertTypeError("val r: Recorder = combine(Recorder(), Fixed(0L))")
  }

  // These compile against the stub too — `combine` returns a `Logger` there,
  // and `stamp` takes one — so they run, and against `???` they fail.
  test("stamp logs the message with the time in front") {
    val recorder = Recorder()
    stamp(combine(recorder, Fixed(42L)))("up")
    assertEquals(recorder.lines.toList, List("[42] up"))
  }

  // The clock's side is what "[42] up" above already reads back.
  test("combine logs through the logger it was given") {
    val recorder = Recorder()
    combine(recorder, Fixed(7L)).log("direct")
    assertEquals(recorder.lines.toList, List("direct"))
  }
end Exercise04Spec
