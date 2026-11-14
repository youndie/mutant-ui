---
id: B-79
title: "The outline's round-joined stroke is the dearest part of a body on the render thread"
status: wip
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-78]
---

# B-79 — The outline's stroke on the render thread

Found by [B-78](B-78-the-bodies-on-the-render-thread.md). Leaving every body's outline stroke out saves 4.03 ms of the render thread's 11.07 ms a frame, the most of any part. The contour is a long generated path with a trembling edge, and it is stroked with a round join every frame. Skia turns that stroke into geometry on the render thread, frame after frame, although the contour changes only at a tick.

- **The decision and its reason.** Turn the stroke into a filled outline once per tick, off the main thread, beside the tick's frame built ahead (B-75). The render thread then fills a path it already has instead of stroking one. The picture must stay the stroke's: the body's parity, goldens and frame tests are unchanged, or the gap is a number with its cause. Rejected: a thinner or mitred stroke, which changes what the design draws.
- AC:
  - B-78's bench, same stand: render CPU per frame before and after, with the control's readings.
  - Parity and goldens hold.
  - A test fails when the stroke is drawn as before.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyDrawing.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/bench/BodyRenderBench.kt`
