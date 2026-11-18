---
id: B-131
title: "A body's decorations drawn by its shader"
status: done
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

## Results, 2026-11-18

**The picture.** `BodyShaderParityTest`, every decoration drawn by both: 0.27–0.59 % of pixels more than 32 levels apart in
the twelve cases, under the line of 0.8 % ([shares](../../logs/b-131/2026-11-18-shares.txt),
[«Копия»](../../logs/b-131/card-kopiya-2.625.png), [«Эфир»](../../logs/b-131/card-efir-2.625.png)): the decorations add next to
nothing to B-130's silhouette. Mutants drawing no flow lines (0.74–1.5 %), no still layers (1.4–4.5 %) and never turning
the flow lines' winding (0.9–1.6 %) each fail it.

**The phone, hot.** The first cut walked every segment of a flow line per pixel: 18.5 frames a second against the paths'
22.8 ([run](../../logs/b-131/2026-11-18-pixel6a.log)); without the flow lines the shader ran at 60.9, without the still layers
at 18.9 ([parts](../../logs/b-131/2026-11-18-pixel6a-parts.log)), so the flow lines were the cost. Binned (2 dp), it fell to
6.9 ([run](../../logs/b-131/2026-11-18-pixel6a-binned.log)): the bins' windings were counted segment by segment for every
bin, on the CPU, every tick. Found by rows, 0.05 ms a body on the desktop, and
**[the run](../../logs/b-131/2026-11-18-pixel6a-binned2.log), 37.9–39.0 °C: the paths (the loop's build) 26.9 frames a second,
the shader with every decoration 60.8, at 60.6–61.2 in every round**, work p50 8.3 ms against 29.5. **It holds.**

**Open.** B-130's silhouette test stays over its line of 0.5 % in five cases (0.50–0.59); the gate is red on it until the
owner decides (B-126). The shader is still off by default (`BODY_ON_GPU`); a pressed or hot body, a pour, an error and
focus keep their paths; iOS is not run.

**Missed, 2026-11-18.** The run measured ten cards with no keyframe loss over them; on a page under one, the loss's snapshot
ran the shader on the CPU and froze the page for 29 s every 37 ([B-133](B-133-the-keyframe-loss-freezes-a-shader-page.md)).
