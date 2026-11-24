---
id: B-181
title: "A body loses its signal in three steps and tunes in when enabled"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: [B-179]
---

# B-181 — A body loses its signal in three steps and tunes in when enabled

Enabled ↔ disabled is a one-frame cut on every body: the drips shorten or lengthen at once (F-icon-fab-04, F-text-field-08, F-search-06). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 3 ([решение]).

- **The decision and its reason.** Disabling takes 300 ms in three steps: a tear band runs over the body, the fill desaturates in steps, and on the third step the slime is replaced by «нет сигнала» (`BodyState.Disabled`, the handoff's README line 83). Enabling is `tune-in` (B-179). Focus in and out stay instant: catching the signal reads as a click.
- The tear band is not drawn by the design: its height and travel are chosen here from the disabled look's own band (14 % of the height, shifted 4–7 dp) and listed in this item when built (D15).
- Rejected: a cross-fade (research D24).

- AC:
  - a frame strip of a button disabled at 0 ms shows three distinct pictures at the steps and «нет сигнала» from 300 ms; enabling shows B-179's steps
  - focus in and out still change in one frame
  - under reduce motion the finished frame shows at once (research D24)
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyStates.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyTransitions.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`
