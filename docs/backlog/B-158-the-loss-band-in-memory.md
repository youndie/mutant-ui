---
id: B-158
title: "In the browser, the keyframe loss's band lives in this module's memory"
status: done
priority: P1
size: M
stage: stage-6-later
blocked_by: [B-157]
---

# B-158 — In the browser, the keyframe loss's band lives in this module's memory

B-144's route A, chosen by the owner and not built: the band's pixels in Kotlin, handed to Skia once a step. B-142…B-145
measured a loss's steps on the phone at tens of milliseconds each, Skia's raster pipeline copying blocks a pixel at a
time, and three remedies inside Skia were not held. [B-157](B-157-textures-in-one-block.md) makes the hand-over one
block copy.

- **The decision**: in the browser (`lossBandInMemory`) the loss keeps its band as `BandPixels`: premultiplied RGBA in an
  `IntArray`. The bodies are painted once into one bitmap the band's size (the strips drew them once a strip) and read
  out in one block; a step's drags, its tints and its clean zones are array copies and sums with Skia's rounding; the band
  becomes one immutable image when next drawn. Elsewhere the strips stay (`StripPicture`), and with them every golden.
- AC:
  - `BandPixelsTest`: the same drags, tint, clears and copies on one Skia bitmap and on the band give the same pixels,
    all 240 000 channels;
  - `ImagePixelsTest`, on the desktop and in the browser: a bitmap's pixels read as premultiplied RGBA, and an image of
    them reads the same;
  - the keyframe loss's tests and goldens unchanged;
  - measured against the arm before it.
- A rectangle's edge at a fraction of a pixel (a block's, at 2.625 px a dp) takes the pixels whose centres it holds,
  where Skia covers the edge pixel partly: a pixel at a time at a block's edge, in the browser only.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BandPixels.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`

## Findings

### 2026-11-21

- Phone proxy (`PHONE=1 CPU=2`, B-151's touch bench, 2 counted rounds of 20 s, against the arm with B-157): scroll's
  missed share 0.2 % against 1.4 %; the loss's start remained, longest 333–400 ms.
