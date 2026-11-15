---
id: B-66
title: "Navigation bar: the pill overruns narrow cells"
status: wip
priority: P3
size: S
stage: stage-5-product
blocked_by: [B-40]
---

# B-66 — Navigation bar: the pill overruns narrow cells

Found in [B-40](B-40-qa-pass-and-real-window.md) (F16). The pill and the icon box are fixed at 56 × 30 dp; with 12 destinations in 360 dp a cell is 30 dp, and the pill reaches 13 dp into each neighbour.

- **The decision.** The pill is never wider than its cell. The rejected alternative is a maximum item count, which the API cannot enforce kindly.
- AC: a test with 12 destinations in 360 dp reads the pill inside its cell; the bar's parity cells unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationBar.kt`
