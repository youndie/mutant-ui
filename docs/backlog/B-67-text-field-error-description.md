---
id: B-67
title: "Text field: the error has no words for a screen reader"
status: done
priority: P3
size: S
stage: stage-5-product
blocked_by: [B-40]
---

# B-67 — Text field: the error has no words for a screen reader

Found in [B-40](B-40-qa-pass-and-real-window.md) (F18), by reading the code. `semantics { if (isError) error("") }`: a reader announces an error with no words, and the field has no way to say what is wrong.

- **The decision.** A parameter carries the error's description; without one the theme's generic word is used. The rejected alternative is an empty description, as now.
- AC: `SemanticsProperties.Error` reads the description, or the generic word.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTextField.kt`

## Findings (2026-11-15)

**Fixed.** `MutantTextField` gained `errorDescription`, the last parameter so existing calls keep their meaning. An error field's semantics carry it as `SemanticsProperties.Error`. Without one the generic `Ошибка` stands in: the kit has no strings in its theme, so the default is the parameter's, as `MutantSearchBar`'s `clearLabel` is.

**Test.** `TextFieldBehaviourTest` reads the error:
- the description when one is given;
- `Ошибка` when none is;
- none at all on a field without an error.

**Mutants.** Both killed by the test: the empty description, and the description not handed down to the body.

**ABI.** One parameter added to `MutantTextField`. Its JVM signature changes, so the ABI dump was regenerated (the library is before 1.0).
