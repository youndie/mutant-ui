---
id: B-45
title: "DatePicker and TimePicker"
status: open
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
