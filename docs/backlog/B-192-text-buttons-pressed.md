---
id: B-192
title: "The text button and the snackbar action show a press"
status: wip
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-192 — The text button and the snackbar action show a press

The text button and the snackbar's action have no pressed look (F-buttons-05, F-snackbar-04). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 15 ([решение]).

- **The decision and its reason.** Pressed, the label drops 1 dp and a solid 2 dp line in the label's colour stands under it; on the text button it replaces the tracking divider. Release is one frame.

- AC:
  - pressed: the label's ink 1 dp lower and a 2 dp line in its colour under it, no tracking divider; released: the rest picture on the next frame
  - under reduce motion the same, at once
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantButton.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSnackbar.kt`
