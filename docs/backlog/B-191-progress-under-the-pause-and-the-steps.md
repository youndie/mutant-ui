---
id: B-191
title: "The progress band under the pause and under the steps"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-191 — The progress band under the pause and under the steps

Under the pause the indeterminate band stands at tick 40, which leaves the track empty at 29 of 177 widths (F-progress-05). `steppedMotion` does not step the band and the reel (F-progress-07). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 13 and 14 ([ошибка]).

- **The decision and its reason.** Under the pause the band stands centred on the track, so the track is never empty. Under `steppedMotion` the band and the reel step at the 125 ms tick: the reel 30° a tick, the band 12 dp a tick. By default they keep moving on every frame at the same speed (D22).

- AC:
  - under reduce motion, at every width from 48 to 600 dp, the band is centred on the track
  - under `steppedMotion`, the band's position and the reel's angle change only at 125 ms ticks, by 12 dp and 30°
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantProgress.kt`

## Result, 2026-11-23

`components/MutantProgress.kt` only.

- **The pause.** Under reduce motion the band stands centred on the track, `x = (track − 96) / 2`: its middle, where
  the solid half starts, at the track's middle, so on any track the solid half shows (on one under 96 dp, from the middle
  to the end). The reel stands where the clock stopped, as before (D7).
- **The steps.** The band and the reel read one count of 125 ms ticks (`progressTicks`): the scene's tick, whole, under
  `steppedMotion` and on a frozen clock (the engine's `data-mu-t`), so they step 12 dp and 30° a tick as the engine's
  `stageStep` draws them; by default the time now over the tick (`MutantMotion.TICK_MS`), read on every frame, so they move
  at the same 96 dp and 240° a second without steps (D22). The research's D22 and the B-27 row of §1.10 say so.
- **What moved.** No golden or parity cell: the parity cells draw on a frozen clock at 5 s, where the time over the tick
  is the scene's tick 40 either way (`ProgressParityTest` unchanged), and no golden draws a paused band. Nothing enters
  the departures registry: a change in live behaviour only.

**Frames looked at** (throwaway sheets, deleted): the paused band at 24, 32, 48, 64, 72, 100, 144, 160, 200, 272, 384, 400
and 600 dp, «Эфир» and «Копия» at density 1 and 2; 24 frames of the running band and reel, stepped and not, at density 1
and 2. Before: the track empty at 24, 64, 144 and 384 dp and only the faint quarters at 32, 72, 160 and 400; the band and
the reel changing one frame in eight with `steppedMotion` off. After: the solid half from the middle of every track; by
default every frame moves the band about 1.5 dp and turns the reel about 4°, and under the steps one frame in eight
moves 12 dp and 30°.

**Tests** (`mutant-core/src/desktopTest`):

| AC | Test |
|---|---|
| under reduce motion, at every width from 48 to 600 dp, the band is centred on the track | `ProgressPauseAndStepsTest` «under reduce motion the band stands centred on the track at every width from 48 to 600 dp»: every whole width 48…600 dp at density 1 («Эфир»), and 48…500 dp in 13 dp steps at density 2 («Копия»); the solid acid run starts at the track's middle and ends 48 dp on or at the track's end, read to a fraction of a pixel. Off by at most 0.5 px against a margin of 1.5. On the code before, 588 widths off (the run from 0 at 48 dp, and so on) |
| under `steppedMotion`, the band's position and the reel's angle change only at 125 ms ticks, by 12 dp and 30° | `ProgressPauseAndStepsTest` «stepped, the band and the reel step 12 dp and 30 degrees a 125 ms tick, by default every frame»: 64 frames at density 2; stepped, the band moves on exactly 8 frames, 7 or 8 frames apart, each 12 dp, and the reel turns on the same frames, 30° by its holes' centroids; by default the band moves 1.52–1.55 dp on every frame (1.536 expected, margin 0.35) and the reel about 3.84°, and the band travels 63 × 1.536 dp. On the code before, the default run moved 0 dp on seven frames of eight |
| goldens and parity cells at rest unchanged | `ProgressParityTest` and every golden as before |

`ProgressBehaviourTest`: «the band runs 12 dp a scene tick …», «the reel turns 30 degrees a tick …» and «with no width to
fill …» now run under `steppedMotion` (sampled 125 ms apart, a default run moves by the frames in between, 112 or 128 ms);
the paused band's end at 200 dp is 147 (centred) where it was 183 (tick 40).

**Mutants**, each after the commit, the aimed test named among the failures in the JUnit XML, the file restored with
`git checkout`:

| Mutant | Killed by |
|---|---|
| the paused band a third of the way, `(track − BAND) / 3` | «… centred …»: the solid run from 32 at 48 dp, and so on at every width |
| the steps inverted, `if (clock.frozen \|\| !steps)` | «stepped, …»: the stepped band moved 1.52–1.55 dp on every frame |
| the reel 24° a tick, `REEL_STEP = 24.0` | «stepped, …»: 23.9–24.1° a tick |

**The ACs as written hold**; the second AC's test also holds the default's every-frame movement, since the code before
already stepped and a stepped-only test would have passed on it.

**Gates on this Mac:** `make check`, `make docs-against BASE=origin/main`, `./gradlew check` (desktop, Android, the iOS
simulator's tests, wasm), `./gradlew :mutant-core:compileKotlinIosArm64`.

**Not verified.** No phone and no real browser: Android, iOS and wasm only as `./gradlew check` runs them. The reel's
turn on every frame was read at 60 frames a second of the test clock only; on a 120 Hz screen it turns 2° a frame.
