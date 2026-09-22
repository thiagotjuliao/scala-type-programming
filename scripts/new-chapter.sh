#!/usr/bin/env bash
# Scaffolds a chapter: the theory document, the walkthrough, and a matching
# exercise / solution / spec set on both sides of the repository.
#
#   usage: ./scripts/new-chapter.sh <number> "<title>" [options]
#          ./scripts/new-chapter.sh <number> --add [-n N]
#
#   -n, --exercises N  how many exercises to scaffold (default 3)
#   --add              add exercises to a chapter that already exists
#   --no-doc           skip the theory document
#
# Nothing here is ever overwritten: a file that already exists is reported and
# left alone, so the script is safe to re-run.
set -euo pipefail

die() { echo "error: $*" >&2; exit 1; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

NUMBER="${1:-}"
[[ -n "$NUMBER" ]] || die 'usage: new-chapter.sh <number> "<title>" [-n N] [--add] [--no-doc]'
shift

TITLE=""; COUNT=3; ADD=0; WANT_DOC=1
# A bare second argument is the title. Everything after it is a flag.
if [[ $# -gt 0 && "$1" != -* ]]; then TITLE="$1"; shift; fi
while [[ $# -gt 0 ]]; do
  case "$1" in
    -n|--exercises) COUNT="${2:?-n needs a count}"; shift ;;
    --add)          ADD=1 ;;
    --no-doc)       WANT_DOC=0 ;;
    *) die "unknown flag: $1" ;;
  esac
  shift
done

NN="$(printf '%02d' "$((10#$NUMBER))")"
TEMPLATES="docs/templates"

# --- locate or derive the chapter's identity ----------------------------------
#
# A chapter is identified by its number alone. The slug and the package name are
# derived from the title the first time and read back off disk afterwards, so
# `--add` never has to be told the title again.
EXISTING_PKG="$(find modules/exercises/src/main/scala/typeprog -maxdepth 1 -type d -name "ch${NN}*" 2>/dev/null | head -1)"
EXISTING_DOC="$(find docs/theory -maxdepth 1 -name "ch${NN}-*.md" 2>/dev/null | head -1)"

if [[ -n "$EXISTING_PKG" ]]; then
  PKG="$(basename "$EXISTING_PKG")"
  SLUG="${EXISTING_DOC##*/ch${NN}-}"; SLUG="${SLUG%.md}"
  [[ $ADD -eq 1 ]] || echo "chapter $NN already exists ($PKG) — adding to it"
else
  [[ -n "$TITLE" ]] || die "chapter $NN does not exist yet, so it needs a title"
  SLUG="$(printf '%s' "$TITLE" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/-/g; s/^-+//; s/-+$//')"
  PKG="ch${NN}$(printf '%s' "$SLUG" | tr -d '-')"
fi

# The title is only needed when something is being rendered from a template.
if [[ -z "$TITLE" && -n "$EXISTING_DOC" ]]; then
  TITLE="$(sed -n '1s/^# Chapter [0-9]* — //p' "$EXISTING_DOC")"
fi
[[ -n "$TITLE" ]] || TITLE="$SLUG"

EX_MAIN="modules/exercises/src/main/scala/typeprog/$PKG"
EX_TEST="modules/exercises/src/test/scala/typeprog/$PKG"
SO_MAIN="modules/solutions/src/main/scala/typeprog/$PKG"
SO_TEST="modules/solutions/src/test/scala/typeprog/$PKG"
mkdir -p "$EX_MAIN" "$EX_TEST" "$SO_MAIN" "$SO_TEST" docs/theory

# --- rendering ----------------------------------------------------------------

render() { # <template> <destination> [exercise-number]
  local tmpl="$1" out="$2" ee="${3:-}"
  if [[ -e "$out" ]]; then
    echo "  kept:    $out"
    return 0
  fi
  sed -e "s|{{NN}}|$NN|g" \
      -e "s|{{TITLE}}|$TITLE|g" \
      -e "s|{{SLUG}}|$SLUG|g" \
      -e "s|{{PACKAGE}}|$PKG|g" \
      -e "s|{{EE}}|$ee|g" \
      "$tmpl" > "$out"
  echo "  created: $out"
}

echo "chapter $NN — $TITLE  (package typeprog.$PKG)"

[[ $WANT_DOC -eq 1 ]] && render "$TEMPLATES/chapter.md" "docs/theory/ch${NN}-${SLUG}.md"
render "$TEMPLATES/Walkthrough.scala.tmpl" "$EX_MAIN/Walkthrough.scala"

# Exercise numbering continues after whatever is already there, so `--add`
# never collides with an exercise that has been written.
HIGHEST=0
for f in "$EX_MAIN"/Exercise*.scala; do
  [[ -e "$f" ]] || continue
  n="$(basename "$f" .scala)"; n="${n#Exercise}"
  (( 10#$n > HIGHEST )) && HIGHEST=$((10#$n))
done

for ((i = 1; i <= COUNT; i++)); do
  EE="$(printf '%02d' "$((HIGHEST + i))")"
  render "$TEMPLATES/Exercise.scala.tmpl"         "$EX_MAIN/Exercise${EE}.scala"     "$EE"
  render "$TEMPLATES/ExerciseSpec.scala.tmpl"     "$EX_TEST/Exercise${EE}Spec.scala" "$EE"
  render "$TEMPLATES/ExerciseSolution.scala.tmpl" "$SO_MAIN/Exercise${EE}.scala"     "$EE"
  render "$TEMPLATES/ExerciseSpec.scala.tmpl"     "$SO_TEST/Exercise${EE}Spec.scala" "$EE"
done

cat <<EOF

The spec is the contract: write it first, from the theory document, and let it
fail. Then the stub, then the solution.

  sbt "exercises/testOnly typeprog.$PKG.*"
      what is left to solve
  sbt verify
      what the tag will be held to
  ./scripts/finish-chapter.sh $NN
      close it
EOF
