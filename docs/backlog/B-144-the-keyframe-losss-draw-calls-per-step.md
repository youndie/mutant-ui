---
id: B-144
title: "The keyframe loss's draw calls per step"
status: done
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-144 — The keyframe loss's draw calls per step

Found by [B-143](B-143-the-keyframe-loss-reads-its-blocks-without-copying-strips.md): a running keyframe loss still holds
the Pixel 6a's Chrome at 8–20 frames a second, a second in every nine on the resting Components page, after the
whole-strip copy left its profile. What remains is the blocks' drawing itself: about 360 ms of a loss in
`StripPicture.drawPart`, through `SkiaBackedCanvas.drawImage` into `wasm-to-js`
(`logs/b-143/2026-11-19-profile-fix.txt`). A step draws each block twice (read out, written in) on every strip it
meets, each a call from Kotlin into Skia, with a new bitmap and canvas per block.

- **The decision.** To be decided from the numbers; first count what a step does: draw calls, bitmaps made, and their
  pixels, per step on the Components page's loss at the phone's density, and where in the step the time goes (a profile
  split by `drawPart`'s read and write).
- Candidates, none chosen: a step drawn as one pass over the strips (each strip's blocks in one canvas save, read from
  one frozen copy of that strip where the blocks do not overlap); blocks kept as one picture recorded per step and
  drawn once; fewer, wider strips.
- AC: the count and the split, each with its log; a route measured by B-142's protocol on the phone against `main` with
  the loss-free arm as the floor.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/StripPicture.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`

## Findings

### Iteration 1, 2026-11-19: the count and the split

**The count** ([log](../../logs/b-144/2026-11-19-step-count.txt), from `moshPlan` and `advance`'s own loop, the sample's
360 × 200 dp loss at density 2.625, the first eight losses of the default cassette; a scratch test, not kept): a block
once started is dragged again at **every** later step, so a step draws up to 128–152 blocks, all of them by the middle
of the run. Each block is one read (blocks lie on the 16 dp rows, so one strip) and 1.7–1.8 writes (the pull moves it
across a strip's edge), with a new bitmap and canvas. Over one loss: 690–1232 blocks drawn, **1901–3381
`drawImageRect` calls**, as many part bitmaps, 2.2–3.7 M part pixels, in 8–12 steps.

**The split** ([profile](../../logs/b-144/2026-11-19-profile-split.txt), the Pixel 6a in Chrome, 29.0 °C, 12 s, `main`
with `drawPart` cut into its read, its write and the part's allocation as three functions kept from inlining; a worktree,
not pushed): of 644 ms under the loss, **405 in `drawPart`: 206 reading, 189 writing, 9 allocating parts**; 10 drawing
the picture; the rest the start's snapshot and the bodies. About 0.2 ms a read and 0.1 ms a write: the cost is per call
into Skia, as B-143 found, not per pixel or per allocation.

So a route has to take the calls out, not make each cheaper. Two, with what each costs:

- **A. The band's pixels in Kotlin.** The band kept as one premultiplied `IntArray` (945 × 504 px at the phone's
  density, about 480 k pixels); a block's drag, the clean zones' restore and the tint done on the array; the picture
  handed to Skia once per step (one bitmap, installed and marked finished, so drawing it each frame copies nothing). A step
  would then make two or three calls instead of 200–400. It moves `StripPicture` and the loss's drawing whole (size M–L).
  Its risk is the pixels: a part is drawn by source-over, and the band has partly transparent pixels at the antialiased
  edges of its cleared blocks (2.625 is no whole density), so source-over has to be done in Kotlin; whether to Skia's
  rounding is unknown until measured, and a parity cell or golden may move by a channel at those edges.
- **B. Fewer calls, same model.** One scratch part and canvas reused for every block, and B-143's subset read. B-143's
  profile bounds it: `drawPart` went from 472 to about 360 ms with the subset, and the stall did not move. Not expected
  to move it now.

## The question

Take route A as the next item (M–L, goldens may move by a channel at the cleared blocks' edges, recorded as the
departures rule asks), or leave the loss as it is (a second in nine at 8–20 frames on the resting Components page in a
phone's browser; desktop, Android and iOS unmeasured here)? Route B is not recommended on B-143's numbers.

## The decision

The owner, 2026-11-19: «Yes, route A». It goes on as [B-145](B-145-the-keyframe-losss-steps-off-the-slow-path.md), which
first checks what route A's own upload would cost in the browser, before anything is built on it.
