---
id: B-205
title: "Every layout mirrored for right to left"
status: open
priority: P2
size: L
stage: stage-6-later
blocked_by: []
---

# B-205 — Every layout mirrored for right to left

B-65 mirrored the tabs and the navigation bar only: the segmented button does not mirror and its arrows are not mapped, the tabs' echo stays right of the bar, the date picker's month arrows keep their left-to-right look; a left-to-right title in a right-to-left layout is cut at its end with the ellipsis drawn at its start (F-segmented-06, F-app-bar-tabs-05, F-pickers-10, F-data-table-06). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 37 ([решение]; the ellipsis [ошибка]).

- **The decision and its reason.** Every component's layout mirrors: the segments, the tabs' echo (its tail points back along the reading direction), the month arrows, the switch's thumb, the slider, the progress, the drawer's side. What does not mirror: the slime's shape and drips, the highlight (the light is always top-left), the offset shadow (4, 4), on-screen digits and time, the Tape, the reel. A left-to-right title's ellipsis follows the text's own direction, at its end.

- AC:
  - a sweep over every public component under `LayoutDirection.Rtl` finds each layout mirrored and the listed parts unmirrored, by `positionInRoot` and pixels
  - arrow keys move the segmented button and the pickers in reading order
  - a left-to-right title in a right-to-left bar ends in its ellipsis on the right
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSegmentedButton.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTabs.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDatePicker.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSwitch.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSlider.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantProgress.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationDrawer.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTopAppBar.kt`
