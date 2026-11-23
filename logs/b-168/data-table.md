# B-168 — data-table: `MutantDataTable`, `MutantTableColumn`

Group 23 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-data-table` from `662fa7a`,
2026-11-23, on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand). The table lives in
`mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDataTable.kt`; its box is the checkbox's
`CheckBoxControl` (`MutantCheckbox.kt`, the checks-switch group's file). Its numbers were decided in
[B-46](../../docs/backlog/B-46-data-table.md). The design draws it at rest with one row selected (handoff README,
«Компоненты»: DataTable, `rest, row selected`); the later page's checkboxes are live, so the engine pours and melts them.

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-data-table/` (not in the
repository).

- **The table as the sample composes it** (`core/<look>/`): the sample's four rows, 520 dp, the record column free, the
  length 90 dp (Handjet), the channel 80 dp (Handjet, secondary), the second row selected, in eight looks: «Эфир»,
  «Копия», reduce motion, «Чистый сигнал», `steppedMotion`, density 2, and the browser's way of drawing
  (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true,
  `LocalMutantBodyAsImage` false) in both palettes. Every frame captured, a per-frame pixel diff (`*.diff.txt`) and
  sheets, the box column zoomed 2× for the transitions:
  - appear (`appear.png`) and two seconds at rest (`rest.png`);
  - a mouse click selecting the first row (`select-r0.png`), deselecting the second (`deselect-r1.png`);
  - selection moving quickly: the third row on, the fourth 96 ms later, the third off 96 ms after that, the first off
    (`rapid.png`);
  - a press held 480 ms on a row and released into a toggle (`press-r1.png`); a hover arriving and leaving
    (`hover-r2.png`);
  - `enabled` false and back (`disable.png`, `enable.png`);
  - the keyboard: Tab to the first row, Tab, Space (`keyboard.png`).
- **The tick against the fill under it**, every frame of a check, an uncheck, and an uncheck 80 ms into a check, by
  the same run twice, the second with the ticks drawn transparent (`DataTableBody(hidden = true)`, the bare twin's
  seam), turned in the same frames: per row, the pixels the tick changes, how many read under 2:1, their mean contrast
  (`core/contrast-<look>.txt`; «Эфир», «Копия», `steppedMotion`, the browser's way in both palettes).
- **Widths and words** (`core/constraints/`): the sample's table at 520 dp, a 360 dp one with the English headers at
  the demo's 70 / 60 dp columns and long cells (`1:58:10`, `100:42:00`, `CH 103`, an 83-letter title), and a 200 dp one,
  left to right and right to left (`widths-ltr.png`, `widths-rtl.png`, crops `ltr-zoom.png`, `rtl-zoom.png`); no width at
  all, in a `horizontalScroll` row and under `wrapContentWidth(unbounded = true)`, with and without the selection column
  (`unbounded-*.png`); font scale 2 (`fontscale2.png`).
- **Many rows scrolling** (`core/scroll/`): 60 rows, five selected, in a 400 dp `verticalScroll`, scrolled 23 dp a
  frame through its `ScrollState` for 60 frames, by paths and in the browser's way.
- **The demo** (`sample/`): `DemoApp` on its Search tab at 360 × 720 in both palettes, a mouse click selecting «Test
  card», then the sort menu's «Longest» (`demo-sort-efir.png`, `demo-sort-kopiya.png`).

## Findings

### F-data-table-01 — given no width, the record column is 0 wide: every name and its header vanish — local

- **What.** In a `horizontalScroll` row or under `wrapContentWidth(unbounded = true)` the table is the fixed columns
  alone, 210 dp (40 + 90 + 80): `RECORD` and every record's name are gone, with no ellipsis, the lengths and channels
  standing where the names were. The free column is `weight(1f)`, and a `Row` under no width limit gives its weighted
  children nothing.
- **Frames.** `core/constraints/unbounded-hscroll.png`, `unbounded-wrapUnbounded.png`, `unbounded-noHandler-hscroll.png`
  (the same without the selection column).
- **The design and the item.** The page sets the column `minmax(0, 1fr)` in a 560 px box: it never meets no width.
  B-46's acceptance: «a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content»; the
  kit's rule for no width (B-40, B-58) is the content's own width, as a `Text` and a `Row` give.

### F-data-table-02 — rows are matched by position: a reorder leaves the selection on the slot, not on the record — local (the means) and reported (the sample uses it)

- **What.** The demo's Search screen sorts its table with «Test card» selected. In the first frame after the sort
  the acid box and its tick stand whole on the third row, which now holds «Rain over the transmitter», while «Test
  card», moved to the last row, shows an empty box; then the third row's box pours away and the last one's pours in,
  ~330–400 ms, so for that time the table says the wrong record is chosen. Each row's composition, its interaction
  source and its box's pour state are kept by its index, and the table offers no way to key a row by what it shows.
- **Frames.** `sample/demo-sort-efir.png`, 2404 → 2676 ms (the third row acid with «Rain…», the last row empty, then
  both pouring); the same in `demo-sort-kopiya.png`.
- **The design and the item.** The page never reorders its rows; B-46 is silent. The skill's rule (cmp-ui-component §3,
  field-ui B-89): repeated items are keyed by identity, never by position.

### F-data-table-03 — unchecked during its check's first steps, the tick shows whole for a frame-run before it sinks — reported (`surface/BodyTransitions.kt` and B-94's rule in `surface/BodyMoves.kt`; the box is `CheckBoxControl`)

- **What.** A row checked and unchecked 80 ms later: the tick had 3 of its 66 pixels melted in; in the first frame of
  the uncheck it stands whole and stays 100–200 ms before sinking out. `mutantMelt` starts its melt-out from a whole mark
  whatever the melt-in reached (the engine's `melt` restarts its entry too), and B-94 pours the box back from its own old
  fill under half a pour, so in «Эфир» the whole tick lies on the dark old fill: 66–90 pixels, every one under 2:1,
  mean 1.44–1.86:1 (`core/contrast-Efir.txt`, row 2, 3140 → 3216 → 3424 ms; the browser's way the same,
  `contrast-GpuEfir.txt`; under `steppedMotion` 66 pixels for 130 ms). In «Копия» the old fill is light and the
  flicked-in tick reads (`contrast-Kopiya.txt`): there it is a mark appearing on an uncheck.
- **Frames.** `core/Efir/rapid.png` shows the slower case (unchecked 192 ms in: the tick sinks from where the pour stood,
  5884 → 6008 ms, as designed).
- **Reach.** A new face of F-checks-switch-04 (that one checks again within the first half of an uncheck); the same
  path in the checkbox.

### F-data-table-04 — the fixed columns hold their dp whatever the words: at font scale 2 the header is cut and cells touch; at scale 1 an ellipsis touches the next column — reported (owner: a number the design does not state)

- **What.**
  - Font scale 2: `ДЛИНА` fills its 90 dp and `КАНАЛ` ends `КАН…`; `ДЛИНАКАН…` reads as one word, and «Настроечная
    таблица» meets its length as `таблица02:10` (`core/constraints/fontscale2.png`).
  - The demo at scale 1: an ellipsized title butts on its length, `Static between chann…09:02`,
    `Rain over the transmi…31:12` (`sample/demo-sort-efir.png`, any frame).
- **The design and the item.** The page's grid has no gap between its columns (`grid-template-columns: 40px
  minmax(0, 1fr) 90px 80px`) and its words are short; B-46 decided «a cell's text runs on one line and ends in an
  ellipsis; a larger font scale grows the row», and its acceptance asks that font scale 2 not clip the content. A gap
  between columns, or fixed columns that grow with the font scale, are numbers for the owner.

### F-data-table-05 — narrower than its fixed columns, the table drops the record names without a mark — reported (owner)

- **What.** At 200 dp (under the 210 the fixed columns take) the record column is 0: the names vanish, the selected row
  is a box, a length and a channel (`core/constraints/widths-ltr.png`, the third table). The page's `minmax(0, 1fr)`
  does the same in a browser, so it is as designed; whether the free column keeps a minimum (or the fixed ones give
  way) is not decided.

### F-data-table-06 — right to left, a left-to-right word's ellipsis stands at its start — reported (shared: the text, `MutantText.kt` / the platform)

- **What.** In a right-to-left layout the 83-letter Cyrillic title is set from its start and cut at its end with no
  mark there, the ellipsis drawn at its left: `…Эфир, ночь на вторник, п` (`core/constraints/rtl-zoom.png`, against
  `ltr-zoom.png`: `Эфир, ночь на вторник, п…`). The paragraph's ellipsis goes to the layout's end, not the word's.
  Every ellipsized word of the kit in a right-to-left app with left-to-right content shares it.

## Looked at and found right

- **Select and deselect**, in every look: the acid pours down in steps of 83 ms (on every frame by default, D22), the
  tick melts in from the top and, unchecked, sinks out below the box as the checkbox's does since F-checks-switch-01;
  under reduce motion each toggle lands in one frame and the rest stands (0 pixels change at rest); density 2 has every
  box, tick and divider where density 1 has it.
- **Selection moving quickly** across rows: each box keeps its own pour; nothing jumps but F-data-table-03.
- **Pressed and hovered**: the box simmers to amber under the mouse and boils under a held press (the row is its
  interaction source, B-46); a press released into the toggle drops the press's heat for the pour's first steps
  (F-checks-switch-03, the same path, not reported again).
- **The words** stand on the page's ground, not on a fill: no row is filled when selected (the page's `row selected`
  is the box alone), so their contrast never changes; disabled and enabled again, the header and cells take
  `textDisabled` and back in the first frame (B-169 in place), the boxes greyed with their ticks.
- **The keyboard**: Tab reaches each row and the box shows the brackets (B-46 decided the box shows the row's focus),
  Space toggles.
- **The header** 36 dp and rows 48 dp (held by `DataTableBehaviourTest`), the tracking dividers under the header and
  every row, mirrored columns right to left with the box at the right.
- **Sixty rows scrolling**: every visible box drawn with its body in every frame, in both ways of drawing; the diff is
  the scroll's own (~39 000 pixels a frame), no frame jumping out.
- **The browser's way**: per-frame diffs within a few dozen pixels of the desktop's in every scenario, no stale fill.
- **«Чистый сигнал»** changes nothing in a table (the Tape and the loss are the screen's): frames identical to «Эфир».
- The dividers are keyed `0`…`n` whatever the table's `key`, as the engine numbers a stage's dividers; two tables on
  one stage draw the same strokes (the reported default-key face; not seen on any of the sample's screens).
