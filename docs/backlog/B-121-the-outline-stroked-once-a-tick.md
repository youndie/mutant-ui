---
id: B-121
title: "A body's outline stroked once a tick"
status: wip
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-121 — A body's outline stroked once a tick

Found by [B-120](B-120-what-a-body-costs-over-b-119.md): the outline's stroke is about 11.6 ms of B-119's 43 ms a frame on
the calibrated proxy. Its path changes once a tick, and a body drawn in the frame replays its stroke every frame, where
Skia turns the stroke into a fill each time.

- **The decision to test.** Turn the outline's stroke into its fill path once, when the tick's paths are built (Skia's
  `Paint.getFillPath`, round joins, the frame's width), and fill that path each frame. Where Skiko is (desktop, browser,
  iOS); Android keeps the stroke. Rejected: an outline marched from the field at ± half its width, which is another shape.
- **Measured before it runs.** The proxy slowed ×2, in one rotation: B-119's build, this item, B-119's build without the
  outline; `TenCards8` and `Blank`, six rounds of ten seconds, round 1 not counted. It **buys its cost** if every counted
  round is cheaper than B-119's build in ms a frame by more than twice the largest move of `Blank`'s work between the same
  builds.
- AC: every parity test and golden green with nothing raised; a mutant that fills the wrong width fails a test.
