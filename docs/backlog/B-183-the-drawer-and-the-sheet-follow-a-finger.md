---
id: B-183
title: "The drawer and the sheet follow a finger and a mouse"
status: wip
priority: P2
size: L
stage: stage-6-later
blocked_by: []
---

# B-183 — The drawer and the sheet follow a finger and a mouse

The navigation drawer and the bottom sheet cannot be dragged by touch, fling or mouse, though D15 names their dragging (F-drawer-09, F-bottom-sheet-07). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 5 ([решение]).

- **The decision and its reason.** The panel and the sheet follow the finger 1:1, unquantised. They close when dragged past 40 % of their size or flung faster than 800 dp/s. The settle takes 330 ms on the `slide` curve. The scrim is proportional to the position. Past the open position they drag with a resistance of 0.3, at most 24 dp. While dragged, the body boils.
- A mouse drag scrolls nothing in Compose, so the gesture reads the pointer itself (cmp-ui-component §4).
- Not covered: the sheet's own scrolling and its hand-over to the drag (B-184).

- AC:
  - by touch and by mouse: a drag moves the panel and the sheet by the pointer's distance on every frame; released past 40 % or above 800 dp/s they close, otherwise they settle back in 330 ms
  - the scrim's alpha is proportional to the position at every frame
  - dragged past open, they move 0.3 of the pointer, never past 24 dp
  - the body is hot while dragged
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationDrawer.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantBottomSheet.kt`
