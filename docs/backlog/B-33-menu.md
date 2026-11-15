---
id: B-33
title: "Menu: 36 dp items, the selection by venom"
status: done
priority: P2
size: M
stage: stage-4-v1
epic: feature-components
blocked_by: [B-10, B-12, B-43]
---

# B-33 — Menu: 36 dp items, the selection by venom

v1. Body `btn` (fill = surf, r 8, `over` 18), items 36 dp, the selection window `pill` r 6. States: open, selection (venom). Opening and closing are [B-43](B-43-undesigned-behaviour.md)'s.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты v1.dc.html`

## Question (2026-11-15): rows shorter than the 48 dp touch target

The page stacks the menu items at 36 dp, edge to edge. The brief and the handoff ask for a touch target of at least 48 dp by the slot («Зона нажатия … ≥ 48 dp», README: «Зоны нажатия не меньше 48 dp»). A chip meets it with its 32 dp body in a 48 dp box (B-17). Rows stacked edge to edge cannot: each row can only own its own 36 dp band, and a 48 dp target per row would overlap its neighbours, so a press between two rows would belong to either. The same question stands for B-33 (36 dp menu items) and B-34 (42 dp drawer items). Both items wait for it; the loop goes on with the next item.

Choices, for the owner:
1. **Keep the design's rows** (36 dp) as an owner-named exception to the 48 dp rule, pinned by an accessibility test, as plakat-ui's 44 dp menu item.
2. **Rows of 48 dp**, the drawing kept at 36 dp inside with the space between rows grown. This departs from the page's spacing, and the parity of the column moves.
3. **Rows of 36 dp, each target reaching half into its neighbours' bands.** The column is one target and a press goes to the nearest row: every row's target is still 36 dp tall, so this is choice 1 under another name.

**Answered 2026-11-15 by the owner: choice 1.** The design's 36 dp rows stay, as an owner-named exception to the 48 dp rule (research D19). An accessibility test pins it: every row's target is the whole row, 36 dp tall and as wide as the menu, the rows tile the column with no gap, and nothing else in the kit is under 48 dp without being on D19's list.

## Findings (2026-11-15)

**Built.** Two public parts, as the v1 page draws them (`v1-menu`).
- **`MutantMenu(expanded, onDismissRequest, items, selectedIndex, onSelect, …)`** is a popup standing `offset` from its parent: 50 dp down by default, the page's place under the anchor.
  - Its body is `btn`: 216 dp, padding 6, the surface's fill, radius 8, drips to 18 dp (`cf .5`), no marks.
  - Its rows are 36 dp in Handjet 20, tracked 1, on the row's 36 dp line as Blink puts it.
  - The rows stand in a ladder 30, 38, 46 and 54 dp in, repeating every four.
  - The selection is a clean window (`pill`, radius 6, calm .2) with a filled play mark 8 dp in, and its word in black.
- **`MutantDropdown(label, items, selectedIndex, onSelect)`** is the page's acid anchor chip with its label (Rubik 13/500) and a chevron stroked 3, in a 48 dp target, with the menu under it.

**The venom.** A new selection reaches its row by venom, as the segmented button's does (B-31): tentacles from the old window, the mass along them, the window shown once filled, then springing. The run is now one internal `VenomRun` that both use. Each gives its slots' geometry: the segmented button's horizontal, the menu's vertical. The segmented button's parity and behaviour are unchanged.

**Designed here (D15).**
- The menu opens and closes at once, as the tooltip does. The brief's event table has no menu row.
- The dropdown closes once the venom has arrived (720 ms), at once under reduce motion, on a timer of its own rather than on the screen's clock.
- Up and Down move the keyboard's focus from row to row, wrapping. Moving focus with `FocusManager` did not reach the popup's rows, so each row holds a `FocusRequester`.
- Enter or Space picks; Escape and a press outside dismiss.
- Every row and the anchor draw the focus brackets.

**The 36 dp rows (research D19).** Each row is its whole target, 36 × 204, and the rows tile the column: the owner's named exception. `MenuBehaviourTest`'s D19 test pins it.

**Parity** (`MenuParityTest`, both themes).
- The anchor's word and chevron, the play mark and the four rows' words lie exactly on the page's.
- Bare: 56 px in «Эфир» and 124 px in «Копия». Chrome draws the speckle as an SVG pattern rasterised once and resampled, where the kit draws each square exactly: single pixels on its 40 dp grid, over the window in «Эфир», and over the body under «Копия»'s slip with two short runs of the window's highlight at its top edge, as the segmented button's (research §1.15). Recorded and never to be raised.
- Before the 36 dp line, every word stood a pixel low: Compose centred the face's own line in the row.

**Behaviour** (`MenuBehaviourTest`):
- the anchor opens the menu, a pick reaches `onSelect` and the menu closes;
- Escape closes it, and Down moves the focus to the next row;
- on a running clock the window reaches a new row by venom: at 160 ms the third row holds less than half the acid it holds settled;
- D19: the rows are 36 × 204 and stand 36 apart.

**QA.** The kit gained the dropdown and the menu's body for the constraint, long-content and font-scale sweeps. The keyboard sweep and the hover test gained the dropdown, which has no disabled state.

**Sample.** A «Menu» section on the components page.

**Mutants.** All eight killed:
- `MenuParityTest`:
  - no ladder;
  - the words on the face's own line;
  - the window at full wear.
- `MenuBehaviourTest`:
  - no venom, so the window is there at once;
  - 40 dp rows;
  - Escape ignored;
  - Down going nowhere;
  - the dropdown never closing after a pick.
