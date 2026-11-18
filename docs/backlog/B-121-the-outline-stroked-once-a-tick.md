---
id: B-121
title: "A body's outline stroked once a tick"
status: dropped
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

## Results, 2026-11-18: dearer, and reverted

Both gates green with the fill: every parity test and golden unchanged, and a mutant filling an outline 15 % wider failed
twenty parity tests (`ButtonParityTest`, `CardParityTest` and on). But on [the run](../../logs/b-121/2026-11-18-proxy-x2.log),
the proxy slowed ×2, it was **dearer in every counted round, by 21.5 to 23.6 ms a frame** (15.3 frames a second against
B-119's 23.3); B-119's build without the outline saved 11.4 to 13.1 in the same run. Filling the stroke's many overlapping
contours costs Skia more than stroking the path. Reverted (`8ce9f23`).

**Which path the bodies take at this load.** A build of B-119's code drawing every body that comes from its tick's image as
a red box: unslowed every card is red; slowed ×1.5 one of ten; **slowed ×2, as the phone runs, none**
([×1](../../logs/b-121/2026-11-18-image-path-cpu1.png), [×1.5](../../logs/b-121/2026-11-18-image-path-cpu1.5.png),
[×2](../../logs/b-121/2026-11-18-image-path-cpu2.png)). At the phone's load every body is built in the frame and drawn as
vectors every frame; the tick image of B-82 is not reached. The builds ahead are posted at the scheduler's `background`
priority (B-87), which a page drawing every frame does not leave room for. That is [B-122](B-122-builds-ahead-at-user-visible-priority.md).
