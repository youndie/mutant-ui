---
id: B-180
title: "A new screen tunes in as the switch starts"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: [B-179]
---

# B-180 — A new screen tunes in as the switch starts

The screen's content swaps in one frame while the navigation pill, the tabs' bar or a chip moves for 330 ms (F-screens-08, F-app-bar-tabs-04, F-app-bar-tabs-07). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 2 ([решение]).

- **The decision and its reason.** The old content goes in one frame; the new tunes in (B-179, 320 ms) starting on the same frame as the pill, the bar or the chip. No slide and no cross-dissolve.
- The kit offers the host (a content slot keyed by the screen), so a consumer gets the same swap as the demo; the sample's screens use it. Rejected: building it in the sample only, which leaves every consumer the one-frame cut.
- Rejected: the brief's `switch` (a 200 ms vertical tear and a noise band), which the answer replaces.

- AC:
  - in the demo, switching a tab, a navigation destination or a filter chip: the old screen's last frame is the one before the switch, and the new screen's tune-in begins on the frame the pill, bar or chip starts to move
  - nothing slides and no frame mixes the two screens
  - under reduce motion the finished frame shows at once (research D24)
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyTransitions.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationBar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTabs.kt`, `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/DemoApp.kt`
