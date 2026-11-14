---
id: B-75
title: "The generator's tick frame runs on the main thread, 60–80 ms per tick on the Components page"
status: done
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-72]
---

# B-75 — The generator's tick frame runs on the main thread

Found by [B-72](B-72-frame-cost-on-the-jvm.md). On the resting Components page, the frame a tick falls on costs 60–80 ms of main-thread work; the frames between ticks cost 0.1 ms. Ticks come eight times a second, so 27 % of the intervals exceed 33 ms. JFR puts 77 % of the event thread in `gen.frame`, called from the surface's draw: `distanceField` 58 %, `march` 15 %. Every body rebuilds its field and outline on every tick, in the frame.

- **The decision and its reason.** A body's next tick is known before it comes: its time is `(tick + 1) · step`, a pure function of the instance. So each surface builds the coming tick's frame off the main thread ahead of it, and at the tick it takes what is built. This is the pattern [B-50](B-50-first-frame-off-the-frame.md) set for the first build. A frame not ready in time is built in the frame as today, so nothing is ever skipped or stale. Out of scope, and rejected: a coarser field or fewer ticks, which change what the design draws (D3); and caching frames across a loop, whose period is not established.
- **Unchanged.** Reduce motion (the clock stopped), a pressed body's hot tick, and parity fixtures and goldens, which run on a frozen clock: these keep building in the frame, byte for byte as now.
- AC:
  - B-72's bench, same stand: the resting and wheel Components scenes reach the green line (work p95 ≤ 8 ms, intervals over 16.7 ms under 1 %), or the remaining cost is profiled and named.
  - A test shows that the frame taken at a tick is identical to the one built in the frame.
  - A mutant that drops the hand-over (always building in the frame) fails a test.
  - Goldens and parity stay as they are.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/Frame.kt`, `sample/src/desktopTest/kotlin/io/github/youndie/mutant/sample/bench/FrameBench.kt`

## Results, 2026-11-14

`FrameAhead` in `MutantSurface.kt` builds the next resting tick's frame on `Dispatchers.Default` and hands it over only for the same instance and the same time. `FieldCache.settledFor` holds the build back until the frame would only read the cache: the static layers, and all four boil phases where the body boils. A shared cache is therefore never written from two threads.

**Measured** with B-72's bench at `0200a62` on the same stand and window, 6 rounds, round 1 not counted: [the log](../../logs/b-75/2026-11-14-desktop.log), [every frame](../../logs/b-75/2026-11-14-desktop/frames.csv). The before numbers are B-72's run at `7dc98a6`: same host, a different run.

| Scene | frames/s | interval p95 | > 33 ms | work p95 | Verdict before → after |
|---|---|---|---|---|---|
| Components, at rest | 31.5 → 59.6 | 85.0 → 25.2 | 26.9 → 0.47 % | 66.8 → 12.9 | RED → **GREY** |
| Components, wheel | 29.2 → 59.0 | 89.0 → 28.1 | 28.9 → 1.09 % | 67.4 → 13.8 | RED → **GREY** |
| Components, «Чистый сигнал» | 33.4 → 60.2 | 84.2 → 21.7 | 24.8 → 0.10 % | 69.2 → 11.4 | RED → **GREY** |
| Demo, Library alone | 53.1 → 60.2 | 35.4 → 22.9 | 8.8 → 0.10 % | 14.0 → 2.3 | RED → **GREY** |

The control read 10.00–10.04 ms against 10 ms. The wheel scrolled 15 092–16 470 px a round; before, it was 9 370–10 744 at the same events, because the frames now keep up.

**Why still grey, not green**, two reasons:

1. **Frame-to-frame jitter.** At 60 Hz the intervals sit on 16.67 ms, and 36–43 % of them land a hair over 16.7. Every scene here does that, Settings at 1.7 ms of work included. B-72's line counts them as late. It is a limitation of the line, recorded here, not moved.
2. **Work p95 of 11–14 ms.** Profiled after the change ([profile](../../logs/b-75/2026-11-14-profile-components-rest.txt)), the event thread holds a quarter of its samples before (253 against 975 over the same time). Of these:
   - `gen.frame`, 30 %: frames still built in the frame. These are a body's first ticks, before its cache settles, and ticks whose frame was not ready in time, with some fifty bodies sharing eight cores.
   - The Tape, 10 %.
   - Paint and rect drawing, 5 %.

**Tests.** `TickFramesAheadTest`:
- once settled, the ticks are taken ahead;
- tick for tick, the pixels are identical to building in the frame;
- a clock jumping three seconds draws its own time.

Mutants, each aimed at a named test and each killed:
- never taking the ready frame;
- building the frame of the wrong tick;
- dropping the time check.

Goldens and parity run on a frozen clock and are unchanged.
