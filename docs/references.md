# References

Primary sources, preferred over summaries of them. Each was resolving when it
was added; a chapter's own *Further reading* section carries the links specific
to it.

## Language

- [Scala 3 reference](https://docs.scala-lang.org/scala3/reference/) — the specification
  in readable form. The `new-types` and `contextual` sections are most of this repository.
- [Scala 3 book — types](https://docs.scala-lang.org/scala3/book/types-introduction.html) —
  the gentler path through the same material.
- [Match types](https://docs.scala-lang.org/scala3/reference/new-types/match-types.html) —
  worth reading in full before chapter 07; the reduction rules are the part that surprises.
- [Type class derivation](https://docs.scala-lang.org/scala3/reference/contextual/derivation.html) —
  `Mirror`, and what the compiler synthesises for a case class.

## Metaprogramming

- [Macros guide](https://docs.scala-lang.org/scala3/guides/macros/index.html) — start at
  [inline](https://docs.scala-lang.org/scala3/guides/macros/inline.html) and go no further
  until it is boring.
- [Scala improvement proposals](https://github.com/scala/improvement-proposals) — how the
  features got their present shape, and which trade-offs were argued over.

## Libraries worth reading as source

- [shapeless-3](https://github.com/typelevel/shapeless-3) — generic programming rebuilt on
  Scala 3's own primitives; small enough to read.
- [The Type Astronaut's Guide to Shapeless](https://underscore.io/books/shapeless-guide/) —
  Scala 2 and shapeless 2, so the encodings are dated. The *reasoning* is not, and it is
  still the best long-form explanation of why generic derivation works.

## Papers

- Odersky, Blanvillain et al., *Simplicitly: foundations and applications of implicit
  function types* — POPL 2018, doi:10.1145/3158130.
- Blanvillain, Brachthäuser, Kjaer, Odersky, *Type-level programming with match types* —
  POPL 2022. The formal account of what chapter 07 does informally.

## Tooling

- [munit](https://scalameta.org/munit/docs/getting-started.html) — in particular
  `compileErrors`, which every spec in this repository is built on.
