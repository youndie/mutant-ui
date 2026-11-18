---
id: B-117
title: "A body's still layers drawn once, on the surface's grid"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-117 — A body's still layers drawn once, on the surface's grid

[B-112](B-112-static-decorations-drawn-once.md)'s cut, measured again on the calibrated proxy by
[B-116](B-116-the-proxy-calibrated-to-the-phone.md): 6.0–6.7 ms a frame saved in every counted round, 16.4 → 18.3 frames a
second. It failed four parity tests of the design in the frame path (`DrawerParityTest`, `IconButtonParityTest`,
`SheetParityTest`, `KeyframeLossParityTest`), by 1.7–2.7 % of pixels.

- **The decision.** The speckle, the mosaic's tiles and the dead pixels drawn once per instance into one image on the
  surface's grid ([B-115](B-115-a-body-image-on-the-surface-grid.md) put the tick's image there too), so the tick's image
  and the frame draw the same image at the same pixel. Where the frame's canvas is not that grid (a body in the keyframe
  loss's picture, a surface placed off whole pixels, a slide), the layers are drawn as before.
- **Measured before it runs.** As B-116: the proxy slowed ×2, the kit, this item, the kit without the four decorations.
  It **buys its cost** if every counted round is cheaper than the kit in work p50 and in ms a frame by more than twice the
  largest move of `Blank`'s work between the same builds.
- AC: every parity test and golden green; `StillLayersOnceTest` at 1 and 2.625, both clocks, within 0.2 % of pixels over
  16 levels; a one-pixel shift of the image fails it.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyDrawing.kt`

## Results, 2026-11-18

**The parity failures** were edge pixels: one image clipped by the contour blends its squares with the edge's coverage
once, where each square clipped alone blends it each time. Against the kit's own renders, at most 3 pixels more than 16
levels apart, the largest 29 (the sheet in «Копия»); against the references that moved the drawer's residual 9 → 10, the
sheet's 40 → 79, the extended FAB's 17 → 18, all three recorded "never to be raised". So a frozen clock (fixtures,
references) keeps drawing the layers as before, as it already draws vectors and no tick image (B-82): every parity test
and golden is green with nothing raised, and `StillLayersOnceTest` pins the frozen renders as the same pixels (a mutant
dropping that guard fails it). Under a running clock the layers come from the image, held to the each-tick draw within
0.013 % of pixels over 16 levels at 1 and 2.625 (a one-pixel shift fails it).

**Measured** ([the run](../../logs/b-117/2026-11-18-proxy-x2.log), the proxy slowed ×2, on mains). `Blank`'s work moved by
at most 1.4 ms, the line 2.8. Against the kit at 16.3 frames a second, every counted round: **3.7, 4.8, 6.1, 6.7, 7.5 ms a
frame saved** (work p50 7.4 to 69.6), 18.0 frames a second. The kit without the four decorations, in the same run: 21.8 to
23.8 saved, 26.0 a second. **It buys its cost**, and leaves about 17 ms of the decorations' 23.

Both gates green on `6d2df97`.
