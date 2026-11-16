---
id: B-93
title: "The calendar's selection jumps to the new day"
status: done
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

## Findings (2026-11-16)

**Seen.** A frame strip of picking the 18th after the 5th: the drop stood on the 5th, then on the 18th, with nothing between.

**The decision.** The navigation bar's slide, given a vertical part (`SlideRequest.dy`, `slid`):
- 330 ms, eased, stepped at 12 a second, as the bar's;
- stretched along its run, so a run more down than across stretches tall.

The picker follows the chosen day from composition (`DayRun`) and gives the drop the way back from the new cell to the old in both axes. A new month resets it, so the drop lands without a run.

A first try turned the numbers when the drop's run was half done. The strip showed the 5th dark on dark once the first step had carried the drop off it, and the 12th light on lime as it passed. A number is now dark while the drop's middle stands within half a cell of it, so the day it leaves, each day it crosses and the day it reaches are each dark only while covered.

Every number here the design does not state is the bar's, already chosen in B-35. The run itself is a departure (D20) that moves no cell: the references are at rest.

**Tests.** `CalendarRunTest`, from the 5th to the 18th:
- the drop's lime stands between the two days on both axes on at least two frames, and lands on the 18th;
- the 5th, the 12th it crosses and the 18th read at least 3:1 on every frame;
- under reduce motion it is on the 18th from the first frame.

Three mutants, all killed:
- no `dy` from the picker;
- the slide drawn without its vertical part;
- the numbers turned at the click.
