---
id: B-80
title: "Clipping a body's layers to its contour costs the render thread 2.7 ms a frame"
status: open
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-78]
---

# B-80 — The clips to the contour on the render thread

Found by [B-78](B-78-the-bodies-on-the-render-thread.md). Every body draws two groups under a path clip: its fill, speckle, mosaic, dead pixels and flow lines under the contour; its highlight under two rectangles. Leaving the contour clip out saves 2.07 ms a frame, and leaving the highlight out (its clip and two fills) saves 0.61 ms. With the fill left out, the draws under the clip cost 37 ms more, which points at the anti-aliased path clip as the mechanism.

- **The decision and its reason.** Measure first which form of the same picture is cheapest on the render thread, each read on B-78's bench with its control:
  - the clip as it is;
  - the group drawn into a layer and masked by the filled contour;
  - the fill drawn as the contour path itself, with only the inner layers clipped.

  Take the cheapest form whose pixels are the body's. Rejected: dropping the clip, which lets the speckle and mosaic out past the edge.
- AC:
  - The render CPU per frame of each form, with the control.
  - The form chosen holds the body's parity and goldens.
  - A test fails when the old form is drawn.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyDrawing.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/bench/BodyRenderBench.kt`
