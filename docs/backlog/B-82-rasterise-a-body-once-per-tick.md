---
id: B-82
title: "Rasterise a body once per tick, and draw the image between ticks"
status: done
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

## Results, 2026-11-15

**What changed.** B-75's job builds the next resting tick's frame ahead. It now also rasterises the body into an image (`rasterisedBody`), with the inputs of the moment it was prepared: the look's colours, the theme, the state, focus, density, direction, cassette, stage, wear and dead pixels. At the tick, the body draws that image for every frame until the next tick (`FilterQuality.None`, at whole device pixels). The image is drawn only if the inputs are still what they were. A body that pours, slides or is heated, a frozen clock, a tick whose image is not ready, and the first ticks before the field cache settles all draw as vectors, as before. `LocalMutantBodyAsImage` is on by default; tests and the bench switch it off to compare.

**Measured on `bodyRenderBench`**, the Library screen's bodies ×3 with no Tape. One run, scenes interleaved, six rounds, round 1 not counted ([log](../../logs/b-82/2026-11-15-bodies.log)):

| | Render CPU / frame | frames/s | main thread busy | RSS |
|---|---|---|---|---|
| vectors | 20.97 ms (20.44–21.40) | 41.4 | 10–11 % | 396–487 MiB |
| images | **1.67 ms** (1.45–1.78) | **60.0** | 7–8 % | 454–481 MiB |

- **The control** reads alike everywhere: 3.25–3.62 ms on a blank page, 3.31–4.07 over the vectors, 2.96–3.41 over the images.
- **Rasterising** costs 3.0–3.5 ms a body on the background threads, about 0.6 s of CPU a second spread over the other cores. The total CPU is about what the render thread spent before; it moved off the bound.
- **Memory.** The largest image is 943 KiB. RSS shows no growth over the run.
- **Stand.** The Mac ran on battery (80 %, discharging). On battery the vectors cost about twice B-78's 11 ms on mains, so these numbers compare with each other, not with B-78's.

**The sample's Demo page**, `FrameBench`, runs alternating vectors and images, three rounds each ([logs](../../logs/b-82/)):

| | vectors 1 | images 1 | vectors 2 | images 2 |
|---|---|---|---|---|
| Library alone, render CPU / frame | 9.59 ms | **1.61** | 9.51 | **2.29** |
| Demo page | 19.95 | 19.45 | 19.99 | 11.27 |

- **Library alone** gains in both image runs.
- **The Demo page does not gain**, by construction. Its Settings screen opens a dialog, and a dialog holds the screen's clock (`MutantDialog`: `screen.holds++`). With no next tick, no image is ever built, and the bodies keep their vectors while the dialog's own clock asks for frames. → [B-83](B-83-a-held-clock-draws-vectors.md)
- **The two image runs disagree on the Demo page** (19.45 against 11.27 ms) with identical code. Between them the battery fell from 80 to 67 %, and even the blank page's main-thread work doubled. Across runs on battery the stand is not stable: the A/B that counts is the one within a run.

**Tests.** `BodyAsImageTest`:

- once the cache settles, resting ticks are drawn from an image, rasterised once per tick;
- the image draws what the vectors draw.

The second needs an explanation. An image is rasterised apart and then drawn, while a vector body is recorded and replayed, so their anti-aliased edges round apart. Measured: a level on about 1 % of pixels, and more than 16 levels on 0.013–0.03 % of pixels, at densities 1 and 2. The ticks before the cache settles, which are vectors on both sides, differ by 0. The test holds the image as goldens are held: at most 0.05 % of pixels more than 16 levels apart, in «Эфир» at densities 1 and 2 and in «Копия». «Копия» is in it because on «Эфир»'s dark fill the speckle's ink stays under 16 levels.

**Mutants.** Killed:
- the image a pixel off;
- the image without its speckle (by the «Копия» pass);
- the image never used.

Survived as equivalent: the image's picture drawn at a later time. A resting body's fill and decor do not depend on time.

B-75's and B-50's tests switch images off: they hold the frame and the first build, and the image is held here.

**Not built here** (a running clock covers the gain measured):
- an image under a clock that is held or paused (B-83);
- B-80 and B-81, which wait for this item. Once a resting body is an image, its clip and speckle cost the background threads, not the render thread, so their premise is gone for resting bodies. They stay open for bodies drawn as vectors, and their numbers should be re-read before they are worked.
