---
id: B-127
title: "A body's fill and speckle as one tile the GPU repeats"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-127 — A body's fill and speckle as one tile the GPU repeats

Found by [B-125](B-125-what-the-decorations-cost-on-the-phone-now.md) and [B-126](B-126-bodies-on-the-gpu.md): on a warm
phone bodies hold 60 frames a second without decorations, and the still layers' image of B-117, the size of each body's
box, costs about as much as it saves. The owner's first idea for the speckle: a tile the GPU repeats.

- **The decision to test.** A resting body's fill and speckle drawn as one rectangle under the contour's clip, as the fill
  alone was, painted by a small tile (the speckle's 40 dp period in whole device pixels, at the surface grid's phase) that
  holds the fill colour and the squares; the mosaic's tiles and the dead pixels drawn as before, a few dozen rectangles;
  B-117's body-sized image no longer drawn. A frozen clock draws as before.
- **Measured before it runs.** Chrome on the Pixel 6a, warm as it is, on mains, in one rotation: the loop's build (B-119),
  this item, the kit before the loop; `TenCards8` and `Blank`, six rounds of ten seconds, round 1 not counted. It **buys its
  cost** if its pooled frames a second is higher than the loop's build's and every counted round of it is faster in ms a
  frame by more than twice the largest move of `Blank`'s work between the same builds. Rounds where the phone is past
  38 °C are reported with their temperature.
- AC: the same pixels as drawing each square each tick, within `StillLayersOnceTest`'s line (0.2 % of pixels over 16
  levels) at 1 and 2.625; the frozen clock's pixels unchanged; a one-pixel phase error fails a test; both gates green.
