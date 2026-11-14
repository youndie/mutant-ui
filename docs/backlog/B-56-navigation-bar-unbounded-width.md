---
id: B-56
title: "Navigation bar: throws in a horizontally scrolling row"
status: done
priority: P0
size: S
stage: stage-5-product
blocked_by: [B-40]
---

# B-56 — Navigation bar: throws in a horizontally scrolling row

Found in [B-40](B-40-qa-pass-and-real-window.md) (F01). Given an unbounded width (`horizontalScroll`, `LazyRow`, a catalogue row of phone-sized frames), `MutantNavigationBar` passes `Infinity` into `Constraints.fixed` and throws `IllegalArgumentException` at layout. It was the only throw in about 3 900 composed cases.

- **The decision.** Under an unbounded width each destination takes the design's cell, 82 dp (the page's 328 dp bar over four), and the bar is their sum; the pill's slide counts in that cell too. The rejected alternatives: a `require` with a clear message, since nothing a consumer's layout does may throw at layout or draw time; and the width of the widest item, first written here, which would measure the labels' text during composition for the slide's distance, while the cells are equal by design and their labels already ellipsize.
- AC: `ConstraintsSweepTest.F01` passes without its `@Ignore`; a mutant that restores `constraints.maxWidth` is killed.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationBar.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/ConstraintsSweepTest.kt`

## Result (2026-11-14)

`MutantNavigationBar` reads `hasBoundedWidth`: bounded, the cells divide the width as before (parity and goldens unchanged); unbounded, `DESIGN_CELL` per destination. `ConstraintsSweepTest` holds every subject in every place with no known exceptions left, and `F01` runs un-ignored. `NavigationBarBehaviourTest` holds the two-destination bar at 164 dp in a horizontally scrolling row, its second cell at 82–164, and the pill sliding from 41 to 123 under way at 160 ms. Mutants, each killed by the aimed test: the bar's width back to `constraints.maxWidth` (F01 throws), the cell at 80 dp, the slide's cell from `maxWidth`.
