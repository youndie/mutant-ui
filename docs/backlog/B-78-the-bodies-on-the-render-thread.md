---
id: B-78
title: "The bodies' drawing is most of the render thread's frame on the Demo page"
status: open
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
