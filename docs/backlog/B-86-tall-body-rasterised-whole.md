---
id: B-86
title: "A tall body rasterises its whole height every tick"
status: wip
priority: P3
size: M
stage: stage-6-later
blocked_by: [B-69]
---

# B-86 — A tall body rasterises its whole height every tick

Found by [B-69](B-69-tall-body-tick-cost.md) on 2026-11-15, in a real window on an Apple M1 (`logs/b-69/2026-11-15-desktop.log`). A resting card 360 dp wide costs per tick, over a blank page, all threads:

| Height | Cost per tick | Of it, rasterising |
|---|---|---|
| 200 dp | 8.2 ms | about 4.7 ms |
| 1 000 dp | 22.7 ms | about 20 ms |
| 3 000 dp | 177.5 ms | about 119 ms |

At 3 000 dp the cost is 1.4 CPU-seconds a second, more than the 125 ms between ticks, and the render thread's frame grew from 0.4 to 4.4–6.7 ms. The image drawn ahead (B-82) is the whole body, rasterised at the window's density on every tick, most of it off the screen.

- **The decision and its reason.** To be decided when the item is taken. The candidates:
  - rasterise only the part of the body the window shows, plus the drips' reach;
  - draw a body over a stated area as vectors, as a held clock does (B-83), and measure that against the image;
  - leave the field's grid as it is. The generator's share is the smaller one; capping the grid changes the drawing (B-69's rejected alternative).
- AC: the 3 000 dp card's cost per tick under the 125 ms between ticks, on `tallBodyBench`, with the 200 dp card's cost and its parity unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/bench/TallBodyBench.kt`
