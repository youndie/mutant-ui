---
id: B-117
title: "A body's still layers drawn once, on the surface's grid"
status: wip
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
