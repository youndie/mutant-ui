---
id: B-184
title: "The bottom sheet scrolls its content and hands a pull at the top to its drag"
status: open
priority: P2
size: M
stage: stage-6-later
blocked_by: [B-183]
---

# B-184 — The bottom sheet scrolls its content and hands a pull at the top to its drag

The bottom sheet does not scroll its content; B-178 left its content running off the screen's foot (F-bottom-sheet-05, F-bottom-sheet-08). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 30 ([решение]).

- **The decision and its reason.** The sheet scrolls its content itself. With the content at its top, a downward gesture becomes the sheet's drag (B-183).
- The sheet scrolls through a nested-scroll connection rather than wrapping its content in `verticalScroll`, so a consumer's lazy list inside it still works; the item checks that.

- AC:
  - content taller than the sheet scrolls inside it, by touch, wheel and keyboard
  - at the top, a downward drag moves the sheet; elsewhere it scrolls the content
  - a `LazyColumn` inside the sheet scrolls and does not throw
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantBottomSheet.kt`
