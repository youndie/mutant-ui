---
id: B-66
title: "Navigation bar: the pill overruns narrow cells"
status: done
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

## Findings (2026-11-15)

**Fixed.** The pill is measured no wider than the chosen destination's cell. At the page's four destinations the cell is far wider than 56 dp and nothing changes; with twelve in 360 dp the pill is 30 dp and stays in its cell.

**Tests.**
- `NavigationBarBehaviourTest` gained a test: twelve destinations in 360 dp, the sixth chosen, and every acid pixel of the pill lies within that cell (150–179).
- The navigation bar's parity cells are unchanged, as are its behaviour tests and the QA sweeps.

**Mutant.** The pill measured unbounded again is killed by the new test.
