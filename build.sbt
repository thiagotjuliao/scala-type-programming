ThisBuild / scalaVersion := "3.9.0"
ThisBuild / organization := "dev.thiagojuliao"
ThisBuild / version      := "0.1.0"

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Wunused:imports",
    // Type-level recursion (Peano arithmetic, tuple folds, derivation) blows
    // past the default budget of 32 long before it blows past anything that
    // deserves to be called a limit.
    "-Xmax-inlines:128"
  ),
  libraryDependencies += "org.scalameta" %% "munit" % "1.3.6" % Test,
  testFrameworks += new TestFramework("munit.Framework"),
  // A snippet handed to `assertTypeChecks` is type-checked in the scope of its
  // call site, so the imports it leans on are genuinely used — by a string the
  // unused-import checker has no reason to look inside. Left on, every chapter
  // spec in the repository would report the one import that makes it work as
  // dead. The check stays on for main sources, where it means something.
  Test / scalacOptions := scalacOptions.value.filterNot(_ == "-Wunused:imports")
)

// Shared vocabulary: the `Unsolved` placeholder that keeps an unfinished
// exercise compiling, the evidence helpers, and — in its *test* sources — the
// `TypeLevelSuite` base class every chapter's specs extend.
lazy val core = project
  .in(file("modules/core"))
  .settings(commonSettings, name := "typeprog-core")

// `test->test` is what publishes TypeLevelSuite to the chapters: sbt does not
// put a module's test classes on a dependent's test classpath otherwise.
lazy val chapterDeps = core % "compile->compile;test->test"

// The exercises. Every file here compiles as it ships; what is missing is the
// *right* type, so an unsolved exercise shows up as a red test, never as a
// build that will not compile.
lazy val exercises = project
  .in(file("modules/exercises"))
  .dependsOn(chapterDeps)
  .settings(commonSettings, name := "typeprog-exercises")

// The answer key. This module is the one the release gate holds to a green
// test run.
lazy val solutions = project
  .in(file("modules/solutions"))
  .dependsOn(chapterDeps)
  .settings(commonSettings, name := "typeprog-solutions")

// Scratch space. Nothing here is verified and nothing depends on it.
lazy val playground = project
  .in(file("modules/playground"))
  .dependsOn(chapterDeps)
  .settings(commonSettings, name := "typeprog-playground")

lazy val root = project
  .in(file("."))
  .aggregate(core, exercises, solutions, playground)
  .settings(
    name           := "scala-type-programming",
    publish / skip := true
  )

// The release gate, and the only definition of "green" in this repository:
// everything is formatted, the answer key passes, and the exercises still
// compile. `scripts/finish-chapter.sh` runs exactly this.
//
// `testFull`, not `test`: under sbt 2 the `test` task is incremental — it runs
// what failed last time, what never ran, and what a changed dependency touched,
// and nothing else. Excellent while working, useless as a gate, because the
// greenest possible run of it is the one that ran no tests at all.
addCommandAlias("verify", "scalafmtCheckAll; solutions/testFull; exercises/Test/compile")

// The practice run. Red by design — every unsolved exercise is a failure here,
// and the failures are the to-do list.
addCommandAlias("practice", "exercises/testFull")
