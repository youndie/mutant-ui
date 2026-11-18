---
id: B-133
title: "The keyframe loss freezes a page the shader draws"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-133 — The keyframe loss freezes a page the shader draws

Found by the owner on the phone, the day [B-131](B-131-decorations-in-the-shader.md)'s shader build was put on it: the
components page "stands still and hangs" while scrolled.

## What it was

The page did not hang on scrolling: it froze for **28.7 s every 36.7 s, scrolled or not**. In the phone proxy (Chrome, CPU
×2) the shader build's `ComponentsWheel` had one frame interval of 29.0–29.2 s in each round
([before](../../logs/b-133/2026-11-18-proxy-wheel-before.log), [long frames](../../logs/b-133/2026-11-18-proxy-wheel-before-long-frames.csv));
a probe timing every WebGL compile and link found under 20 ms in all, so not a shader compile, and a CPU profile put two
thirds of the time inside one Skia call made from a body's drawing.

The period is [MutantKeyframeLoss](../../mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt)'s:
every 9 s it takes a snapshot of the screen's bodies into strips of `ImageBitmap`, a bitmap on the CPU, by calling each
body's drawing there. A body the shader draws drew its runtime shader into it, and Skia in the browser ran the shader on
the CPU pixel by pixel. B-131's run was on ten cards, with no loss over them, so it could not see it.

## The decision

A body hands the scene a snapshot of its own (`MoshBody.snapshot`, read through `inSnapshot`); a body the shader draws gives
its paths, built only when a loss takes the snapshot (a frame by paths once in 9 s for the bodies on the screen). The
rejected alternative, a snapshot on the GPU, is not reachable from common Compose code: `ImageBitmap` is a raster bitmap on
Skiko.

## Results, 2026-11-18

- **The test.** `MoshSnapshotOnGpuTest`: a card the shader draws gives the scene a snapshot, and drawn on a CPU bitmap it is
  the paths' picture pixel for pixel, every decoration on (none more than 2 levels apart, at 2.625). Mutants giving no
  snapshot, and the scene drawing `draw` instead of `inSnapshot`, each fail it.
- **The freeze is gone.** The same probe over the fixed build, 40 wheel steps in 75 s: no frame interval over 0.9 s, where
  the build before froze 11 times. In the proxy, three rounds, the paths build against the shader
  ([after](../../logs/b-133/2026-11-18-proxy-wheel-after.log)): scrolling 44.4 against 51.5 frames a second, at rest 40.8
  against 51.6, no interval over 500 ms in either; same host, same run.
- **Not measured**: the phone itself, where the owner looks at the fixed build.
