---
id: B-81
title: "The speckle's squares cost the render thread 1.3 ms a frame"
status: open
priority: P2
size: S
stage: stage-5-product
blocked_by: [B-78, B-80, B-82]
---

# B-81 — The speckle on the render thread

Found by [B-78](B-78-the-bodies-on-the-render-thread.md). The speckle is drawn as one small rectangle per square per tile, over every tile of a body's box and under the contour clip. Leaving it out saves 1.32 ms a frame.

- **The decision and its reason.** The speckle's tile is fixed per stage, so draw it once into an image and fill the body's box with that image repeated. Skiko's `ImageShader` samples nearest, which is what square pixels want. The pixels must stay the speckle's, an SVG pattern's tile included: a square cut at the tile's edge. Measure after [B-80](B-80-contour-clip-on-the-render-thread.md), because the cost of draws under the clip depends on what B-80 does to it.
- AC:
  - B-78's bench before and after, with the control.
  - The body's parity and goldens hold.
  - A test fails when the squares are drawn one by one.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyDrawing.kt`
