# Glossary

One line each, in the sense the repository uses them. The chapter column is
where the term is introduced, not everywhere it appears.

| term | meaning | ch |
| --- | --- | --- |
| **subtyping** (`A <: B`) | permission to supply an `A` where a `B` is expected | 01 |
| **covariance** (`+A`) | `F[Dog] <: F[Animal]`; safe when `A` is only produced | 01 |
| **contravariance** (`-A`) | `F[Animal] <: F[Dog]`; safe when `A` is only consumed | 01 |
| **invariance** (`A`) | no relation between `F[Dog]` and `F[Animal]` | 01 |
| **upper bound** (`A <: T`) | `A` may be anything conforming to `T` | 01 |
| **lower bound** (`B >: A`) | `B` may be anything `A` conforms to; the repair for covariant puts | 01 |
| **type member** | a type declared in a trait's body rather than its parameter list | 01 |
| **refinement** | `Trait { type M = C }`, a subtype with a member made concrete | 01 |
| **evidence** (`=:=`, `<:<`) | a relation between types turned into a summonable value | 01 |
| **kind** | the "type of a type": `Int` is `*`, `List` is `* -> *` | 02 |
| **higher-kinded type** | a parameter that is itself a type constructor, `F[_]` | 02 |
| **type lambda** | an anonymous type constructor, `[X] =>> Either[String, X]` | 02 |
| **path-dependent type** | a type member selected through a value, `repo.Id` | 03 |
| **dependent function type** | a function whose result type depends on its argument's value | 03 |
| **literal type** | a single value promoted to a type, `42`, `"id"` | 04 |
| **singleton type** | the type inhabited only by one value, `obj.type` | 04 |
| **intersection / union** | `A & B` requires both, `A \| B` accepts either | 04 |
| **opaque type** | an alias visible only inside its scope; a wrapper with no runtime cost | 05 |
| **phantom type** | a type parameter no value ever inhabits, carrying a fact | 05 |
| **type class** | behaviour attached to a type from outside it, via a `given` | 06 |
| **given / using** | the instance, and the place it is demanded | 06 |
| **context bound** (`[A: Show]`) | shorthand for a `using` parameter of type `Show[A]` | 06 |
| **implicit scope** | the companions of a type and of its parts, searched when the lexical scope finds no given | 06 |
| **low-priority trait** | a parent trait of a companion, holding the givens that should lose a tie | 06 |
| **match type** | a type-level `match`, reduced by the compiler | 07 |
| **reduction** | the compiler evaluating a match type to a concrete type | 07 |
| **disjoint** | two types no value can belong to both of; what lets a match type move past a case | 07 |
| **stuck** match type | one left unreduced: a case neither matches nor can be proved disjoint | 07 |
| **`inline`** | a definition expanded at the call site before type-checking finishes | 08 |
| **`constValue`** | reading a literal type back as a value at compile time | 08 |
| **`erasedValue`** | a value that exists only for its type; never evaluated | 08 |
| **`transparent inline`** | an inline method whose call has the type of its expansion, not of its declaration | 08 |
| **`summonFrom`** | a match whose cases are instance searches, tried in order at the expansion | 08 |
| **Peano numbers** | naturals encoded as `Zero` / `Succ[N]`, arithmetic by recursion | 09 |
| **HList** | a heterogeneous list; in Scala 3 an ordinary `Tuple` | 09 |
| **`compiletime.ops`** | arithmetic and comparisons on literal types, evaluated when the arguments are literals | 09 |
| **`Mirror`** | the compiler-synthesised description of a case class or enum | 10 |
| **derivation** | building a type class instance from that description | 10 |
| **`NotGiven`** | evidence that no instance exists — negation in implicit search | 11 |
| **quote / splice** (`'{ }`, `${ }`) | moving between code as value and code as expression | 12 |
| **`Expr` / `TypeRepr`** | a typed expression, and the compiler's view of a type | 12 |
