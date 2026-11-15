---
id: B-64
title: "Unbounded width: weighted parts collapse to nothing"
status: done
priority: P3
size: S
stage: stage-5-product
blocked_by: [B-40]
---

# B-64 — Unbounded width: weighted parts collapse to nothing

Found in [B-40](B-40-qa-pass-and-real-window.md) (F14). In a horizontally scrolling row the `weight(1f)` children get 0: the list item's headline and supporting line, the top app bar's title and the linear progress track vanish. Nothing throws.

- **The decision.** Under an unbounded width a weighted part takes its intrinsic width (the track a minimum stated here). The rejected alternative is leaving it to the consumer's `width(...)`: the parts vanish silently.
- AC: `OverlayAndMouseTest.F14` passes without its `@Ignore`.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantListItem.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTopAppBar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantProgress.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/OverlayAndMouseTest.kt`

## Findings (2026-11-15)

**Fixed.** An internal modifier, `ownWidthWhenUnbounded`, measures a component at its maximum intrinsic width when its width is unbounded, and changes nothing under a bounded width. Weighted parts then take their own widths instead of nothing. It sits on:
- the list item's row, which also carries `MutantSearchResult`;
- the top app bar;
- the linear progress.

The progress track has no width of its own, so it takes 200 dp at least (`TRACK_MIN`: a third of the page's 600 dp column, stated here as the item asked). Under a bounded width the weight still sets the track exactly.

**Tests.** `OverlayAndMouseTest.F14` passes without its `@Ignore`: the headline, the title and a progress track over 100 dp, each in a horizontally scrolling row. The list item, top app bar, progress and search parity tests are unchanged, and so are the QA sweeps.

**Mutants.** All four killed by F14: the modifier dropped from the list item, from the bar and from the progress, and the track's minimum dropped.
