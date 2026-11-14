---
id: B-56
title: "Navigation bar: throws in a horizontally scrolling row"
status: open
priority: P0
size: S
stage: stage-5-product
blocked_by: [B-40]
---

# B-56 — Navigation bar: throws in a horizontally scrolling row

Found in [B-40](B-40-qa-pass-and-real-window.md) (F01). Given an unbounded width (`horizontalScroll`, `LazyRow`, a catalogue row of phone-sized frames), `MutantNavigationBar` passes `Infinity` into `Constraints.fixed` and throws `IllegalArgumentException` at layout. It was the only throw in about 3 900 composed cases.

- **The decision.** Under an unbounded width the bar takes the width its items need: each cell at the widest of its pill, icon box and label, and the bar the sum of the cells. The rejected alternative is a `require` with a clear message: nothing a consumer's layout can do may throw at layout or draw time.
- AC: `ConstraintsSweepTest.F01` passes without its `@Ignore`; a mutant that restores `constraints.maxWidth` is killed.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationBar.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/ConstraintsSweepTest.kt`
