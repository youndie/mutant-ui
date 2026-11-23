---
id: B-187
title: "Tab labels hand the accent over late; a narrow top bar drops its status first"
status: open
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-187 — Tab labels hand the accent over late; a narrow top bar drops its status first

The tabs' labels turn in the click's first frame while the bar takes 330 ms (F-app-bar-tabs-04); a top bar under about 170 dp gives its title zero width and pushes its action out (F-app-bar-tabs-06). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 9 and 31 ([решение]).

- **The decision and its reason.** The old label loses its accent in the first frame, the new takes it after 280 ms, as the menu and the segmented button do. In a narrow top bar the status on the right (`CH 03 · SP`) goes first, then the title is cut with an ellipsis; the navigation icon stays.

- AC:
  - after a click the new tab's label shows its accent from 280 ms, the old none from the first frame
  - at widths from 360 down to 120 dp: the navigation icon and the action in full, the status dropped before the title is cut, the title ellipsised, no node out of the bar
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTabs.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTopAppBar.kt`
