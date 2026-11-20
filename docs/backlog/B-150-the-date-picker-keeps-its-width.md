---
id: B-150
title: "The date picker keeps its width"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-150 — The date picker keeps its width

The owner, 2026-11-20, on the site after B-148 and B-149: «календарь уехал по ширине в components». On the Components
page's Selection and input group the calendar filled the row's width: its body stretched, the month and the arrows
went to the far right, the days stayed 7 × 36 dp at the left.

**Why**: `MutantDatePicker`'s header row was `fillMaxWidth()`. In a column sized by its content, a child filling the
width takes all the width offered, and the column, the body around it, took it too. Its fixtures and parity tests give
it exactly the design's 284 dp, so none of them could see it; the specimen on the page (a `Cell` with no width) offers
the whole row. The specimen did not change in B-148: it stretched before as well.

- **The change**: the calendar's column is as wide as its widest row's own width (`IntrinsicSize.Max`): the days, or a
  wider header under a large font. The header fills that, not the width offered.
- AC:
  - a test, failing before (600 dp wide where it should be 284): offered 600 dp, the calendar is 284 dp wide and its
    next-month arrow stands over its days;
  - the date picker's parity and goldens unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDatePicker.kt`

## Findings

### 2026-11-20

- `DateTimePickerBehaviourTest`, `given more room than it needs, the calendar keeps its width and its header over its
  days`: failed on the code before (`the calendar's width ==> expected: <284.0> but was: <600.0>`), green with the
  change (`91900b3`), with the picker's other behaviour and parity tests (12).
