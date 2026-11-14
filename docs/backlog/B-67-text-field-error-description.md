---
id: B-67
title: "Text field: the error has no words for a screen reader"
status: open
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
