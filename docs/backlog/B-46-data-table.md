---
id: B-46
title: "DataTable: OSD header, 48 dp rows, tracking dividers, row selection"
status: done
priority: P3
size: M
stage: stage-6-later
epic: feature-components
blocked_by: [B-18, B-25]
---

# B-46 — DataTable: OSD header, 48 dp rows, tracking dividers, row selection

Later. Body `box` for selection; header OSD 36, rows 48, tracking dividers. States: rest, row selected.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты позже.dc.html`

## Findings (2026-11-15)

**Built.** `MutantDataTable(rows, columns, …)`, `MutantTableColumn` and `MutantTableCellStyle`, as the later page composes them (`l-table`, 560 dp):
- a 36 dp header in Handjet 18, tracked 1, the on-screen-display amber;
- 48 dp rows, with a tracking divider under the header and under every row;
- columns of a fixed width, or the rest (the page's `minmax(0, 1fr)`);
- three cell styles: Rubik 14/500 (the record), Handjet 20 (the length), Handjet 20 secondary (the channel).

Given `onSelectedChange`, a 40 dp column of checkboxes leads each row. The box is the checkbox's own, the acid fill and the tick, without its two burnt blocks: the page's table draws the tick alone. `MutantCheckbox`'s box is now an internal `CheckBoxControl` that both use.

**Designed here (D15).** The page presses the 22 dp box in its 40 × 48 cell, narrower than the 48 dp rule. In the kit the whole row is the toggle (`Role.Checkbox`), so every target is the row: 48 dp tall and as wide as the table. The box shows the row's focus from the keyboard, as a checkbox's does.

**Parity** (`DataTableParityTest`, both themes).
- Bare: 1 px in «Эфир». In «Копия» it is 13 px against a floor of 5: single speckle pixels under the print slip over the four boxes (research §1.15), recorded and never to be raised.
- The header, the names, the lengths, the channels and the tick lie within 1 px of the page's, their colour within 16. Handjet is allowed its row above on macOS 26 (B-74).
- No layout cause was found: the grid's columns are whole pixels.

**Behaviour** (`DataTableBehaviourTest`):
- a press on a row's words toggles it, and every target is at least 48 × 48;
- Tab reaches each row and Space toggles it;
- without a handler there is no checkbox column and nothing to toggle;
- the rows stand at 38, 88 and 138 dp under the header and its divider.

**QA.** The kit gained a table for the constraint, long-content and font-scale sweeps. The keyboard sweep and the hover test gained it too. **Reduce motion**: the boxes are `MutantSurface`s under the screen's clock, and the checkbox's pour and melt keep their own reduce-motion rules (B-19).

**Sample.** A «Data table» section on the components page.

**Mutants.** All seven killed:
- `DataTableParityTest`:
  - the checkbox's burnt blocks drawn in the table;
  - a 40 dp header;
  - the header untracked;
  - the channel in the text colour;
  - a 48 dp checkbox column.
- `DataTableBehaviourTest`:
  - the row no longer the toggle;
  - 44 dp rows.
