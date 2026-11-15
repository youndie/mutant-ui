---
id: B-69
title: "Measure what a tall body costs per tick"
status: done
priority: P3
size: S
stage: stage-5-product
blocked_by: [B-40]
---

# B-69 — Measure what a tall body costs per tick

Found in [B-40](B-40-qa-pass-and-real-window.md) (F20), suspected only. The distance field grows with the body's area; a 360 × 3 000 dp card cost 64–78 ms per tick in the test runtime against 16 ms for 360 × 200, and those numbers include `captureToImage`.

- **The decision.** Measure first, on the real frame clock with B-06's harness, before changing the generator. The rejected alternative is capping the grid now, which changes every tall body's drawing on a suspicion.
- AC: the cost per tick of 200, 1 000 and 3 000 dp bodies measured in a real window, with a verdict: a new item for a fix, or closed with the numbers.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`

## Findings (2026-11-15): measured; a fix is B-86

**The bench.** `tallBodyBench` (`TallBodyBench.kt`, run by hand) opens a real window, 600 × 900 on an Apple M1, macOS 27.0.1, JDK 25.
- Scenes: one resting card 360 dp wide, 200, 1 000 or 3 000 dp tall, in a scrolling column on the theme's running clock, and a blank page as the control.
- Per scene, 8 s after a 1 s settle, it reads: the whole process's CPU (every thread: the frame, the builds and rasterising ahead, the render thread), the body frames built, the render thread's CPU a frame, the rasterising's own time and the main thread's busy share.
- Scenes rotate each round. Six rounds; round 1 is not counted.
- A tick's cost is the process CPU a second above the blank page's of the same round, divided by the body frames built that second (8, the resting tick).

Every number is from one run on one host: `logs/b-69/2026-11-15-desktop.log`, commit `b012005`.

| Body | CPU above blank | Cost per tick (rounds 2–6) | Rasterising a second | Render thread a frame |
|---|---|---|---|---|
| 360 × 200 | 65.7 ms/s | 8.22 ms (7.11–9.62) | 37.4 ms/s | 0.44 ms |
| 360 × 1 000 | 181.2 ms/s | 22.67 ms (21.99–23.86) | 160.3 ms/s | 0.39 ms |
| 360 × 3 000 | 1 418.8 ms/s | 177.52 ms (162.73–195.67) | 949.6 ms/s | 4.35–6.70 ms |
| blank | — | — | 0 | 0.40 ms |

**Reading.** A body five times taller costs 2.8 times as much per tick, and fifteen times taller 22 times as much: past 1 000 dp the cost grows faster than the area. Two thirds of it is rasterising the image drawn between ticks (B-82), which covers the whole body, most of it off the screen. At 3 000 dp a tick costs more than the 125 ms between ticks. The window still drew 60 frames a second, but the process spent 1.4 CPU-seconds a second and the render thread's frame grew tenfold.

**The test runtime's 64–78 ms of F20** included `captureToImage` and did not run the image path. The real window's 177 ms is the figure that counts.

**Verdict.** A new item for a fix, [B-86](B-86-tall-body-rasterised-whole.md): rasterise what the window shows, or draw tall bodies as vectors. The generator is not capped, as this item decided. No positive control was needed for the reading: the effect is 8–20 times the blank page's level, far outside the round-to-round spread (±10 %).
