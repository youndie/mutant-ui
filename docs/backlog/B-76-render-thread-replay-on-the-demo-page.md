---
id: B-76
title: "Skia's replay on the render thread bounds the Demo page"
status: done
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

## Results, 2026-11-14

Run at `869b571`, B-75 plus this bench; after the rebase onto `main` the same tree is `a4ebf3f`. Measured on B-72's stand and window. Six rounds of ten seconds a scene, round 1 not counted: [the log](../../logs/b-76/2026-11-14-desktop.log), [every frame](../../logs/b-76/2026-11-14-desktop/frames.csv), with a `render_ms` column.

**The instrument.** A sampler on a thread of the bench's own adds up, every 5 ms, the CPU time of every `skiko-dispatcher-to-block-on` thread it has seen; it saw two, because the dispatcher replaces its thread. The cost is read as render CPU per frame drawn. A share of wall time cannot be the measure: on the Demo page the thread is 87–92 % busy and a share cannot grow. The time of one frame alone is meaningless too, because the replay runs asynchronously and its CPU lands in intervals in bursts.

**The control reads.** A fixed load of 1 200 filled paths of 120 points each, which Skia tessellates on the render thread's CPU, adds per counted round:

- on a blank page: 3.40, 3.43, 1.49, 3.29 and 3.51 ms;
- on the Demo page: 3.32, 3.85, 4.46, 3.52 and 3.13 ms.

| Scene | frames/s | render CPU / frame | busy |
|---|---|---|---|
| Blank page | 60.1 | 0.37 ms | 2 % |
| Demo page (header, page Tape, three screens) | 46.5 | 19.79 ms | 91 % |
| Demo page, «Чистый сигнал» (no Tape) | 55.2 | 16.62 ms | 91 % |
| Three screens in a row, rounded clip | 49.7 | 18.58 ms | 92 % |
| The same, no rounded clip | 51.5 | 17.97 ms | 92 % |
| Library alone | 59.6 | 10.14 ms | 60 % |
| Recording alone | 60.1 | 6.21 ms | 37 % |
| Settings alone | 60.3 | 5.69 ms | 34 % |

Same host and same run for every row. B-72 read the Demo page at 34.5 frames a second; with B-75 merged it is at 46.5. Its main-thread work is 0.4 ms at p95, so the bound is the render thread alone.

**Where the render thread's time goes on the Demo page:**

1. **The Tape: 3.2 ms a frame** (19.79 → 16.62 ms). It is drawn four times, once over the page and once over each screen. → [B-77](B-77-the-tape-on-the-render-thread.md)
2. **The screens' rounded clip: 0.6 ms** (18.58 → 17.97 ms). This is 3 %, under the 1.5–4.5 ms spread of the control's own rounds, so it gets no item.
3. **The content of the screens: about 15–16 ms**, scaling with what each screen holds (Library 10.1, Recording 6.2, Settings 5.7 ms alone). The parts of a body (fill, outline, highlight, drips, flow lines, mosaic, dead pixels) cannot be switched off from the sample, so the ablation moves into the kit. → [B-78](B-78-the-bodies-on-the-render-thread.md)
