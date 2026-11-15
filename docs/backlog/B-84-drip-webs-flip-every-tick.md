---
id: B-84
title: "Neighbouring drips' webs zip open and shut every tick at rest"
status: done
priority: P2
size: M
stage: stage-5-product
blocked_by: []
---

# B-84 — Drip webs flip every tick

Seen by the owner in the sample on 2026-11-15: two drips that hang close together stick and come apart at every step of the animation. The design's engine does the same, so this is a fix to what the design draws ([D15](../research/research-architecture.md)), not a port defect.

## Findings (2026-11-15)

A scratch test measured, for every adjacent pair of drips, the web's depth: how far below the body's bottom the field stays inside along the pair's midline. It covered the 18 golden instances that have drips, over 480 ticks (a minute at rest, cassette 5).

- **Tick to tick, the web jumps 3 px or more on 10 % of pair-ticks** (6 450 of 65 144), up to 19.5 px (`field/b`), 9.9–17 px on buttons and cards. Only the track and the thumb are free of it.
- **The drips themselves are not moving.** 6 450 of the 6 624 jumps fall on ticks where both drips change length by under 1 px and tip radius by under 0.5 px.
- **The control: the same boil phase.** Comparing each tick with the tick four before it, which has the same phase of the resting boil, gives 529 jumps in 53 566 pair-ticks, about 1 %. That is ten times fewer.
- **Holes** (a closed gap between two merged drips) are rare. Only `fab/b` has them, 0–3 of them, changing 221 times a minute. The visible defect is the web's depth, not a hole.

**The mechanism.** At rest the field gets a tremble of `REST_BOIL · calm` (0.8 px) times a value noise that changes with the boil phase, four phases per 500 ms (`distanceField`, `bk`). On the body's edge the field is a true distance whose slope is about 1, so the edge moves by the intended 0.8 px. Between two neighbouring drips the smooth union (`smoothMin`, blend `4k`) leaves a saddle where the field is nearly flat. The same 0.8 px of noise there moves the zero line by 10–19 px, so the web zips open or shut on every phase change. Drips are placed `(.8 + 9·gp³)·k` apart, so half of the pairs sit within the blend's reach.

- **The decision and its reason.** Bound the edge's displacement rather than the noise: scale the tremble at each grid point by `min(1, |∇d| / g₀)`, with `∇d` the field's slope before the tremble is added. Where the field is a distance (the body's edge, a lone drip) nothing changes. In a saddle the tremble fades, and the web follows its drips' lengths instead of the boil phase.
  - Rejected: spacing the drips apart. It changes every body's shape and the generator's goldens, and webs are part of the look.
  - Rejected: a plain `min` between drips. It cuts the webs off to sharp creases.
  - Rejected: smoothing the field between ticks. The design steps at the tick on purpose.
- AC:
  - The web-depth measure as a kept test: between drips standing still, the web's depth changes by no more than the edge's own tremble per tick, with the same-phase control beside it.
  - The cells it moves are listed as a departure with their numbers before and after; the body's parity elsewhere stays within the floor; the moved goldens are re-recorded and looked at.
  - A mutant that drops the slope factor fails the test.
  - The field's cost per tick before and after on `fieldBench`.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/Field.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/Random.kt`

## Findings (2026-11-15, built)

**The slope factor was refuted, and the merge weight replaced it.** Built as decided, `min(1, |∇| / g₀)` brought the jumps between still drips only from 6 450 to 4 623 of 61 963 at g₀ = 0.5. It did no better (4 808) with the slope taken before the edge's roughness. The saddle's slope is 0.5–2, not near 0: sweeping g₀ took 1, 2, 4 and 8 to 1 939, 933, 609 and 365 jumps. A g₀ of 4 or more damps the tremble on every true edge (slope 1) to a quarter, and the whole body would stop boiling.

**Built instead: damping by the merge.** In `distanceField` each `smoothMin` already knows how much its two shapes blend, `h = max(k − |a − b|, 0) / k`. A point keeps `(1 − h)²` of the tremble, h its strongest merge. That is a drip into the body, or a web between two drips. A point outside every merge, the body's edge and a lone drip's sides, trembles as the engine draws it.

| | jumps ≥ 3 dp between still drips, 60 s, 18 bodies |
|---|---|
| the engine | 6 450 of 61 963 |
| no boil at all (control) | 246 |
| the same boil phase every tick (control) | 252 |
| merge weight, power 1 | 730 |
| **merge weight, power 2 (built)** | **444** |
| merge weight, power 4 | 312 |

Power 2 moves 18 % of the field's points near the edge (|f| < 1) by more than 0.2, against 14 % at power 1 and 20 % at power 4. A contact sheet of three bodies over six ticks showed the same drips and the same boiling top edge.

**Only a running body.** `MutantSurface` passes `steadyWebs = !clock.frozen` to every frame it builds, in the frame and ahead of it (B-75). A frozen clock (fixtures, references, the generator's goldens) draws the engine's field. With `steadyWebs` off the field is bit for bit the engine's: the tremble is multiplied by exactly 1.0. **No parity cell and no golden moves**, so the departures registry stays empty: this departure is of behaviour, recorded as research D18. The AC's "cells it moves" are none, and nothing was re-recorded.

**Cost** (`fieldBench`, `logs/b-84/2026-11-15-desktop.log`, rounds 2–5, Mac, same run): a resting frame costs 3.6 % more summed over the 27 cases (8 308 µs against 8 017), 0.3–5.8 % per body (card-a 536 → 561 µs). The warm/warm2 ruler of the same run is 1.0 %. The extra cost is the blend weight per capsule test.

**Tests.**
- `SteadyWebsTest` (generator): the web-depth measure. It asserts that the engine's webs do jump (more than 1 in 20 still ticks) and that steady ones jump at fewer than 1 in 100.
- `SteadyWebsSurfaceTest`: a running clock's frames are all steady and a frozen clock's none, read through the frame probe, which now also reports the frame's `steadyWebs`.

**Mutants.** All four killed:
- no damping (`kept = 1`): `SteadyWebsTest`;
- the frame built in the tick passing `false`: `SteadyWebsSurfaceTest`;
- the frame built ahead passing `false`: `SteadyWebsSurfaceTest`;
- steady under a frozen clock as well: `SteadyWebsSurfaceTest`.
