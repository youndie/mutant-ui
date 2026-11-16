---
id: B-94
title: "The navigation bar's selected label stands on its pill's drips"
status: open
priority: P1
size: S
stage: stage-6-later
---

# B-94 — The navigation bar's selected label stands on its pill's drips

The owner, 2026-11-16: «в navbar выбранный элемент имеет белый цвет текста — на зелёном не читается».

The selected item's label is `text` (white in «Эфир») and stands 3 dp under the lime pill. In the demo the pill's drips hang over that gap onto the label: a look at the Pages build (`4403600`) shows «On air» on green.

- **The decision and its reason.** To be decided when taken, after a look at every frame of a slide. Candidates:
  - the selected label in `onPrimary` where the pill's ink is under it;
  - the pill's drips kept off the label, as the design's `gap` intends (2 px).

  Either is a departure (D20).
- AC:
  - The selected label reads at least 4.5:1 against what is under it on every frame, at rest and through a slide, in both themes.
  - A test reads the label's pixels.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationBar.kt`
