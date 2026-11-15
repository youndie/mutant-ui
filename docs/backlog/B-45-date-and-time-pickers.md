---
id: B-45
title: "DatePicker and TimePicker"
status: done
priority: P3
size: M
stage: stage-6-later
epic: feature-components
blocked_by: [B-10, B-43]
---

# B-45 — DatePicker and TimePicker

Later. DatePicker: body `bar` (fill = surf, r 12), cells 36, the chosen day a `pill` r 18. TimePicker: two `field` bodies 76 × 72, `over` 14, the active one fill = accent. Neither is interactive in the design.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты позже.dc.html`

## Question (2026-11-15): the calendar's day cells against the 48 dp touch target

The design draws the calendar's days in a grid of 36 dp cells, seven to a row, inside a 284 dp body (`grid-template-columns: repeat(7, 36px)`, `grid-auto-rows: 36px`). The kit's rule is a touch target of at least 48 dp by the slot (CLAUDE.md, the AC above). The design is not interactive, so it never had to choose.

This is the question already put for the menu's 36 dp rows and the drawer's 42 dp rows ([B-33](B-33-menu.md), [B-34](B-34-navigation-drawer.md)), with one choice fewer. Cells of 48 dp make a row of 336 dp, wider than the body, so the design's drawing cannot be kept inside larger cells here. What is left:

1. **Keep the 36 dp cells as an owner-named exception** to the 48 dp rule, pinned by an accessibility test. The cells tile the grid with no gap, so every touch inside it lands on a day; Compose also widens a lone small target for touch.
2. **Change the calendar's geometry**: 48 dp cells and a body about 368 dp wide. This departs from the page in every cell of the grid, and the parity of the whole picker moves.

The time picker's two fields (76 × 72) and the month arrows (which the kit would draw as 48 dp buttons) are not affected. Other findings, for when the item resumes:
- Handjet has no `◄` or `►`; the page draws them in the host's fallback face, so the kit draws them as vector triangles.
- November 2026 begins on a Sunday: six blank cells, Monday first.
- The selected day is the pill `cal-day` (r 18) with its number in `#05060c`, 700.

Who decides: the owner. The answer to B-33/B-34 likely settles this too.

**Answered 2026-11-15 by the owner: choice 1.** The calendar's 36 dp day cells stay, as an owner-named exception to the 48 dp rule (research D19). An accessibility test pins it: every day's target is its whole cell, 36 × 36, the cells tile the grid with no gap, and the month arrows and the time fields are 48 dp targets at least.

## Findings (2026-11-15)

**Built.** Two public parts, as the later page draws its recording timer (`l-date`).

**`MutantDatePicker(year, month, selectedDay, onSelect, onMonthChange, …)`.**
- The body is a dark `bar`: the surface's fill, radius 12, no marks, 284 dp wide, padding 14/16/18.
- Its header holds the month in the amber (Handjet 22, tracked 1) and the two arrows. Under it, the weekdays (Handjet 16, secondary, Monday first), then the days in 36 dp cells (Rubik 14).
- The chosen day stands in a clean drop (`pill`, radius 18), its number black and bold.
- The dates are the kit's own arithmetic: Gregorian leap years, and the first weekday by Sakamoto's method. There is no new dependency.
- The month and weekday names are parameters, Russian by default.

**`MutantTimePicker(hour, minute, onTimeChange, …)`.**
- Two fields (`field`, 76 × 72, drips to 14 dp) in Handjet 52 semibold, two digits each, with a colon in the amber between them, 10 dp apart.
- The field being set is acid with black digits (the page's «часы выбраны, минуты ждут»).

**Designed here (D15).** The page is a still.
- A press on a day picks it, and the drop stands on it at once. The arrows turn the month, and the year with it.
- The arrow keys move the focus a day or a week; Enter or Space picks.
- A press on a time field makes it the one being set. Up and Down step the focused field, wrapping. Each field reads to a screen reader as a value that can be set (`setProgress`).

**The arrows.** Handjet has no `◄` or `►`, so the page draws them in the host's fallback face. The kit draws them as triangles of the glyphs' measured size: 12 × 8, 6 dp apart, 2 dp from the row's end, a pixel under its middle. They also make the header row 25 dp, which the kit holds.

**The arrows' targets.** Their targets are 48 dp, but the glyphs stand 18 dp apart. Centred targets overlapped, and the later took the earlier's presses: a test caught "back" moving the month forward. Each target now meets the other halfway across the gap and reaches outward.

**The 36 dp cells (research D19).** Each day is its whole cell, 36 × 36, and the cells tile the grid. A D19 test pins it, with the arrows at 48 dp.

**A larger font scale.** The numbers sit on the face's own line, the cells grow to the largest of them and the calendar widens. The time fields grow to their digits. The font-scale sweep caught both.

**Parity** (`DatePickerParityTest`, `TimePickerParityTest`, both themes).
- **Calendar.**
  - Bare: 1 px in «Эфир», and 69 px in «Копия» from the speckle on its 40 dp grid (research §1.15), recorded.
  - The month, arrows, weekdays and days lie on the page's within 1 px. In «Эфир» the month's top row is one above (amber on dark on the threshold, B-24's cause), recorded.
- **Time.**
  - Bare: 3 px in «Эфир», and 20 px in «Копия» from the speckle, recorded.
  - The minutes stood a pixel late until the colon advanced by its rounded width, as B-44's chips do.
  - The digits' top rows differ by one with the bottom rows exact: their round tops sit on the ink threshold. That is recorded for the hours in both themes and for the minutes in «Эфир».

**QA.** The kit gained both for the constraint, long-content and font-scale sweeps. The keyboard sweep and the hover test gained them too; they have no disabled state.

**Sample.** A «Date · Time picker» section on the components page.

**Mutants.** All nine killed:
- `DatePickerParityTest`:
  - a 24 dp header;
  - the arrows 10 dp apart.
- `DateTimePickerBehaviourTest`:
  - the arrows' targets centred again (overlapping);
  - the week starting on Sunday;
  - every fourth year leap;
  - Down moving a day;
  - a field that stops at its end instead of wrapping.
- `TimePickerParityTest`:
  - the colon at Compose's own width;
  - the field being set without its acid.
