---
id: B-10
title: "MutantSurface: the body behind the content, the layers, the clean zone, drips outside the bounds"
status: wip
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
