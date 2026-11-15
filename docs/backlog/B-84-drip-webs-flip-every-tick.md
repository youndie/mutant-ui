---
id: B-84
title: "Neighbouring drips' webs zip open and shut every tick at rest"
status: open
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
