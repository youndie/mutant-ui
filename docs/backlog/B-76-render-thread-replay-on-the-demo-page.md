---
id: B-76
title: "Skia's replay on the render thread bounds the Demo page"
status: open
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-72]
---

# B-76 — Skia's replay on the render thread bounds the Demo page

Found by [B-72](B-72-frame-cost-on-the-jvm.md). On the Demo page, with its three screens side by side, frames come every 27 ms at p50 while the main thread works 1.9 ms at p95. Skiko replays the recorded picture on its own thread (`skiko-dispatcher-to-block-on`), where JFR finds 898 of 1 050 native samples in `Canvas.drawPicture` and 137 in the flush. That is more than twice the resting Components page's 462. Each screen alone holds 53–60 frames a second.

- **The decision and its reason.** First give the bench what it lacks: the render thread's time per frame, beside the main thread's (B-72 timed only the main thread, and the intervals alone said a frame was late). Then find which drawing costs the replay, by ablation on one screen at a time: the Tape, the mosaic and dead pixels, the drips, the outlines, the clip of the screen's rounded corners. Each cause gets an item of its own with a number before and after. Rejected: guessing at a cause before the replay is timed.
- AC:
  - The render thread's time per frame is in the bench's log, with a control: a known cost of drawing added to one scene reads as such.
  - Each ablation has its own number.
  - There is an item per cause.
- Anchors: `sample/src/desktopTest/kotlin/io/github/youndie/mutant/sample/bench/FrameBench.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantTape.kt`
