---
id: B-120
title: "What a body's parts cost, over B-119"
status: wip
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-120 — What a body's parts cost, over B-119

After [B-119](B-119-the-still-image-marked-finished.md) ten cards run at 23.3 frames a second on the calibrated proxy,
about 43 ms a frame, and the main thread is Skia's for 63 % of it ([the profile](../../logs/b-119/2026-11-18-proxy-x2-profile.txt));
the decorations are about 4.4 ms of it. Skiko's module carries no names, so the profile cannot say which drawing costs.

- **The decision to test.** By leaving out one part at a time from B-119's build (`LocalMutantSkipBodyParts`, as B-78 and
  B-111): the outline, the highlight, the shadow, the clip to the contour, the flow lines. No code changes; a measurement,
  to aim the next cut.
- **Measured before it ran.** The proxy slowed ×2, in one rotation, B-119's build and the five; `TenCards8` and `Blank`, six
  rounds of ten seconds, round 1 not counted. A part **costs** what leaving it out saves in ms a frame if every counted
  round saves more than twice the largest move of `Blank`'s work between the same builds; under that it is below
  resolution.
