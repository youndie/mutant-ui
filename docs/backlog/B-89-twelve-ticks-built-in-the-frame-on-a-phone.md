---
id: B-89
title: "At 12 ticks a second a third of a phone's ticks are built in the frame"
status: wip
priority: P2
size: M
stage: stage-5-product
blocked_by: [B-49]
---

# B-89 — At 12 ticks a second a third of a phone's ticks are built in the frame

Found by [B-49](B-49-frame-cost-on-a-pixel-6a.md) on the owner's Pixel 6a, 2026-11-16 (`logs/b-49/2026-11-16-pixel6a/`).

Ten resting cards under the Tape at the brief's 12 ticks a second:
- 6.8 % of frames took 17–30 ms of main-thread work, about a third of the ticks;
- 1.51 % of vsyncs were skipped.

At the kit's own 8 ticks a second the same screen skips none, and its main thread stays under 3.3 ms at p95. A resting tick's frame is built ahead on a background thread (B-75), and its image rasterised with it (B-82). When it is not ready at the tick, the frame builds it on the main thread. On the phone, at 83 ms between ticks, it often is not ready.

- **The decision and its reason.** To be decided when the item is taken, after a profile of the phone at 12 ticks. Candidates:
  - start the build ahead earlier;
  - draw the previous tick's image once more rather than build in the frame;
  - bound the work ahead per tick.

  The cause is not known yet: the builds' thread pool, the rasterising's share or the ordering of the ten bodies are each possible.
- AC:
  - `TenCards12` on `scripts/device-bench.sh` skips under 1 % of vsyncs, with its main-thread p95 ≤ 16.7 ms.
  - `TenCards8` and `List100` are no worse.
  - The control reads its 5 ms.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `bench-android/src/main/kotlin/io/github/youndie/mutant/bench/android/BenchActivity.kt`
