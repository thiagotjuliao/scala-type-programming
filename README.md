# Scala type programming

Type-level programming in **Scala 3.9.0**, laid out as a course that runs.
Twelve chapters, each one a theory document, a walkthrough the compiler
checks, and exercises whose tests stay red until they are solved.

## Getting started

```bash
sbt verify      # the gate: formatted, answer key green, exercises compile
sbt practice    # the to-do list: every unsolved exercise, red
```

`practice` is supposed to fail. The failures are the point — each one names an
exercise that has not been done yet.

## How a chapter is laid out

Take chapter 01. It lives in four places:

| where | what |
| --- | --- |
| `docs/theory/ch01-type-foundations.md` | the prose: motivation, mechanism, pitfalls |
| `modules/exercises/…/ch01foundations/Walkthrough.scala` | the same content, executable |
| `modules/exercises/…/ch01foundations/ExerciseNN.scala` | the stubs, and their specs |
| `modules/solutions/…/ch01foundations/ExerciseNN.scala` | the answer key, and the same specs |

The exercise and solution modules share a package name, so a solved exercise
and its answer are the same file in two trees — `diff` reads cleanly.

## Why the tests look like the way they do

A wrong answer to a type-level exercise is normally a *compile* error, and a
compile error in one file takes down the whole module. A repository full of
unsolved exercises would not build at all, and the one thing it could never
tell you is which exercise you got wrong.

Two devices avoid that:

**`Unsolved`** is the placeholder a stub answers with. It is a real type, so
the stub compiles; it is uninhabited and satisfies nothing, so every assertion
about it fails.

**Snippet assertions** move the claim into a string that is type-checked where
it sits:

```scala
test("a source of dogs is a source of animals") {
  assertTypeChecks("evidence.subtypeOf[Source[Dog], Source[Animal]]")
}

test("a channel is invariant in both directions") {
  assertTypeError("evidence.subtypeOf[Channel[Dog], Channel[Animal]]")
}
```

Both come from `TypeLevelSuite` in the core module, which builds them on
munit's `compileErrors`. The second one matters as much as the first: a design
that accepts the right code is only interesting if it also *rejects* the wrong
code, and a suite that never checks a rejection cannot tell a precise type from
`Any`.

The rule that follows, and that the gate enforces: **every spec must compile
against the unsolved stub.** A claim that only type-checks once the exercise is
solved belongs inside a snippet, never in the test body as plain code.

## Running

| command | what it does |
| --- | --- |
| `sbt verify` | the release gate — `scalafmtCheckAll`, the answer key, and the exercises compiling |
| `sbt practice` | every exercise spec, red where work remains |
| `sbt "exercises/testOnly typeprog.ch01foundations.*"` | one chapter's exercises |
| `sbt "solutions/testOnly typeprog.ch01foundations.*"` | the same, against the answer key |
| `sbt scalafmtAll` | apply the formatting contract |
| `sbt playground/console` | a REPL with everything on the classpath |

Under sbt 2 the plain `test` task is incremental: it runs what failed last
time, what never ran, and what a changed dependency touched. That is what you
want while working and useless as a gate, so `verify` uses `testFull`.

## Handing in a solved chapter

```bash
./scripts/solve-chapter.sh 2 my-solutions
```

```powershell
./scripts/solve-chapter.ps1 2 my-solutions
```

Commits your work on chapter 2's exercises as `solutions — chapter 02` and
pushes it to `my-solutions`, switching to that branch first (or creating it
from the current one). Only the chapter's exercise sources go into the commit;
anything else modified is listed and left alone. As with `finish-chapter`, the
check runs against the staged tree in `target/solve-chapter/`: formatting, and
every spec of the chapter green. A chapter that is not solved yet is not
committed. `main` is refused — solutions pushed there would reach every other
learner along with the next chapter. `--no-push` (`-NoPush`) stops at the commit.

## Starting a new chapter

```bash
./scripts/new-chapter.sh 2 "Higher-kinded types"
```

```powershell
./scripts/new-chapter.ps1 2 "Higher-kinded types"
```

Every script in `scripts/` comes in two flavours: `.sh` for bash (macOS, Linux,
Git Bash) and `.ps1` for PowerShell 7+ on Windows. They do the same thing and
take the same arguments; only the flags change shape — `-n 4` becomes
`-Exercises 4`, `--no-push` becomes `-NoPush`.

That renders the theory document, the walkthrough, and three exercises with
their specs on both sides, all from `docs/templates/`. Nothing is ever
overwritten, so it is safe to re-run; `--add` appends more exercises to a
chapter that already exists.

## Closing a chapter

```bash
./scripts/finish-chapter.sh 2
```

The script refuses to close a half-written chapter. It stages the commit first,
then checks out the *index* into `target/finish-chapter/` and runs the gate in
there — against the tree that is about to be tagged, not against your working
directory. A file you forgot to `git add` is present on disk and absent from
the tag, which is exactly how a green run publishes a tree that does not
compile.

In that copy it checks:

- the theory document exists and has no template `<!-- TODO -->` markers left;
- the chapter has a `Walkthrough.scala`;
- every exercise has a spec, a solution, and the solution's spec;
- the answer key mentions neither `Unsolved` nor `TODO`;
- `sbt verify` passes.

Fail any of them and the index is restored, nothing is committed, nothing is
tagged, and the tree that failed is left in `target/finish-chapter/` to look at.

Passing, it commits and creates an **annotated tag**:

| | |
| --- | --- |
| commit | `chapter 01 — type system foundations` |
| tag | `ch01` |
| tag subject | `Chapter 01 — Type system foundations` |

Finally it runs `git push --follow-tags`, so the commit and the tag travel
together.

For the progress board: `git tag -l 'ch*'`.

## Layout

```text
build.sbt                     four modules, one gate, two aliases
docs/
  README.md                   the twelve chapters, in order
  theory/chNN-*.md            one document per chapter
  glossary.md                 the vocabulary, one line each
  references.md               primary sources
  templates/                  what new-chapter.sh renders
modules/
  core/                       Unsolved, the evidence helpers, TypeLevelSuite
  exercises/                  walkthroughs, stubs, specs
  solutions/                  the answer key, and the same specs
  playground/                 scratch; verified by nothing
scripts/
  new-chapter.{sh,ps1}        renders a chapter from the templates
  finish-chapter.{sh,ps1}     stages, verifies that tree, commits, tags, pushes
```

## Conventions

Scala 3 indentation syntax throughout, formatted by
[.scalafmt.conf](.scalafmt.conf) — `maxColumn = 100`, no vertical alignment, so
every diff shows a change in meaning rather than in whitespace. Code, comments,
documents and commit messages are in English.

The build runs on sbt 2.0.8. It is developed on JDK 26 and CI builds it on 21.
