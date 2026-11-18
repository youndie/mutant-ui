---
id: B-129
title: "The contour's distance computed once a tick, read by the shader"
status: dropped
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-129 — The contour's distance computed once a tick, read by the shader

[B-128](B-128-the-shader-edge-from-the-contour.md) drew the paths' curve within 0.26 % of pixels, but by walking segments per
pixel it ran at 3.7 frames a second on the phone. [B-110](B-110-a-body-as-a-shader.md)'s shader, four texture reads a query,
ran at 61.

- **The decision.** Each tick, on the CPU, the signed distance to the contour (and to the highlight) on a grid of
  0.75 dp over the body's box, exact within a band of half the outline and a pixel around the curve (from B-128's binned
  segments: the nearest segment, inside by the even-odd rule) and saturated outside it; the shader reads it bilinearly, four
  texels a query, as B-110 read the field. Rejected: B-128's walk on the GPU; a finer grid until the picture asks for it.
- **Set before the first render.** `BodyShaderParityTest` within its line of 0.5 % in every case; on the phone, the
  decorations left out, in one rotation with paths and B-110's shader: **it holds** if its pooled frames a second is within
  2 of B-110's shader's and its work p50 under the paths'.

## Results, 2026-11-18: right picture, far too slow, dropped

`BodyShaderParityTest` at 0.06–0.28 % (a mutant inverting inside fails it at 87 %); both gates green. On
[the run](../../logs/b-129/2026-11-18-pixel6a.log), 37.3–37.7 °C, the decorations left out of all three
([diff](../../logs/b-129/gpu-none4.diff)): paths 50.9 frames a second (rounds 38, 61, 61, 33, 61), B-110's shader 61.2 in
every round, **this item 2.4, its main thread's work 437.6 ms a frame**. The cost is the CPU: on the desktop's JVM the two
grids of one card take about 3 ms a tick, warm (`GRIDTIME`, a throwaway probe), and the phone's wasm is several times
slower; on a thread this busy every frame is a tick. B-128's 3.7 frames a second was most likely the same CPU cost (its
binning), not the GPU walk it was put down to. Not measured apart; recorded as the likelier reading. The CPU cannot carry
the curve each tick on the phone: [B-130](B-130-the-shader-marches-its-cells.md) moves the march to the GPU.
