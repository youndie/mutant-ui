---
id: B-82
title: "Rasterise a body once per tick, and draw the image between ticks"
status: open
priority: P1
size: L
stage: stage-5-product
blocked_by: [B-78]
---

# B-82 — Rasterise a body once per tick

Found by [B-79](B-79-outline-stroke-on-the-render-thread.md)'s refutation, on [B-78](B-78-the-bodies-on-the-render-thread.md)'s numbers. A body's picture changes only at a tick, eight times a second at rest. Yet Skia's render thread rasterises its contour, clip, speckle and highlight on every frame, sixty times a second: 11 ms a frame for the Library screen's bodies ×3, 15–16 ms on the Demo page. Turning the stroke into geometry saved nothing, because the cost is the rasterising itself.

- **The decision to measure.** Draw each body's picture into an image once per tick, off the main thread where B-75 already builds the tick's frame, and let the frames between ticks draw that image. Measure on B-78's bench, with its control, and with the screen to the bench alone:
  - the render thread's CPU per frame;
  - the cost of rasterising on the building thread;
  - the memory per body at the window's density.

  Then compare the pixels with the vector drawing: the goldens and parity, and what the real window shows on Metal. A body that slides, pours or boils keeps drawing as vectors. Rejected: simplifying the contour, which changes what the generator draws (D3).
- **Open, and decided by the numbers:**
  - whether the image is drawn at the window's density or the kit's;
  - how a body larger than the screen (B-69) is bounded;
  - whether B-80 (the clips) and B-81 (the speckle) still matter once a body is an image.
- AC:
  - Render CPU per frame before and after, with the control.
  - The building cost and the memory.
  - Parity and goldens hold, or each gap is a number with its cause.
  - A test fails when the image is not reused between ticks.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyDrawing.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/bench/BodyRenderBench.kt`
