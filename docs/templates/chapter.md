# Chapter {{NN}} — {{TITLE}}

> Status: **draft**. Remove every `<!-- TODO -->` marker below before closing the
> chapter — `scripts/finish-chapter.sh` refuses to tag a chapter that still has one.

## Why this exists

<!-- TODO: the problem this chapter's machinery solves. Start from the code that
     cannot be written without it, not from the feature's name. -->

## The idea

<!-- TODO: the mechanism itself, in prose. What the compiler is actually doing. -->

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/{{PACKAGE}}/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

<!-- TODO: what goes wrong in practice, and what the error message looks like
     when it does. The error text is half of what makes a chapter usable later. -->

## Exercises

| # | asks for |
| --- | --- |
| <!-- TODO --> | |

```bash
sbt "exercises/testOnly typeprog.{{PACKAGE}}.*"
```

## Further reading

<!-- TODO: specs, SIPs, papers, talks. Link the primary source, not a blog post
     summarising it, unless the blog post is genuinely better. -->
