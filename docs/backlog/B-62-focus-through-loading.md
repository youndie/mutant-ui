---
id: B-62
title: "Button: keyboard focus survives Loading and Success"
status: done
priority: P2
size: S
stage: stage-5-product
blocked_by: [B-40]
---

# B-62 — Button: keyboard focus survives Loading and Success

Found in [B-40](B-40-qa-pass-and-real-window.md) (F11). Loading disables the `clickable`, so a keyboard user who presses Enter on a button loses focus, and after Success the next Tab starts again from the top of the window. In a long form that throws the user back to the start.

- **The decision.** Loading keeps the button focusable and ignores clicks while it lasts; semantics say it is busy. The rejected alternative is moving focus to the next control, which the user did not ask for.
- AC: `KeyboardFocusSweepTest.F11` passes without its `@Ignore`; Loading still takes no click (the existing button tests).
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantButton.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/KeyboardFocusSweepTest.kt`

## Findings (2026-11-15)

- **Fixed.** Loading keeps the button's `clickable` enabled, so keyboard focus stays on it through the work and its Success. Its click is ignored while it loads. For a reader, its semantics carry `disabled()` beside the indeterminate progress: busy, it cannot be pressed now. Focus does not follow semantics, so it stays.
- **Tests.** `KeyboardFocusSweepTest.F11` runs, its `@Ignore` gone. `ButtonBehaviourTest`'s "a loading button takes no click" holds unchanged.
- **Mutants.** Killed:
  - Loading turning the clickable off again (by F11);
  - a click taken while loading (by the button's own test).
