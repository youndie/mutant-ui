---
id: B-07
title: "The kit clock: one per screen, in ms, ticks of 125 and 83 ms, pausable"
status: open
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
