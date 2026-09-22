# Working in this repository

A course on type-level programming in Scala 3, not a library. The deliverable of
any change is a chapter someone can learn from; compiling is the floor, not the
goal.

## Before writing a chapter

Read [CONTRIBUTING.md](CONTRIBUTING.md) and follow its order — document, spec,
stub, solution, walkthrough. It is not a style preference; each step out of
order produces a chapter that looks finished and teaches nothing.

Scaffold with `./scripts/new-chapter.sh NN "Title"` rather than creating files
by hand. The gate checks for exactly what the templates lay out.

## The two rules that break things quietly

**Every exercise spec must compile against the unsolved stub.** That is what
`typeprog.core.Unsolved` is for. A claim that only type-checks once the
exercise is solved goes inside an `assertTypeChecks` / `assertTypeError`
snippet — put it in the test body as plain code and an unsolved exercise stops
the module compiling instead of failing a test.

**Check that an assertion is discriminating** by running it against the stub
and watching it fail. Inference is generous; assertions that pass while the
exercise is unsolved are the most common defect here, and the gate cannot catch
them.

## Commands

| command | |
| --- | --- |
| `sbt verify` | the gate: `scalafmtCheckAll`, `solutions/testFull`, `exercises/Test/compile` |
| `sbt practice` | `exercises/testFull` — red by design |
| `./scripts/finish-chapter.sh NN` | stage, verify the staged tree, commit, tag `chNN`, push |

`testFull`, not `test`: under sbt 2 `test` is incremental and its greenest
possible run is the one that ran nothing.

## Conventions

- Scala 3 indentation syntax, formatted by `.scalafmt.conf`. Run `sbt scalafmtAll`.
- English everywhere: code, comments, documents, commit messages.
- Commits are `chapter NN — lowercase title`; tags are `chNN`, annotated.
- Do not commit or tag by hand — `finish-chapter` is what verifies the tree that
  actually gets tagged.
