---
id: B-185
title: "The dialog rises opaque and keeps a margin when tall"
status: wip
priority: P1
size: M
stage: stage-6-later
blocked_by: [B-179]
---

# B-185 — The dialog rises opaque and keeps a margin when tall

The dialog's window fades with its scrim as one alpha (B-28), so for 200 ms the screen's words read through its text (F-dialog-05). A dialog taller than the screen stands flush, its outline clipped and its last line cut (F-dialog-07). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 7 ([ошибка]) and 29 ([решение]).

- **The decision and its reason.** The window is opaque from its first frame and enters by `tune-in` (B-179). The scrim keeps its 200 ms. A tall dialog stands 24 dp from the window's edges; its title and buttons are pinned and its content scrolls.

- AC:
  - no frame of the opening shows the screen through the window's pixels; the window follows B-179's steps while the scrim takes 200 ms
  - a dialog taller than the window stands 24 dp from every edge, its title and buttons in view, its content scrolling
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDialog.kt`
