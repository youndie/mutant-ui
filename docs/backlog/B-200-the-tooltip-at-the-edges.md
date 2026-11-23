---
id: B-200
title: "The tooltip stays in the window and wraps"
status: open
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-200 — The tooltip stays in the window and wraps

The tooltip's plate is cut at the window's edges, and wholly above the window for an anchor in its top 58 dp (F-tooltip-02); it never wraps, so a long hint runs off the window (F-tooltip-03). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 26 and 27 ([решение]).

- **The decision and its reason.** The plate shifts along the edge, 8 dp from the window, still anchored to its target; it flips above or below only when it does not fit vertically. It is at most 240 dp wide, up to three lines (30, 44 and 58 dp high), then an ellipsis.

- AC:
  - for anchors along every edge of the window, the plate stands 8 dp or more inside it and its arrow still points at the anchor
  - an anchor in the top 58 dp shows the plate below it
  - a long hint wraps at 240 dp to at most three lines at those heights and ends in an ellipsis
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTooltip.kt`
