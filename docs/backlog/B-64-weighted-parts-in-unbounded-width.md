---
id: B-64
title: "Unbounded width: weighted parts collapse to nothing"
status: wip
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
