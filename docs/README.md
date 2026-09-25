# The chapters

Twelve chapters, each one a tag. The order is load-bearing: every chapter uses
the vocabulary of the ones before it, and chapters 07 onwards are hard to read
out of sequence.

| # | chapter | covers | tag |
| --- | --- | --- | --- |
| 01 | [Type system foundations](theory/ch01-type-foundations.md) | subtyping, variance, bounds, type members, `=:=` / `<:<` | `ch01` |
| 02 | [Higher-kinded types & type lambdas](theory/ch02-higher-kinds.md) | `F[_]`, kinds, `[X] =>> F[X]`, kind mismatch errors | `ch02` |
| 03 | [Path-dependent & dependent function types](theory/ch03-path-dependent-types.md) | `a.B`, `(x: A) => x.B`, when the value decides the type | `ch03` |
| 04 | [Literal, singleton, union & intersection types](theory/ch04-literal-union-types.md) | `42` as a type, `A & B`, `A \| B`, `Matchable` | `ch04` |
| 05 | [Opaque types & phantom newtypes](theory/ch05-opaque-types.md) | zero-cost wrappers, tagging, types with no values | `ch05` |
| 06 | [Type classes, givens & implicit search](theory/ch06-type-classes-givens.md) | `given`/`using`, resolution order, priority, ambiguity | `ch06` |
| 07 | [Match types](theory/ch07-match-types.md) | computation over types, reduction, `Tuple.Head` | `ch07` |
| 08 | [`inline` & `scala.compiletime`](theory/ch08-inline-scala-compiletime.md) | `constValue`, `erasedValue`, `summonFrom`, `error` | `ch08` |
| 09 | [Tuples as HLists](theory/ch09-tuples-as-hlists.md) | type-level lists, Peano arithmetic, `Tuple.Concat` | `ch09` |
| 10 | [`Mirror` & generic derivation](theory/ch10-mirror-generic-derivation.md) | `derives`, `MirroredElemTypes`, generic type classes | `ch10` |
| 11 | [Evidence & type-safe state machines](theory/ch11-evidence-type-safe-state-machines.md) | `NotGiven`, phantom state, builders that cannot be misused | `ch11` |
| 12 | [Macros: quotes & splices](theory/ch12-macros-quotes-splices.md) | `'{ }`, `${ }`, `Expr`, `TypeRepr`, when to stop | `ch12` |

Chapters `ch20` and up are reserved for applied case studies — a type-safe
builder, units of measure, compile-time schema validation — so the core trail
can be extended without renumbering.

## Reading one chapter

1. the theory document here, for the motivation and the pitfalls;
2. `Walkthrough.scala` in the exercises module, which is the same content with
   the compiler checking it;
3. the exercises, red first.

## Writing one

See [CONTRIBUTING.md](../CONTRIBUTING.md). The short version: the spec comes
first, and `scripts/new-chapter.sh` lays out everything the gate will ask for.

## Also here

- [glossary.md](glossary.md) — the vocabulary, one line each, with the chapter
  that introduces each term
- [references.md](references.md) — the primary sources
- [templates/](templates) — what `new-chapter.sh` renders
