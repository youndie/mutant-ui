---
id: B-86
title: "A tall body rasterises its whole height every tick"
status: done
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

## Findings (2026-11-16): the image is cut to the window

**The premise, re-measured first.** On `main`'s code (`e643d81`, this branch's first commit, which changes no code), after B-77, B-87 and B-88, the 3 000 dp card cost 92.65 ms per tick (86.10 on a second run), down from B-69's 177.5 and already under the 125 ms between ticks. The rasterising still took 700 ms a second, most of it off the screen, so the item went ahead as the owner asked.

**The decision.** The first candidate: the image drawn ahead covers the window and half a window more on each side, cut on the whole image's pixel grid (`rasterisedBody`'s `band`). Rejected:
- drawing tall bodies as vectors: it moves the cost back to the render thread, which B-82 took it off;
- capping the field: B-69's rejected alternative.

How it works:
- A body that fits in the band rasterises exactly as before.
- Scrolled past its band between two ticks, the body's layout callback asks for its drawing again ([`BodyView.place`](../../mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt)). It draws its vectors until the next tick's image covers where it stands.
- A body under a clock that stands (B-83) keeps its one image whole, since there is no next tick to follow a scroll.

**Measured.** `tallBodyBench`, Apple M1, macOS 27.0.1, window 600 × 814. Two pairs, main then branch, the same day on the same host. Logs: `logs/b-86/2026-11-16-{before,after,before2,after2}.log`; `main`'s code at `e643d81` and `ba3e291`, the branch at `f647f32`.

| Body | main, cost per tick | branch, cost per tick | Round spread |
|---|---|---|---|
| 360 × 200 | 11.69 / 13.37 ms | 15.53 / 13.65 ms | 8.6–17.2 ms |
| 360 × 1 000 | 26.64 / 26.26 ms | 28.05 / 27.44 ms | 21.5–30.7 ms |
| 360 × 3 000 | 92.65 / 86.10 ms | 30.12 / 28.08 ms | 26.5–95.3 ms |

The 3 000 dp card's rasterising fell from 700 to 190 ms a second. The 200 dp card's paired difference was +3.84 ms in the first pair and +0.28 ms in the second. Both are inside the spread of its rounds, so the effect is below resolution: under the spread of about ±3 ms. Its image is the same size as before.

**Pixels.** Skia rasterises an edge a little apart on a smaller canvas. A cut image differs from the whole one's pixels on 0.002–0.019 % of them by more than 16 levels (worst 41–56), on edges inside the band, not at its cut. That is under the tolerance B-82 holds an image to against the vectors. The 200 dp body's parity and goldens are untouched (`./gradlew check`).

**Tests.** `TallBodyBandTest`:
- a cut image holds the whole image's pixels at densities 1, 2 and 2.625;
- a tall card rasterises under two thirds of its whole bytes;
- scrolled a window and a half between two ticks, it draws what its vectors draw: before, in the next frame, and three ticks after.

**Mutants.**

| Mutant | Result |
|---|---|
| band ignored | killed |
| coverage always true | killed |
| no redraw on the scroll | killed |
| cut off the grid by a row | killed |
| no rebuild of the band within a tick | survived |
| no spare around the window (`BAND_SPARE = 0`) | survived |

The two survivors cost time, not pixels:
- without the rebuild, the first tick after a long scroll draws vectors and the second draws its image;
- without the spare, any scroll draws vectors until the next tick.

Neither picture is wrong, so no test holds them; the bench measures the spare.
