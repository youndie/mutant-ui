---
id: B-128
title: "The body shader's edge from the contour itself"
status: done
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-128 — The body shader's edge from the contour itself

Part of [B-126](B-126-bodies-on-the-gpu.md). B-110's shader read the body's edge off the distance field: the field's value
over its gradient, the gradient capped at 1. Where the mosaic quantises the field into blocks, the value jumps by up to ten
across one cell, so the outline 2.5 dp wide thinned to 0.4 dp and broke into fragments, and the edge stepped where paths draw
a smoothed curve ([crops](../../logs/b-126/2026-11-18-b110-mosaic-edge-paths-vs-shader.png)). Paths stroke and fill the
contour the generator marches and smooths: that curve is what the shader must draw.

- **The decision.** Each tick, the contour's and the highlight's `GenPath` (the quadratics in tenths the paths are built
  from) flattened into short segments, three a quadratic, and binned on a grid of the body's box: a bin lists every
  segment within reach of it (half the outline and a pixel), and carries whether its centre is inside (even-odd, as the
  paths fill). The shader reads its pixel's bin: the distance to the nearest listed segment, and inside or not by the bin
  centre's flag flipped at every listed segment the line from the centre to the pixel crosses. Coverage and the outline come
  from that distance, as B-110's did from the field's. Rejected: a better reading of the field (it is not the curve), and
  the quadratics solved in the shader (a cubic per curve, per pixel).
- **Set before the first render.** `BodyShaderParityTest`, decorations left out of both, card, button and FAB, both themes,
  densities 1 and 2.625: **at most 0.5 % of the stage's pixels more than 32 levels apart in every case**, and the crops of
  the mosaic edge and the drips looked at beside the paths'. B-110 read 1.86–3.55 %.

## Results, 2026-11-18

**The picture: done.** `BodyShaderParityTest` at 0.04–0.26 % of pixels more than 32 levels apart in the twelve cases, under
the line of 0.5 % ([shares](../../logs/b-128/2026-11-18-shares.txt)); the mosaic's edge and the drips as the paths draw them
([edge](../../logs/b-128/2026-11-18-mosaic-edge-paths-vs-shader.png), [drips](../../logs/b-128/2026-11-18-drips-paths-vs-shader.png)).
A mutant that never flips inside along the line to the bin's centre fails it at 3.3–5.2 %; the test still held B-110's red
line of 6 % until this item, and the mutant passed under it once. Both gates green.

**The phone: far too slow.** [The run](../../logs/b-128/2026-11-18-pixel6a.log), 30.7–35.1 °C, the decorations left out of
all three ([diff](../../logs/b-128/gpu-none4.diff)): paths 61.2 frames a second (work p50 7.9 ms), B-110's shader 61.1 (4.5),
**this item's shader 3.7 (291 ms)**. A pixel near the edge walks its bin's segments, dozens of them, three texture reads
each, for every query: hundreds of reads a pixel, which a Mac's GPU hides and a phone's does not. The curve stays; the walk
moves to the CPU, once a tick, in [B-129](B-129-the-contour-distance-once-a-tick.md).
