---
id: B-79
title: "The outline's round-joined stroke is the dearest part of a body on the render thread"
status: dropped
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

## Results, 2026-11-14: refuted

The stroke was turned into its filled outline once per tick: `PathUtils.fillPathWithPaint` with a round join at the device scale, on desktop, iOS and wasm. Filling that outline cost the render thread what stroking did, on B-78's bench and stand ([the log](../../logs/b-79/2026-11-14-desktop.log), commit `51edd61`):

- in the smoke run, 11.10 ms a frame against 11.16 ms stroked;
- in the counted run's round 2, 11.09 against 11.16.

Leaving the outline out altogether saved 4 ms in the same runs (6.96–6.99 ms). So what the render thread pays for is **rasterising the contour**, a long anti-aliased path, every frame. Turning the stroke into geometry costs nothing measurable. The change was reverted (`452738e`), because a change that saves nothing does not ship.

**The counted run broke from its third counted round on.** The blank page fell from 60 to 17 frames a second, and a third render thread appeared. The owner was working at the machine, and the bench window most likely lost the screen, so macOS throttled it. Rounds 4–6 are void; the verdict rests on the smoke run and the clean round, which agree. The bench needs the screen to itself.

**What follows.** A body's picture changes at a tick, eight times a second, while the render thread rasterises it on every frame, sixty times a second. Rasterising a body once per tick and drawing the image between ticks is the lever the refutation points at: [B-82](B-82-rasterise-a-body-once-per-tick.md).
