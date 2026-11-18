---
id: B-137
title: "The edge mosaic's blocks past the body's contour, as an experiment"
status: done
priority: P3
size: S
stage: stage-6-later
---

# B-137 — The edge mosaic's blocks past the body's contour, as an experiment

The owner, 2026-11-18: try the edge mosaic not only inside the contour but past it, the pixel blocks leaving the body's
outline, to see how it looks.

How the design draws it. `edgeMos` lays up to three rows of 4 and 8 dp blocks along one side of the body, from the slot's
edge inwards (rows at 0, `bs` and `2·bs`), and records each as a quantisation block, with one more row of 4 dp blocks
just outside the side (`mutant-generator.js`, `edgeMos`). Under a block the field takes its value from the block's
centre (handoff README, «Конвейер кадра», step 3), so a block is wholly in the body or wholly out of it and the contour
breaks into steps along the blocks. The tiles are drawn inside the clip by the contour (handoff README, «Слои
отрисовки», layer 3; `mutant-engine.js`, `itemHtml`): what lies past the outline is never seen. In the kit:
`Instances.edgeMosaic`, `drawBody`.

- **The decision and its reason.** A departure (D20) behind `MutantTheme(mosaicPastContour = true)`, off by default, so
  that every golden, parity cell and the vendored golden stay as they are. Switched on:
  - over each block of the edge row, one block of its size stands outside the slot's side (chance 0.7), and over that
    one 4 dp block further out (chance 0.4), tinted as the design's tiles are; they come from a stream of their own,
    `edge:spill`, so the design's tiles, its quantisation blocks and the field are the same with the switch on or off;
  - the edge row and the blocks outside are drawn over the outline, unclipped: the compression's blocks cross the ink
    line and stand outside the body. Each takes the colour of what it stands on, under its tint — the body's fill inside
    the contour, the theme's page outside it (the owner, 2026-11-18: «цвет мозаики за рамкой тела должен соответствовать
    фону»), so under a block that crosses the outline the ink is gone. A body does not know what lies behind it; on a
    ground other than the page (a card on a sheet) the blocks outside show the page's colour. The rows further in stay
    inside the clip as designed;
  - the blocks outside never enter the clean zone and never touch the slot; layout, the slot and the touch target do
    not depend on them (they are drawn, not measured);
  - a pressed body's edge row runs as the design's running mosaic does; the blocks outside keep their rest.
- **Rejected:** the contour wrapping the blocks — the blocks outside joined to the body through the field (their cells
  forced inside), so the fill and the ink outline go round them. It reads as an 8-bit silhouette, a shape, not
  compression damage over an edge; it changes the field, the highlight and the drips' joins with it, and a block 8 + 4 dp
  outside a side passes the field's 7 dp grid margin, so the grid would grow (D21) for every body that has it.
- **Not covered:** the disabled body («нет сигнала»), which draws its own mosaic under its own clip; the «Темы» cell of
  the sample's Components page, which sets a theme of its own; the browser's or the phone's frame cost of the extra rectangles (tens per body, drawn once a tick
  with the rest).

- AC:
  - off by default: a body drawn by the theme is the body as designed, to the pixel (`MosaicPastContourTest`), and the
    existing goldens and parity pass unchanged;
  - on: only the pixels under the edge row and the blocks outside change, and the blocks outside stand outside the slot
    and clear of the clean zone (`MosaicPastContourTest`);
  - the owner sees contact sheets of both — button, FAB, card, field; «Эфир» and «Копия»; densities 1 and 2.625 — in
    `logs/b-137/`, and decides whether it stays.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/Instances.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyDrawing.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/theme/MutantTheme.kt`

## Findings, 2026-11-18

- **Built.** `MutantTheme(mosaicPastContour: Boolean = false)` and an internal local `MutantSurface` reads; the blocks
  outside as `Instance.spillTiles` (`Instances.spillTiles`, stream `edge:spill`); `drawBody(pastContour)` draws the edge
  row and them over the outline (`drawMosaicPastContour`): the fill clipped to the contour, the page clipped to outside
  it, the tint over both. The sample's header has a «Mosaic past the edge» / «Мозаика за контуром» switch with a
  tooltip.
- **Measured** (`MosaicPastContourTest`):
  - 825 blocks past the contour over 72 bodies (24 keys each of a button, a card and a FAB), none inside the slot or in
    the clean zone. The clean zone lies inside the slot, so the blocks outside cannot reach it; the generator checks it
    anyway, for a zone that one day overhangs;
  - switched on, 3 879 pixels change over eight buttons at density 1, every one of them under the edge row or a block
    past the contour (a pixel wider for the rasteriser's edge);
  - off, `drawBody`'s default draws the same pixels as the switch off, and every existing golden and parity cell passes
    unchanged (`./gradlew check`).
- **Mutants.** Drawing every row over the outline, not the edge row alone, fails the pixel test («pixel (191.5, 35.5)
  changed away from the mosaic»); ignoring the switch fails it with «the switch changed nothing».
- **What moved.** No golden, parity cell or the vendored golden: the switch is off by default and the new stream is read
  by nothing else. The ABI dump: `MutantTheme` gained a parameter, as B-100's did.
- **The sheets.** `logs/b-137/sheet-{efir,kopiya}-{1x,2.625x}.png`: the design on the left, the switch on the right;
  the two buttons, two FABs and a third button, the card, the field, at the still time (5 000 ms).
  - Outside the body the blocks are the page's colour under their tint: grey and pale squares over the page, and where a
    block crosses the outline it cuts the ink line.
  - The first take drew them in the body's fill, as the slime's own pixels thrown over the ink line; the owner asked
    for the ground's colour instead.
- **The owner's answer, 2026-11-18:** kept and merged, behind the switch, off by default; the design's mosaic stays the
  kit's default.
