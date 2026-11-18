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
- **The phone, set before it ran.** Chrome on the Pixel 6a, hot as it is, on mains, one rotation: the paths build (the
  loop's), then the fixed shader build; `ComponentsWheel` and `ComponentsRest`, six rounds of ten seconds, round 1 not
  counted. **It holds** if no frame interval of the shader build is over 1 s and its pooled frames a second are at least
  the paths' in both scenes.

  **[The run](../../logs/b-133/2026-11-18-pixel6a.log)**, 28.2–35.4 °C: no frame interval over 500 ms in either build
  ([long frames](../../logs/b-133/2026-11-18-pixel6a-long-frames.csv): none). Pooled, the paths 33.2 frames a second in
  `ComponentsWheel` and 31.4 in `ComponentsRest`, the shader 38.6 and 42.6. **It holds.** On the phone the wheel scrolls
  nothing (`scrolled 0 px`), so both scenes are the page at rest.
- **Scrolled by touch, on the phone.** [`scripts/phone-touch-scroll.mjs`](../../scripts/phone-touch-scroll.mjs) opens a build
  in a new tab and flings the page with `Input.synthesizeScrollGesture` (a screenshot after one fling showed it scrolled),
  16 flings in 32 s: [the run](../../logs/b-133/2026-11-18-pixel6a-touch-scroll.log), 36.8–37.4 °C, paths 33.1 and 21.0
  frames a second, the shader 20.5 and 19.7, the longest interval 466 ms. No freeze; the phone stepped down between the
  first two builds, so the order of the two is not read from it. **Scrolling the components page is not smooth in either
  build** (a third to two fifths of intervals over 33 ms): a finding for its own item, not this one's.

