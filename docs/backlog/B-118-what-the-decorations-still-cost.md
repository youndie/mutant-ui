---
id: B-118
title: "What the decorations still cost, over B-117"
status: done
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

## Results, 2026-11-18

[The run](../../logs/b-118/2026-11-18-proxy-x2.log), on mains; the builds' differences in [arms.diff](../../logs/b-118/arms.diff).
`Blank`'s work moved by at most 1.3 ms, the line 2.6. Against B-117's build at 18.0 frames a second, ms a frame saved per
counted round:

| Left out of B-117 | Saved | Verdict |
|---|---|---|
| the flow lines | 3.9, 4.2, 4.3, 5.2, 6.3 | **costs about 4.8 ms** |
| the speckle | −0.3, 0.0, −0.6, 0.0, 2.9 | below resolution |
| the mosaic and the dead pixels | −0.9, 0.3, 0.0, 0.0, 3.5 | below resolution |
| all four | 4.4, 5.2, 4.8, 5.0, 7.6 | about 5.4 ms |

So B-117's image took in the speckle, the mosaic and the dead pixels, and the flow lines are what is left of the four.
**But all four left out of B-117 run at 20.0 frames a second, where all four left out of the kit ran at 26.0** (B-117's
run): B-117's code carries a floor of its own of some 12 ms a frame that leaving the decorations out does not remove. The
still layers' image is drawn every frame whatever it holds, and unlike the tick's image it is never marked finished
(B-113), so Compose may copy it into a new image, and the GPU upload it, each frame. That is
[B-119](B-119-the-still-image-marked-finished.md).
