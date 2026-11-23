---
id: B-201
title: "The menu opens upward near the foot, scrolls when tall, and answers its chosen row"
status: wip
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-201 — The menu opens upward near the foot, scrolls when tall, and answers its chosen row

Near the window's foot the menu slides up over its anchor (Compose's popup clamping); a menu taller than the window loses its first rows and does not scroll (F-menu-06, F-menu-07). A press on the chosen row passes nothing to `onSelect`, so the demo's sort menu cannot be closed by picking the current order (F-menu-08). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 28 ([решение]) and 39 ([ошибка]).

- **The decision and its reason.** Near the foot the menu opens upward from its anchor. Taller than the window, it is limited to the window less 16 dp at the top and 34 dp at the foot (16 and the 18 dp overhang), and its content scrolls. A press on the chosen row calls `onSelect`; nothing pours, and the plate gets a heat impulse of 0.8.

- AC:
  - an anchor in the window's lower part opens the menu above it, never over the anchor
  - a menu of 30 rows in a 400 dp window stands 16 dp from the top and 34 dp from the foot and scrolls to its last row
  - a press on the chosen row calls `onSelect` with it, and the plate's heat reads 0.8 with no pour
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantMenu.kt`
