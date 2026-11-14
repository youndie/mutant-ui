---
id: B-10
title: "MutantSurface: the body behind the content, the layers, the clean zone, drips outside the bounds"
status: done
priority: P0
size: L
stage: stage-1-materials
blocked_by: [B-03, B-05, B-07, B-08]
---

# B-10 — MutantSurface: the body behind the content, the layers, the clean zone, drips outside the bounds

Every component sits on one body renderer. The handoff gives its layers, the clean zone and the rule that drips hang below the slot without clipping or taking space (research D5).

- **The decision and its reason.** `MutantSurface(type, key, modifier, state) { content }` draws, bottom to top: misregistered shadow, the clipped group (fill, speckle tile 40 dp with 8 dots, edge mosaic, dead pixels, flow lines, all shifted by `slip`), the highlight iso-line at −3.4 dp with its fill-coloured offset copy, the outline whose width trembles ±12 %·boil. Paths are rebuilt once per tick and remembered by tick. The clean zone is the content's measured bounds plus 6 × 2 dp, passed to the generator as `tz`/`cz`.
- An instance is rebuilt when the slot's size changes; whether the jump is visible on a growing text field is measured and put to the owner if it is (R1).
- Rejected: one `Canvas` per layer (a draw per layer per instance).

- AC: a button, a card and a field match their resting references within the floor; drips leave the slot and the next item's layout does not move; flow lines and mosaic stay out of the clean zone over 1000 keys; a mutant in the layer order or the clean-zone padding is caught.
- Anchors: `reference/design-system/mutant-engine.js`, `reference/design-system/mutant-generator.js`, `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `BodyDrawing.kt`: `drawBody` layers a body as `itemHtml` does; `Speckle` is the stage's 40 dp tile, rounded as the JS writes it; `outlineWidth` is `drawItem`'s tremble to two decimals, seeded by the instance's hash (D16); `highlightFor` is the engine's `hl`. `MutantSurface` (internal): the body behind the content, drawn outside its bounds; the clean zone from what the content marks with `mutantCleanZone()`, padded 6 × 2 dp; the instance and its field cache held across ticks by `HeldInstance`, rebuilt when the cassette, key, size, wear, profile or zone change; one frame per tick of the screen's clock; the body's origin its own (D16). `LocalMutantColors`, `LocalMutantCassette`, `LocalMutantStage` stand in for the theme of B-09.
- References: `Button_RestBody_Efir`, `Card_RestBody_Efir`, `Field_RestBody_Efir`, every text on the stage transparent, each with the slot the engine built recorded in the manifest. `BodyParityTest`: 0.008 %, 0.000 %, 0.000 % against a frozen 0.02 %; the actual and the diff of each looked at — the card's mosaic, dead pixels, scan line, flow lines, drips and highlight are there in both.
- `MutantSurfaceTest` (4): drips paint below the slot while the next element starts at its edge; marked bounds become `tz` and `cz` with the engine's padding; over 320 frames the instance is built once and a frame is built per tick; over 1000 keys no mosaic block, flow line or dead pixel enters the clean zone.
- Mutants, each fails a named test: the outline drawn first; the highlight unclipped; the speckle left out; the zone's horizontal padding 6 → 4; the instance rebuilt every tick.
- R1 measured (research §1.10): a field 260 → 268 dp keeps all drips, 80 % of the mosaic, none of its flow lines; put to the owner as [B-51](B-51-size-change-moves-the-flow-lines.md).
- Not covered: states ([B-11](B-11-surface-states.md)); the first frame built off the frame ([B-50](B-50-first-frame-off-the-frame.md)); «Копия» page cells; the public API, which the first component decides.
