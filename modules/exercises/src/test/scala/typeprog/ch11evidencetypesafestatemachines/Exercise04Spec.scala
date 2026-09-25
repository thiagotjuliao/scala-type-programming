package typeprog.ch11evidencetypesafestatemachines

import typeprog.core.TypeLevelSuite

import Exercise04.*

class Exercise04Spec extends TypeLevelSuite:

  test("closing an opened door gives a closed door") {
    assertTypeChecks("val d: Door[Closed] = Door.opened.on[Close]")
    assertTypeError("val d: Door[Opened] = Door.opened.on[Close]")
  }

  test("a closed door locks, and unlocks back to closed") {
    assertTypeChecks("val d: Door[Locked] = Door.opened.on[Close].on[Lock]")
    assertTypeChecks("val d: Door[Closed] = Door.opened.on[Close].on[Lock].on[Unlock]")
  }

  test("the whole cycle comes back to an opened door") {
    assertTypeChecks("val d: Door[Opened] = Door.opened.on[Close].on[Lock].on[Unlock].on[Open]")
  }

  // The type names in the message are printed as the call site sees them —
  // here, qualified with the enclosing object — so the checks leave room.
  test("a forbidden event does not compile, and says which and where") {
    assertTypeErrorContains("Door.opened.on[Lock]", "Lock a door that is")
    assertTypeErrorContains("Door.opened.on[Lock]", "Opened")
    assertTypeErrorContains("Door.opened.on[Close].on[Lock].on[Open]", "Open a door that is")
    assertTypeErrorContains("Door.opened.on[Close].on[Lock].on[Open]", "Locked")
  }

  test("an event that changes nothing is forbidden too") {
    assertTypeError("Door.opened.on[Open]")
    assertTypeError("Door.opened.on[Unlock]")
  }
end Exercise04Spec
