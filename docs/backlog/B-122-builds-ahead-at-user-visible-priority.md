---
id: B-122
title: "A body's builds ahead posted at user-visible priority in the browser"
status: dropped
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-122 — A body's builds ahead posted at user-visible priority in the browser

Found by [B-121](B-121-the-outline-stroked-once-a-tick.md): slowed ×2, as the phone runs, no body reaches its tick image;
every one is built in the frame and drawn as vectors every frame. The builds ahead are posted one task each at
`scheduler.postTask`'s `background` priority (B-87), which runs only when the page is idle, and a page drawing every frame
on a thread 100 % busy never is.

- **The decision to test.** Post them at `user-visible` priority, still one task each, so they run between frames with the
  page's other tasks. Rejected: `user-blocking`, which would run them ahead of input.
- **Measured before it runs.** The proxy slowed ×2, in one rotation: B-119's build, this item; `TenCards8` and `Blank`, six
  rounds of ten seconds, round 1 not counted. It **buys its cost** if every counted round is cheaper than B-119's build in
  ms a frame by more than twice the largest move of `Blank`'s work between the same builds. Beside it, the red control of
  B-121 on this item's code, to see whether the tick images are reached.
- AC: the logs, the control's shots; both gates green.

## Results, 2026-11-18: far dearer, and reverted

The red control on this item's code, slowed ×2: **still no body from its tick image**
([shot](../../logs/b-122/2026-11-18-image-path-cpu2.png)). And [the run](../../logs/b-122/2026-11-18-proxy-x2.log): 5.5 frames
a second against B-119's 24.1, **137 to 142 ms a frame dearer** in every counted round. Run between frames, ten bodies'
builds with their rasters do not fit in a tick of 125 ms on this load; each new tick cancels the one before, so the work is
done and thrown away. At `background` they never start, and cost nothing. Reverted (`b931ff7`). The tick image cannot be
reached at the phone's load by scheduling; only a cheaper build or raster could reach it.
