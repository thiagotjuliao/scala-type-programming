# Writing a chapter

The order below is not a style preference. Each step exists because doing it
later produces a chapter that looks finished and teaches nothing.

## 1. The document first

`docs/theory/chNN-*.md`, from `docs/templates/chapter.md`. Write *Why this
exists* before anything else, and write it from the code that cannot be written
without the feature — not from the feature's name. A chapter that opens with
"Scala 3 has match types" has already lost the reader who wanted to know when
to reach for one.

*Pitfalls* is the section that dates best. Record what the error message
actually says, verbatim: six months later that text is how the chapter gets
found again.

## 2. The spec second

Before the stub, before the solution. The spec is the chapter's contract, and
writing it first is what keeps the exercise honest — an exercise written first
tends to get a spec that tests whatever it happens to do.

Two rules, both enforced by the gate:

**Every spec must compile against the unsolved stub.** `Unsolved` exists so an
unfinished exercise stays compilable; a spec that puts the claim in the test
body as plain code breaks the module instead of failing a test. Put the claim
inside an `assertTypeChecks` / `assertTypeError` snippet.

**Assert the rejection too.** `assertTypeChecks` alone cannot distinguish a
precise type from `Any`. Every positive claim wants the negative one beside it.

Then check that the assertion is *discriminating*: run it against the stub and
watch it fail. Type inference is generous, and an assertion can easily pass
before the exercise is solved — ascribing an expected type, for instance, lets
inference pick the element type up front and an invariant container will
swallow both pushes. If it passes red, it is not testing the exercise.

## 3. The stub third

It must compile, it must not work, and it must say what is wanted in prose —
the reader is not meant to reverse-engineer the spec. One hint, at the end,
pointing at the mechanism rather than the answer.

## 4. The solution last

The scaladoc on a solution is the most-read text in the chapter, because it is
where the reader arrives after failing. Explain *why* this is the answer, what
the compiler does with it, and what the near-miss version breaks — a solution
that only shows what compiles teaches nothing `git diff` would not have.

## 5. The walkthrough

`Walkthrough.scala` is the document in executable form. Every claim in it is
one the compiler is checking, which is the whole reason it is a source file and
not another Markdown block. It lives in the exercises module only; there is
nothing to key.

## Then

```bash
sbt practice                    # the new exercises are red
sbt verify                      # the answer key is green
./scripts/finish-chapter.sh NN  # stage, verify that tree, commit, tag, push
```

`finish-chapter` re-runs the gate against the staged tree and refuses to tag a
chapter that is missing a document, a walkthrough, a spec or a solution, or
whose answer key still contains `Unsolved` or `TODO`.

## Naming

| thing | form | example |
| --- | --- | --- |
| package | `chNN` + the slug without dashes | `typeprog.ch07matchtypes` |
| document | `chNN-slug.md` | `docs/theory/ch07-match-types.md` |
| exercise | `ExerciseNN.scala` | `Exercise03.scala` |
| spec | `ExerciseNNSpec.scala` | `Exercise03Spec.scala` |
| tag | `chNN` | `ch07` |
| commit | `chapter NN — lowercase title` | `chapter 07 — match types` |

Chapters `ch20` and up are for applied case studies, so the core trail can grow
without renumbering.
