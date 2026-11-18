---
id: B-132
title: "The shader rounds its contour as the paths do"
status: open
priority: P3
size: M
stage: stage-6-later
blocked_by: []
---

# B-132 — The shader rounds its contour as the paths do

Part of [B-126](B-126-bodies-on-the-gpu.md). [B-130](B-130-the-shader-marches-its-cells.md)'s shader draws a body's contour as
the marched polyline itself; the paths round it through `smoothPath` (`gen/GenPath.kt`): quadratics through the segments'
midpoints, the vertices as control points, and points closer than the step dropped (0.9 dp for the contour, 1.1 for the
highlight), loops under `minHole` (5 dp) left out. A drip's end is therefore a shade more angular on the GPU
([drips](../../logs/b-130/2026-11-18-drips-paths-vs-shader.png)).

## The known departure, accepted by the owner on 2026-11-18

`BodyShaderParityTest`'s silhouette test holds every case at 0.5 % of pixels more than 32 levels apart, but four, all
«Копия», which its `known` list holds at 0.6 % each, measured when accepted ([shares](../../logs/b-132/2026-11-18-shares.txt)):

| Case | Share |
|---|---|
| button-kopiya-1.0 | 0.567 % |
| card-kopiya-2.625 | 0.547 % |
| button-kopiya-2.625 | 0.587 % |
| fab-kopiya-2.625 | 0.537 % |

The other eight are 0.266–0.500 %. With every decoration drawn by both ([B-131](B-131-decorations-in-the-shader.md)) all
twelve stay under that test's own line of 0.8 %. A case leaves the list once it is back under 0.5 %; the list never grows,
and no ceiling in it rises.

## The direction

- **Quadratics in the shader.** The shader already marches the cells around a pixel, so it has the neighbouring segments:
  the quadratic from one segment's midpoint to the next, through the vertex they share, is local, and its distance is a
  cubic's root (or a few Newton steps from the nearest point on the polyline). What is not local is the dropping of near
  points, which walks the loop; a segment shorter than the step could be merged with its neighbour inside the cell window
  instead, and the difference measured.
- **Rejected for now: a smoother field.** Sampling the field bicubically rounds the iso-line too, but by another rule than
  the paths', so it would move the cases that already match.

## Acceptance

- **Set before the first render.** The four known cases at 0.5 % or under, the other eight not worse than in the table's
  run, `known` emptied; the decorated test still under 0.8 %.
- **On the phone, hot**, every decoration on, in one rotation with B-131's build: **it holds** at 55 frames a second or more
  pooled. Rounder but slower is not taken.
