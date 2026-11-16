---
id: B-81
title: "The speckle's squares cost the render thread 1.3 ms a frame"
status: dropped
priority: P3
size: S
stage: stage-5-product
blocked_by: [B-78, B-80, B-82]
---

# B-81 — The speckle on the render thread

Found by [B-78](B-78-the-bodies-on-the-render-thread.md). The speckle is drawn as one small rectangle per square per tile, over every tile of a body's box and under the contour clip. Leaving it out saves 1.32 ms a frame.

- **Lowered to P3 by the owner on 2026-11-15**, after [B-82](B-82-rasterise-a-body-once-per-tick.md): the speckle of a resting body is rasterised off the render thread with the rest of it, so what is left is the speckle of bodies drawn as vectors. Re-measure on the present code first.
- **The decision and its reason.** The speckle's tile is fixed per stage, so draw it once into an image and fill the body's box with that image repeated. Skiko's `ImageShader` samples nearest, which is what square pixels want. The pixels must stay the speckle's, an SVG pattern's tile included: a square cut at the tile's edge. Measure after [B-80](B-80-contour-clip-on-the-render-thread.md), because the cost of draws under the clip depends on what B-80 does to it.
- AC:
  - B-78's bench before and after, with the control.
  - The body's parity and goldens hold.
  - A test fails when the squares are drawn one by one.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyDrawing.kt`

## Question for the owner, 2026-11-16

Its blocker [B-80](B-80-contour-clip-on-the-render-thread.md) was dropped, and its premise went the same way. The speckle's squares cost the render thread when a body is drawn as vectors. Since [B-82](B-82-rasterise-a-body-once-per-tick.md), a resting body is drawn from an image rasterised ahead. B-80's re-measure found the bodies as images at 1.16 ms a frame against 11.08 ms as vectors. What is left of the speckle's cost stands in transient vectors and in the rasterising.

Drop B-81 too, or re-measure it on its own (B-78's bench has `NoSpeckle`)?

**Answered 2026-11-16 by the owner: drop it**, perhaps to come back to once the whole kit is re-measured. Its cost on the render thread went with B-82, as B-80's did.
