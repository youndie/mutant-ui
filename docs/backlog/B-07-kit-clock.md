---
id: B-07
title: "The kit clock: one per screen, in ms, ticks of 125 and 83 ms, pausable"
status: done
priority: P1
size: S
stage: stage-1-materials
blocked_by: [B-01]
---

# B-07 — The kit clock: one per screen, in ms, ticks of 125 and 83 ms, pausable

Every body is a function of the cassette seed, its key and the time; the engine keeps one clock in ms, scaled by speed and stopped on pause, rests at 125 ms ticks and runs a pressed body at 83 ms (research §1.2, D6).

- **The decision and its reason.** `MutantClock` provided once per screen through a composition local: elapsed ms from a frame clock, `paused`, `speed`; `tick(t, hot)` and `boilPhase` as the engine computes them (`floor(t / tick + 0.001) mod 4`). Only visible instances subscribe. No component reads system time.
- Rejected: a clock per component (the brief's "one cassette per screen").

- AC: two bodies on one screen step on the same tick; a paused clock draws no new frame; a test reads the boil phase at the engine's boundaries; a mutant in the 0.001 guard or the 1.5 factor is caught.
- Anchors: `reference/design-system/mutant-engine.js`, `reference/design-system/mutant-tokens.json`

## Result (2026-11-14)

- `MutantClock` (`io.github.youndie.mutant.clock`): time in ms stepped by the engine's `dt` (`min(1000, now − last)`, 16 on the first frame, times `speed`), standing while `paused`; `restTick` and `hotTick` are snapshot state that changes only on a tick, so readers recompose 8 and 12 times a second; `restTimeMs()` and `hotTimeMs()` are the engine's `ts` and `tsH`; `frozen` is the engine's `data-mu-t`. `ProvideMutantClock` drives it with `withFrameNanos` only while it has readers (`readMutantClock`) and is neither paused nor frozen; outside a provider the local is a clock frozen at 5000 ms. The boil phase lives beside the generator's `frame` (`gen/Frame.kt`), one formula for both. Everything `internal`; the public API is B-09's.
- The expected values are the engine's own expressions, cut out of `mutant-engine.js` by `scripts/gen-goldens.mjs` and evaluated in Node (`GoldenClock.kt`): 546 times — around every resting and hot boundary of six seconds, and 64 spread over a minute — and four frame steps. `MutantClockTest` (4) passes on the JVM, the iOS simulator and wasm; `MutantClockComposeTest` (3, desktop): two readers draw the same ticks on every frame, at most one draw per tick; a paused clock draws nothing for 60 frames and resumes from where it stood; a frozen clock draws once, a clock nobody reads never moves.
- Mutants, each fails a clock test by name: the boil phase without its 0.001; the hot tick's 1.5 at construction and, after a first survivor, on the path each frame takes (`setTime`); the clamp 1000 → 2000; the first step 16 → 0; time moving while paused; frames asked for with no reader.
- Not covered here: bodies drawn from the clock ([B-10](B-10-mutant-surface.md)); visibility beyond composition (a body scrolled out of a lazy list leaves the composition and stops reading; one merely off screen in a long column still reads); the theme's reduce-motion switch, which will set `paused` ([B-09](B-09-theme.md)).
