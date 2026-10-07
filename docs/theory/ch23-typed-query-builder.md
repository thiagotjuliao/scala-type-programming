# Chapter 23 — Typed query builder

> Status: **done** — `git show ch23`

The fourth case study. A query builder is where a program's types meet data
whose shape lives somewhere else — in a table — and the usual answer is to
give up at the border: build a string, get rows of `Any` back, and cast. This
chapter keeps the types across the border. A column carries its name and the
type of its values; a `select` computes the type of the rows it returns from
the columns it was given (chapters 07 and 09); and a column belongs to its
table, so one table's query cannot be handed another's (chapter 03).

## Why this exists

A query over an untyped row:

```scala
val rows: List[Map[String, Any]] = db.run("select id, name from users")

val names = rows.map(_("name").asInstanceOf[String])  // a typo is a runtime error
val ids = rows.map(_("email").asInstanceOf[Int])      // so is a column not selected
                                                      // and a wrong type, at the cast
```

Everything the compiler could check is a string or a cast: the column names,
the columns the query returns, the type of each value, and which table a
column belongs to. A refactoring that renames a column compiles, and fails
when the query runs.

Typing the result by hand is no better. `def run(...): List[(Int, String)]`
is a promise the signature makes and nothing checks: change the `select` and
the declared type stays where it was. What is wanted is the result type
*computed from the select* — the same columns, in the same order, each giving
its value type — so that there is nothing to keep in step.

## The idea

### A column knows its name and its type

```scala
abstract class Table(val tableName: String):
  final class Column[N <: String & Singleton, A] private[Table] (val name: N)
  protected def column[N <: String & Singleton, A](name: N): Column[N, A] = Column(name)

object Users extends Table("users"):
  val id: Column["id", Int] = column("id")
  val name: Column["name", String] = column("name")
```

`A` is the type of the column's values; no value of it is stored, so it is a
phantom (chapter 05). `N` is the column's name as a literal type (chapter 04),
and it is not decoration: `name` and `email` are both `String` columns, and
without `N` they would have the same type. Anything that looks a column up
*by its type* — a position in a row, below — could not tell them apart.

The constructor is `private[Table]`: a column is declared by its table, not
built by whoever needs one.

### The row type is computed from the columns

A select takes a tuple of columns and remembers it in its type:
`Users.select((Users.id, Users.name))` is a `Query[(Users.Column["id", Int],
Users.Column["name", String])]`. The rows it returns are that tuple with each
column replaced by its value type — one match type for a column, mapped over
the tuple (chapters 07 and 09):

```scala
type Value[C] = C match
  case Column[n, a] => a

type Values[Cs <: Tuple] = Tuple.Map[Cs, Value]
```

`Values[(Column["id", Int], Column["name", String])]` reduces to `(Int,
String)`, and `run` returns a `List` of it. Change the select and the result
type changes with it; there is nothing to declare.

### A column belongs to its table

`Column` is declared inside `Table`, so every table has its own:
`Users.Column` and `Orders.Column` are different classes (chapter 03), even
when both have an `"id"` column of `Int`s. A method of the table that takes a
`Column[N, A]` takes only its own — a `where(Orders.id)` on a `Users` query is
a type mismatch.

`select` takes a whole tuple, so the parameter's type cannot say it. The tuple
is checked as a fact about it instead (chapter 11): every element is one of
this table's columns, which is `Tuple.Union[Cs] <:< Column[?, ?]` — the union
of the element types conforms to this table's column type.

Leaving the check out does not leave the select unchecked; it moves the
failure. `Values` of a tuple holding an `Orders` column is stuck — the
compiler cannot prove the `Orders` column disjoint from the `Users` pattern —
and a stuck type in a *result* is not an error. The select compiles, and the
failure turns up later, wherever its rows are used (see *Pitfalls*).

### A position, computed and handed to the runtime

A row holds its values as the tuple `Values[Cs]`. Reading one by its column,
`row(Users.name)`, needs the column's position in `Cs` — chapter 09's
`IndexOf`, a match type counting with `compiletime.ops.int.S`. The compiler
reduces it to a literal, `1`; `ValueOf[1]` (chapter 04) turns the literal into
the value the runtime indexes with. A column that was not selected has no
position, `IndexOf` is stuck, there is no `ValueOf` for a stuck type, and the
call does not compile.

The value's type comes from the column itself: `apply[N, A](column:
Column[N, A])` returns an `A`. The position is what proves the column is in
the row; the column says what it holds.

## In code

The executable version of this chapter is
[`Walkthrough.scala`](../../modules/exercises/src/main/scala/typeprog/ch23typedquerybuilder/Walkthrough.scala).
Read it alongside this document; every claim it makes is one the compiler is
checking.

## Pitfalls

Measured on 2026-10-06 with Scala 3.9.0; package prefixes trimmed from the
messages.

**A select with no evidence takes any column.** Without the `Tuple.Union`
evidence, `Users.select(Tuple1(Orders.id))` compiles. `Values` of an `Orders`
column is stuck, and a stuck result is a type like any other; the error turns
up where the rows are used:

```
Found:    List[Users.Value[Orders.Column[("id" : String), Int]] *: EmptyTuple]
Required: List[Tuple1[Int]]

Note: a match type could not be fully reduced:

  trying to reduce  Users.Value[Orders.Column[("id" : String), Int]]
  failed since selector Orders.Column[("id" : String), Int]
  does not match  case Users.Column[n, a] => a
  and cannot be shown to be disjoint from it either.
```

With the evidence the same select is rejected where it is written:

```
Cannot prove that Tuple.Union[Tuple1[Orders.Column[("id" : String), Int]]] <:< Users.Column[?, ?].
```

**A column whose type does not say its name.** With `Column[A]` instead of
`Column[N, A]`, `name` and `email` are both `Column[String]`. Reading
`row(Users.email)` compiles, finds the position of the first `Column[String]`
in the select — `name`'s — and returns the name. No error, no warning: the
wrong value, at runtime.

**A column declared without its type.** `val label = column("label")` infers
`N` from the argument and has nothing to infer `A` from, so it is a
`Column["label", Nothing]` — and no value fits it:

```
Found:    ("x" : String)
Required: Nothing
```

Declare columns with their type, `val label: Column["label", String] =
column("label")`.

**A column that was not selected.** `row(Users.email)` on a select of `id` and
`name`:

```
No singleton value available for IndexOf[
  (Users.Column[("id" : String), Int], Users.Column[("name" : String), String])
    ,
Users.Column[("email" : String), String]]; eligible singleton types for `ValueOf` synthesis include literals and stable paths.
```

with an E184 warning beside it, *"Match type reduction failed since selector
EmptyTuple.type matches none of the cases"*. The error is right; it talks
about `ValueOf` because that is what was missing.

**Another table's column.** `where(Orders.id)(1)` on a `Users` query, or
`row(Orders.id)`:

```
Found:    (Orders.id : Orders.Column[("id" : String), Int])
Required: Users.Column[N, A]
```

The name and the value type agree; the prefix does not.

**A generic accessor warns on a correct call.** `def apply[C](column: C)(using
ValueOf[IndexOf[Cs, C]]): Tuple.Elem[Values[Cs], IndexOf[Cs, C]]` works, and
`val name: String = row(Users.name)` returns the right value — with a warning:

```
Match type reduction failed since selector EmptyTuple.type
matches none of the cases

    case Nothing *: _ => (0 : Int)
    case _ *: rest => S[IndexOf[rest, Nothing]]
```

The expected type makes the compiler reduce the result type before `C` is
inferred, with `C` still at `Nothing`. Taking a `Column[N, A]` and returning
its `A` fixes `N` and `A` from the argument first.

## Exercises

| # | asks for |
| --- | --- |
| 01 | `Values`, the type of a select's rows, computed from its columns |
| 02 | a `select` and a `where` that take only their own table's columns, and a `where` value of the column's type |
| 03 | a row read by column, which compiles only for a column that was selected and returns that column's type |

```bash
sbt "exercises/testOnly typeprog.ch23typedquerybuilder.*"
```

## Further reading

Every link below was resolving when the chapter was tagged.

- [Slick, *Queries*](https://scala-slick.org/doc/stable/queries.html) — a
  production query library for Scala in the same spirit: its "lifted
  embedding" types a column as `Rep[T]` and a query's result from its
  columns
- [ZIO Quill](https://zio.dev/zio-quill/) — the other road: queries written as
  ordinary Scala and turned into SQL at compile time, by a macro (chapter 12)
- [jOOQ, *Column expressions*](https://www.jooq.org/doc/latest/manual/sql-building/column-expressions/) —
  the same idea in Java, with columns generated from the database schema
- [`scala.Tuple`](https://github.com/scala/scala3/blob/main/library/src/scala/Tuple.scala) —
  `Map`, `Union` and `Elem`, each a short match type
