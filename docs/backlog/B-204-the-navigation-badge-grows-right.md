---
id: B-204
title: "The navigation badge grows right and stops at 99+"
status: wip
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-204 — The navigation badge grows right and stops at 99+

The navigation badge grows left from its right edge: «12» covers half the icon, and in 30 dp cells it reaches the next (F-nav-bar-04). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 36 ([ошибка]).

- **The decision and its reason.** The badge grows rightwards from the icon's top-right corner, and caps at «99+».

- AC:
  - badges of 1, 12 and 99+ start at the icon's top-right corner and grow right; none covers the icon's ink
  - a count above 99 reads «99+»
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationBar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantBadge.kt`
