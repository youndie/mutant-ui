---
id: B-190
title: "A press under the pause shows a still frame at heat 1"
status: wip
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-190 — A press under the pause shows a still frame at heat 1

Under reduce motion a press shows nothing on buttons, the FAB, toggles and the refresh tongue (F-buttons-06, F-icon-fab-05, F-pull-to-refresh-03); the engine's paused mode does the same. [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 12 ([ошибка]).

- **The decision and its reason.** A press under the pause shows at once a still frame at heat 1: the fill mixed with amber, no bubbles and no tremble. It goes on release. This widens D7 (research D24).

- AC:
  - under reduce motion, pressed: the fill carries the amber mix of heat 1, and two frames 500 ms apart are identical; released: the rest picture on the next frame
  - without reduce motion the press is unchanged
  - each new test of the first criterion fails on the code before, and every new test kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at (rewritten 2026-11-23: the second criterion says nothing changes, so its guard passes on the code before by construction; it is held by the mutant that drops the pause from the rule)
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyHeat.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyDrawing.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/theme/MutantTheme.kt`
