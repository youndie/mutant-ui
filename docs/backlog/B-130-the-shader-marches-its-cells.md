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

## Results, 2026-11-18

**The phone: holds.** [The run](../../logs/b-130/2026-11-18-pixel6a.log), 38.0–38.7 °C, the decorations left out of all three
([diff](../../logs/b-130/gpu-none4.diff)): paths 41.3 frames a second (rounds 36.7, 35.7, 36.6, 35.8, 61.5), B-110's shader
61.0, **this item 61.0, at 60.8–61.3 in every round**, work p50 5.8 ms against B-110's 4.4.

**The picture: grey.** `BodyShaderParityTest` at 0.27–0.59 % ([shares](../../logs/b-130/2026-11-18-shares.txt)): seven cases
under the line of 0.5 %, five of «Копия» over it (0.50–0.59). The mosaic's edge holds its outline whole
([edge](../../logs/b-130/2026-11-18-mosaic-edge-paths-vs-shader.png)); the drips are a shade more angular
([drips](../../logs/b-130/2026-11-18-drips-paths-vs-shader.png)): the shader draws the marched polyline, and the paths round it
through `smoothPath`'s quadratics and drop the points closer than its step. That rounding is what is left to match.

**Accepted, 2026-11-18.** Read to three places ([shares](../../logs/b-132/2026-11-18-shares.txt)), four of the five are over
the line (0.537–0.587 %; card-kopiya-1.0 is 0.495). The owner accepted them as a known departure; the test's `known` list
holds each at 0.6 %, and [B-132](B-132-the-shader-rounds-its-contour.md) is the way back under 0.5 %.
