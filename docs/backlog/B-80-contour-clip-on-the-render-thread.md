---
id: B-80
title: "Clipping a body's layers to its contour costs the render thread 2.7 ms a frame"
status: question
priority: P3
size: M
stage: stage-5-product
blocked_by: [B-78, B-82]
---

# B-80 — The clips to the contour on the render thread

Found by [B-78](B-78-the-bodies-on-the-render-thread.md). Every body draws two groups under a path clip: its fill, speckle, mosaic, dead pixels and flow lines under the contour; its highlight under two rectangles. Leaving the contour clip out saves 2.07 ms a frame, and leaving the highlight out (its clip and two fills) saves 0.61 ms. With the fill left out, the draws under the clip cost 37 ms more, which points at the anti-aliased path clip as the mechanism.

- **Lowered to P3 by the owner on 2026-11-15**, after [B-82](B-82-rasterise-a-body-once-per-tick.md): a resting body is now drawn from an image rasterised off the render thread, so the clip's cost on the render thread is left only to bodies drawn as vectors (pressed, in a transition, the first ticks, and under a held clock until [B-83](B-83-a-held-clock-draws-vectors.md)). Re-measure on the present code before choosing a form.
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

## Re-measured, 2026-11-16

On the present code (`3120977`, after B-82, B-83, B-87 and B-77), B-78's bench with its control ([log](../../logs/b-80/2026-11-16-before.log), [the rasterising](../../logs/b-80/2026-11-16-raster.log)):

| Scene | render per frame |
|---|---|
| a blank page | 0.37 ms |
| the bodies as vectors | 11.08 ms |
| the bodies as vectors, without the contour clip | 8.95 ms: the clip still costs 2.13 ms, as B-78 found |
| **the bodies as images**, as the kit draws them at rest since B-82 | **1.16 ms** |

The control reads: its load adds 2.39–2.73 ms on the blank page, 1.42–1.52 over the vectors and 1.68–1.84 over the images.

- **At rest the clip costs the render thread nothing.** It is paid where the body is rasterised, ahead and off the render thread: 2.58–3.23 ms a body with the clip, 2.33–2.55 without, paired by round 0.23–0.89 ms. On the JVM that is a background thread. In the browser it is the page's only thread, between frames (B-87).
- **The render thread still draws vectors** for a pressed body, one in a transition, and a body's first ticks. That is about 2.13 ms over nine bodies, so roughly 0.24 ms a body, for as long as it lasts.

## Question for the owner, 2026-11-16

The item's premise was a cost on the render thread at rest, and that cost is gone since B-82. The choices:

1. **Drop B-80.** Its cost now stands only in transient vectors (a press, a transition, the first ticks).
2. **Re-scope it to the rasterising.** The clip is about a sixth of a body's raster (0.2–0.9 of ~2.9 ms). That counts most in the browser, where the rasterising runs on the one thread. The forms to try stay those above.
3. **Keep it as written**: measure the three forms on vectors.

B-81 (the speckle on the render thread) is blocked by B-80, and its premise is likely in the same state.
