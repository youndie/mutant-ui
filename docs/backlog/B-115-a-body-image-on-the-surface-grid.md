---
id: B-115
title: "A body's image rasterised on the surface's pixel grid"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-115 — A body's image rasterised on the surface's pixel grid

Found by [B-114](B-114-a-tick-image-built-in-the-frame.md). A body's image of a tick (B-82) was rasterised with its
origin on a whole scene unit and drawn at the nearest whole pixel of the surface. At a whole density the two are the same
pixel; at a fractional one the image stood up to half a pixel off the body's vectors: at 2.625, the owner's Pixel 6a,
2.9 % of a card's pixels more than 16 levels apart (8 625 of 297 675), against `BodyAsImageTest`'s line of 0.05 %. The
test ran at densities 1 and 2 only.

- **The decision.** Rasterise on the surface's own grid: the scene's point at the surface's origin is the image's pixel
  grid's origin, the image starts at the whole pixel under the body's box, and drawn there nothing is resampled. Nothing
  changes at a whole density.
- AC: `BodyAsImageTest` at 2.625 in «Копия» within its line; the goldens unchanged; without the change, the 2.625 case
  fails (it did, at 8 625 pixels).
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`

## Results, 2026-11-18

`BodyAsImageTest` holds the image to the vectors at 2.625 within 0.05 %, on every tick compared; the code gate is green.
