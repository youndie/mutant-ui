---
id: B-89
title: "At 12 ticks a second a third of a phone's ticks are built in the frame"
status: done
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

## Findings (2026-11-16): the frame was ready, and nothing found it

**The premise, re-measured first**, on `main`'s code after B-86 (`logs/b-89/2026-11-16-before/`): `TenCards12` skipped 1.70 % of vsyncs, with a main-thread p95 of 26.86 ms. The host now also counts what the kit's probes say. In ten seconds, 450 of 1 200 body ticks were built in the frame at 12 ticks a second, and only 40 of 800 at 8 ticks: the first ticks after the start. Rasterising ahead took 10–13 ms at p50 on either clock. The builds were not late.

**The cause.** A tick's frame is built ahead for `restTimeMs() + stepMs` and taken when the clock reads `restTick * stepMs`, by equality (B-75). At a step of 125 ms the two are the same number. At 1000/12 or 1000/24 ms they differ in the last bit on 399 of 1 200 ticks, a third. On those ticks the ready frame was never found, and the tick was rebuilt on the main thread. The kit's default 8 ticks a second never met it, so neither did the desktop or the browser.

**The fix.** The clock gives the next tick's time as the same product it will read then, `nextRestTimeMs() = (restTick + 1) * stepMs`.

**Measured after**, at `7021666` on the same phone the same day (`logs/b-89/2026-11-16-after/`), by B-49's amended rule:

| Scene | vsyncs skipped | main-thread p95 | body ticks built in the frame | Verdict |
|---|---|---|---|---|
| TenCards12 | 1.70 % → **0.00 %** | 26.86 → **3.09 ms** | 450 → 60 per 10 s (the start's) | GREY → **GREEN** |
| TenCards8 | 0.00 % → 0.00 % | 3.16 → 3.10 ms | 40 → 40 | (green) |
| List100 | 0.00 % → 0.00 % | 13.32 → 11.76 ms | 1 420 → 1 171 | GREEN → GREEN |

The control read 5.00 ms both times. Thermal status was 0 throughout. `List100` still builds most of its ticks in the frame: rows entering the screen draw their first ticks there (B-50). It skips nothing, so it is left as it is.

**Tests.**
- `MutantClockTest`: at 8, 12, 16 and 24 ticks a second, over 2 000 ticks, the predicted time is the time the clock reads at that tick, bit for bit.
- `TickFramesAheadTest`: at 12 and 24 ticks a second, a card builds no tick in the frame in its second second.

Two mutants were tried, and both were killed:
- the surface's `t + clock.stepMs` restored;
- the clock's `restTimeMs() + stepMs`.
