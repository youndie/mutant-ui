---
id: B-123
title: "Each body drawn in a layer of its own"
status: wip
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-123 — Each body drawn in a layer of its own

Found by [B-121](B-121-the-outline-stroked-once-a-tick.md) and [B-122](B-122-builds-ahead-at-user-visible-priority.md): at the
phone's load every body is drawn as vectors in the frame, and its tick image is out of reach. A body's drawing changes once a
tick, but a frame between two ticks runs its draw code again, in Kotlin, call by call into Skia, wherever something else on
the screen asks for a frame.

- **The decision to test.** A `graphicsLayer` around each body's drawing, so a frame between ticks replays the layer's
  recorded picture instead of running the body's draw code. Nothing drawn changes. Rejected: one layer for the whole
  screen, which a tick of any body would record again whole.
- **Measured before it runs.** The proxy slowed ×2, in one rotation: B-119's build, this item; `TenCards8` and `Blank`, six
  rounds of ten seconds, round 1 not counted. It **buys its cost** if every counted round is cheaper than B-119's build in
  ms a frame by more than twice the largest move of `Blank`'s work between the same builds.
- AC: both gates green with nothing raised (the drips hang outside the body's box, and a layer must not cut them).
