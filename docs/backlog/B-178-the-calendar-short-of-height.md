---
id: B-178
title: "The calendar given less room than it wants keeps its size"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-178 — The calendar given less room than it wants keeps its size

[B-168](B-168-look-and-motion-pass.md)'s pass found it in the bottom sheet's group (F-bottom-sheet-09,
[report](../../logs/b-168/bottom-sheet.md)): once the sheet gives its content at most the screen's height
(F-bottom-sheet-05), a short screen squeezes `MutantDatePicker`, and its first week of days rises into the weekday
names, «1» over «ВС». The demo's timer sheet fits its 720 dp phone and does not show it; a consumer's shorter screen,
a landscape phone or a calendar in a fixed-height pane does.

**Why**: the calendar wants 284 × 307 dp (November 2026, six weeks). Its day grid is a `Layout` that reports its
whole size whatever it is offered; in a shorter slot the column around it gives it only what is left, Compose
coerces the grid's reported height into that and centres the grid on it, so the grid rises by half the shortfall,
over the 8 dp gap and into the weekday row. The same in width: in a slot under 284 dp the weekday row's later cells got
the width that was left (12 dp, then 0) and the grid, centred, moved its days off their columns and past the body's
left edge. Measured before the change, the first week's top against the weekday row's foot (positive is overlap):

| slot (dp) | 300 × ∞ | 300 × 300 | 300 × 280 | 300 × 240 | 300 × 200 | 300 × 160 | a 300 dp screen's sheet |
|---|---|---|---|---|---|---|---|
| overlap | −8 (the gap) | −5 | 5 | 25 | 45 | 65 | 33 |

and in width, at 260, 220, 180 and 142 dp: Sunday's name 12, 0, 0, 0 dp wide; the 2nd (a Monday) 12, 32, 52, 71 dp
left of Monday's name. Densities 1 and 2 alike (within half a dp).

- **The decision and its reason.** The calendar keeps its size and overflows its slot, as the kit's rule for a form
  under a smaller constraint says (cmp-ui-component §9, «squeezed under its size»: the form keeps its size) and as
  B-168 settled it for the icon button and the FAB (F-icon-fab-01) and the empty state's art (F-empty-state-01). It
  is measured with no upper bound and the slot's least kept, so a slot that gives as much or more measures it exactly
  as before (B-150's 284 dp in a 600 dp row, a consumer's `fillMaxWidth` or `height(400)`).
  - Across, it is centred on a narrower slot and overflows both sides alike, as the icon button and the art do.
  - Down, it hangs from the slot's top and runs off its foot, as the sheet's own content does (F-bottom-sheet-05: its
    top kept on the screen). Centred, it would rise over what stands above it: in the 300 dp screen's sheet, the month
    41 dp up, over the sheet's title. A calendar reads from its month down.
  - The layout is a data-object element (`CalendarKeepsItsSize`), so a recomposition does not lay it out again.
- Rejected: **`requiredSize`-like centring on both axes** (the icon button's `NotUnder`): the calendar's month would
  stand over the words above it in any column that gives it less height. Rejected: **the calendar scrolls its own
  weeks**: a scroll inside a consumer's `verticalScroll` or a sheet's scrolling content is given no limit and never
  scrolls, and a nested vertical scroll of the same direction throws under a `LazyColumn`; a consumer who wants the
  page to scroll wraps it. Rejected: **clipping**: the last weeks would be cut with nothing to say they are there, and
  their days out of reach.
- Not covered: the sheet's own content that runs off the screen's foot (F-bottom-sheet-05's open question for the
  owner: whether the sheet scrolls its content), and what a column places after a calendar that took all its height.

- AC:
  - at every height from the natural one down to half of it, and every width from 284 dp down to half, at densities 1
    and 2, and in a bottom sheet on a 300 dp screen: no row overlaps another — the month over the weekday names, the
    first week 8 dp under them, each week 36 dp under the one before, every day under its own weekday — read by
    `positionInRoot`; no light day number is drawn in the weekday row, and the second week's numbers are drawn only
    under the weekday names' span (`CalendarShortOfRoomTest`, failing on the code before);
  - every day's touch target stays its whole 36 × 36 dp cell (research D19), the month arrows' 48 dp unchanged;
  - at its natural size the calendar is laid out as before: `DatePickerParityTest`'s cells, its goldens and B-150's
    width test unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDatePicker.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/components/CalendarShortOfRoomTest.kt`
