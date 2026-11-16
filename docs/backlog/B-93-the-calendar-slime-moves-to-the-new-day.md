---
id: B-93
title: "The calendar's selection jumps to the new day"
status: open
priority: P2
size: M
stage: stage-6-later
---

# B-93 — The calendar's selection jumps to the new day

The owner, 2026-11-16: «в календаре надо чтобы слизь с анимацией дату меняла (как bottom bar)».

The date picker's selection is one body placed at the selected cell, and a new day moves it there at once. The navigation bar's pill slides to a new item (`SlideRequest`, `BodyMoves`, `slid`: 330 ms, eased, stretched), but on the x axis only. A day in another row needs y too.

- **The decision and its reason.** To be decided when taken. Candidates:
  - the slide given a `dy`;
  - the segmented button's `VenomRun`, which already runs in two dimensions.

  The selected number's colour follows the body, not the click. A change of month starts the body at the new day without a run.
- AC:
  - Picking another day in the same month moves the body there over the slide's time, through both axes. A frame strip shows it.
  - Under reduce motion it moves at once.
  - The number stays readable on every frame.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDatePicker.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyTransitions.kt`
