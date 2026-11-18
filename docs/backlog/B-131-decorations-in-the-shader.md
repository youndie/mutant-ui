---
id: B-131
title: "A body's decorations drawn by its shader"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-131 — A body's decorations drawn by its shader

Part of [B-126](B-126-bodies-on-the-gpu.md): [B-130](B-130-the-shader-marches-its-cells.md)'s shader holds 61 frames a second
on a hot phone without decorations. The owner keeps every decoration.

- **The decision.** In the shader, inside the contour's coverage as the paths' clip does: the fill; the still layers'
  image of B-117 (the speckle, the mosaic's tiles, the dead pixels), drawn once an instance on the surface's grid and read
  texel for texel; the flow lines, from the segments of their own path (a few short loops a tick), each loop read only by
  the pixels in its box, inside by the non-zero rule as the paths fill them. A body with anything else drawn under its clip
  (a pour, the running mosaic of a hot body, an error's blocks or bands, an error outline, focus brackets), or off the
  surface's whole pixels, keeps its paths.
- **Set before the first render.** `BodyShaderParityTest` with every decoration drawn by both, the same cases: at most
  0.8 % of pixels more than 32 levels apart in every case (B-130's 0.6 % for the silhouette and room for the flow lines'
  edge); on the phone, every decoration on, in one rotation with paths (the loop's build): **it holds** if it runs at 55
  frames a second or more pooled, hot.
