---
id: B-196
title: "The pickers: today, disabled days, six rows, the clock's accent"
status: wip
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-196 — The pickers: today, disabled days, six rows, the clock's accent

The pickers have no today mark and no disabled days (F-pickers-08); the calendar's height changes with the month, 4–6 rows, and its body regenerates (F-pickers-06); the time picker's acid stays on the hours while Up and Down set the focused minutes (F-pickers-07). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 21 and 32 ([решение]) and 40 ([ошибка], the clock).

- **The decision and its reason.** Today wears four corner brackets in the `osd` colour around its number, as a focused field does. A disabled day is drawn in `dis`, without a body, and cannot be pressed. The calendar always has six rows, so its body never changes height with the month. The clock's accent follows the field being edited.
- Typed digits stay out: the designer deferred them.

- AC:
  - today shows four brackets in `osd`; a disabled day draws no body, ignores a click and Enter, and its semantics say it is disabled
  - every month of 2026 lays the calendar out at the same height
  - editing the minutes by Up and Down moves the accent to the minutes
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDatePicker.kt`
