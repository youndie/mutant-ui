---
id: B-130
title: "The body shader marches its own cells"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-130 — The body shader marches its own cells

[B-110](B-110-a-body-as-a-shader.md)'s field shader held 60 frames a second hot but broke the outline where the mosaic
quantises the field; [B-128](B-128-the-shader-edge-from-the-contour.md) and [B-129](B-129-the-contour-distance-once-a-tick.md)
drew the paths' curve but carried it from the CPU, too slow on the phone.

- **The decision.** The field goes to the shader as in B-110, and a pixel near the edge marches the cells around it as
  `march` does (the same crossings on the cells' edges, the same choice in a saddle) and takes its distance to those
  segments: the paths' curve before `smoothPath` rounds it. Inside or out is the field's sign at the pixel. A CPU pass over
  the field, cheap and linear, marks the nodes within reach of a crossing (two cells for the outline, one for the
  highlight), so a pixel far from the edge reads one texel. Rejected: anything per tick heavier than a pass over the field.
- **Set before the first render.** `BodyShaderParityTest` within 0.5 % in every case; on the phone, the decorations left
  out, in one rotation with paths and B-110's shader: **it holds** if its pooled frames a second is within 2 of B-110's
  shader's.
