---
id: B-75
title: "The generator's tick frame runs on the main thread, 60–80 ms per tick on the Components page"
status: open
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
