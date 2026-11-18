---
id: B-118
title: "What the decorations still cost, over B-117"
status: wip
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-118 — What the decorations still cost, over B-117

After [B-117](B-117-still-layers-once-on-the-surface-grid.md) the kit draws its still layers once and saves about 6 ms a
frame of the decorations' 23 on the calibrated proxy; about 17 remain.

- **The decision to test.** By leaving out one part at a time from B-117's build, as B-111 did from the kit: where the
  rest goes, so the next cut aims at the dearest. No code changes; a measurement.
- **Measured before it ran.** The proxy slowed ×2, in one rotation: B-117's build; it without the flow lines; without the
  speckle; without the mosaic and the dead pixels; without all four; `TenCards8` and `Blank`, six rounds of ten seconds,
  round 1 not counted. A part **costs** what leaving it out saves in ms a frame if every counted round saves more than
  twice the largest move of `Blank`'s work between the same builds; under that it is below resolution.
