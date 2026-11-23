---
id: B-198
title: "The keyframe loss ends on a screen change and takes the dividers"
status: wip
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-198 — The keyframe loss ends on a screen change and takes the dividers

The keyframe loss keeps dragging the picture it took when it started after the screen under it changed — after a tab switch, a chosen chip without its body for 1.08 s — and leaves the dividers out of its picture (F-screens-01, F-foundations-05, F-list-divider-badge-02). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 24 ([решение]; the dividers [ошибка]).

- **The decision and its reason.** On a screen change the loss ends at once, and no new one starts for 1 s after it. The dividers are in its picture.
- The kit learns of a screen change from the screen host (B-180) or a key the consumer passes; the item decides which. A scroll is not a screen change, and the answer does not settle it: it stays as it is, and says so.

- AC:
  - a tab switch during a loss shows the new screen whole on the next frame, and no loss begins within 1 s
  - a loss over a list shows its dividers in the dragged picture
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/KeyframeLoss.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantDivider.kt`
