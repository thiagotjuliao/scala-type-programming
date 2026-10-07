package typeprog.ch23typedquerybuilder

/** Chapter 23 — Typed query builder.
  *
  * Read this file top to bottom before opening the exercises. It is the whole
  * chapter in executable form: every claim below is one the compiler is
  * checking as you read it.
  *
  * The examples run beside the exercises rather than through them — a form
  * whose fields are asked for, instead of a table whose columns are selected
  * — so that the query builder is left for you to build. In order:
  *
  *   1. a field that knows its name and its type;
  *   2. the answers' type, computed from the fields asked for;
  *   3. a fact about a whole tuple, checked where the call is written;
  *   4. a number computed by the compiler, handed to the runtime.
  *
  * Prose version, with the motivation and the pitfalls:
  * `docs/theory/ch23-typed-query-builder.md`.
  *
  * (Messages quoted below drop the `typeprog.ch23typedquerybuilder.Walkthrough.`
  * prefix the compiler prints in front of every name.)
  */
object Walkthrough:

  // ---------------------------------------------------------------------------
  // 1. A field that knows its name and its type
  //
  // `N` is the field's name as a literal type, `A` the type of its answer.
  // No `A` is stored: it is a phantom, there for the compiler. The field is
  // declared inside `Form`, so each form has its own `Field` (chapter 03), and
  // its constructor is the form's alone.
  // ---------------------------------------------------------------------------

  abstract class Form:

    final class Field[N <: String & Singleton, A] private[Form] (val label: N)

    protected def field[N <: String & Singleton, A](label: N): Field[N, A] = Field(label)

    // -------------------------------------------------------------------------
    // 2. The answers' type, computed from the fields
    //
    // One match type for one field, binding its answer type; `Tuple.Map`
    // applies it to every element of a tuple of fields (chapter 09).
    // -------------------------------------------------------------------------

    type Answer[F] = F match
      case Field[n, a] => a

    type Answers[Fs <: Tuple] = Tuple.Map[Fs, Answer]

    // -------------------------------------------------------------------------
    // 3. A fact about a whole tuple
    //
    // `ask` takes a tuple of fields, and only this form's. No parameter type
    // says "a tuple of my fields", so the fact is demanded as evidence: the
    // union of the element types conforms to this form's `Field[?, ?]`.
    // -------------------------------------------------------------------------

    def ask[Fs <: Tuple](fields: Fs)(using Tuple.Union[Fs] <:< Field[?, ?]): Fs = fields

    // The same method without the evidence. Its result for another form's
    // field is stuck, and a stuck result is not an error.
    def askAnything[Fs <: Tuple](fields: Fs): Fs = fields
  end Form

  object Signup extends Form:
    val name: Field["name", String] = field("name")
    val age: Field["age", Int] = field("age")

  object Survey extends Form:
    val age: Field["age", Int] = field("age")

  val answers = summon[
    Signup.Answers[(Signup.Field["name", String], Signup.Field["age", Int])] =:= (String, Int)
  ]

  val asked: (Signup.Field["name", String], Signup.Field["age", Int]) =
    Signup.ask((Signup.name, Signup.age))

  // `Signup.ask(Tuple1(Survey.age))` does not compile — same label, same type,
  // another form:
  //
  //   Cannot prove that Tuple.Union[Tuple1[Survey.Field[("age" : String), Int]]] <:< Signup.Field[?, ?].
  //
  // Without the evidence it does, and nothing complains until an answer of
  // that type is needed: `Signup.Answers` of it does not reduce.

  val loose: Tuple1[Survey.Field["age", Int]] = Signup.askAnything(Tuple1(Survey.age))

  // ---------------------------------------------------------------------------
  // 4. A number computed by the compiler, handed to the runtime
  //
  // `Tuple.Size` of a tuple type is a literal type, and `ValueOf` of a literal
  // type (chapter 04) is that literal as a value. Nothing is counted at
  // runtime: the compiler did it, and wrote the answer in.
  // ---------------------------------------------------------------------------

  def size[Fs <: Tuple](fields: Fs)(using n: ValueOf[Tuple.Size[Fs]]): Int = n.value

  val two: Int = size((Signup.name, Signup.age))

  // A stuck type is no literal, so there is no `ValueOf` for it — which is how
  // a computed position rejects a column that is not there.
end Walkthrough
