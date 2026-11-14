---
id: B-78
title: "The bodies' drawing is most of the render thread's frame on the Demo page"
status: done
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-76]
---

# B-78 — The bodies' drawing on the render thread

Found by [B-76](B-76-render-thread-replay-on-the-demo-page.md). Of the Demo page's 19.8 ms of render CPU a frame, about 15–16 ms is the content of its three screens, and it scales with what each holds: Library 10.1 ms alone, Recording 6.2, Settings 5.7. The thread is 91 % busy, and the page holds 46.5 frames a second. The sample cannot switch off the parts of a body, because they are the kit's internals.

- **The decision and its reason.** Measure inside the kit, in `mutant-core`'s own tests, where the internals are reachable. A real window draws a fixed set of bodies (the Library screen's set), and B-76's sampler and render control read it while one part at a time is left out of the draw:
  - the fill;
  - the outline;
  - the highlight;
  - the drips;
  - the flow lines;
  - the mosaic;
  - the dead pixels;
  - the shadow and the print slip.

  Each part's cost then becomes an item, with a number before and after its fix. Rejected: changing a part's drawing before its cost is known.
- AC:
  - The bench in `mutant-core`, with the render control reading on it.
  - A number per part.
  - An item per part that costs more than the control's spread.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyDrawing.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`

## Results, 2026-11-14

Run at `c7ddf52` on B-72's stand: [the log](../../logs/b-78/2026-11-14-desktop.log). The bench is `mutant-core`'s `bodyRenderBench`: a real window, three columns of the Library screen's bodies (a field, four chips, two cards and a button each), no Tape. Each part is left out of every body through `LocalMutantSkipBodyParts`, which the kit leaves empty. B-76's sampler reads the render thread's CPU per frame. Six rounds of nine seconds, scenes rotating, round 1 not counted.

| Left out | Render CPU / frame | Saved | Spread over rounds |
|---|---|---|---|
| nothing (the bodies) | 11.07 ms | — | 10.99–11.13 |
| the outline's stroke | 7.04 ms | **4.03 ms** | 6.93–7.08 |
| the clip to the contour | 9.00 ms | **2.07 ms** | 8.92–9.03 |
| the speckle | 9.75 ms | **1.32 ms** | 9.62–9.81 |
| the highlight | 10.46 ms | **0.61 ms** | 10.34–10.49 |
| the mosaic / dead pixels / flow lines / shadow | 10.81 / 10.86 / 10.99 / 11.08 ms | 0.26 / 0.21 / 0.08 / 0.01 ms | |

**The control reads, but not as it did on the Demo page.** A fixed load of paths adds 3.64–3.99 ms on a blank page, and 1.46–1.58 ms over the bodies, in every round. So on this thread costs do not simply add: Skia batches and clips draws by what is around them. A row above reads what leaving that part out saves in this scene. It is not the part's cost alone. The order of the parts holds, since every reading is a removal in the same scene, stable to ±0.1 ms.

**A finding about the clip.** Leaving the fill out makes the frame *dearer*, 48.05 ms and 20 frames a second, in every round. With no fill drawn first, the small draws under the contour clip (the speckle's squares, the mosaic) become very expensive. That points at the clip as the mechanism behind both the clip's own cost and the speckle's. The kit always fills, so this is not a configuration anyone runs.

**Items, for each part that saves more than the spread:**

- [B-79](B-79-outline-stroke-on-the-render-thread.md): the outline's stroke, 4.0 ms.
- [B-80](B-80-contour-clip-on-the-render-thread.md): the clips. The contour clip saves 2.1 ms and the highlight's clip 0.6 ms, the same mechanism.
- [B-81](B-81-speckle-on-the-render-thread.md): the speckle, 1.3 ms.

The mosaic, dead pixels, flow lines and shadow each save under 0.3 ms and get no item.
