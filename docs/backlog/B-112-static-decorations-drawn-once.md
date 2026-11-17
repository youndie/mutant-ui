---
id: B-112
title: "A body's still decorations drawn once, and painted without the clip"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-112 — A body's still decorations drawn once, and painted without the clip

Found by [B-111](B-111-what-the-decorations-cost-on-a-phone.md): on a Pixel 6a in Chrome, ten cards cost about 48 ms a
frame, the decorations about 23 of it and the clip to the contour about 16; the speckle alone is some 290 draws a card
a tick under the clip. The speckle, the mosaic's tiles and the dead pixels do not move with the tick.

- **The decision to test.** Draw a body's speckle, mosaic tiles and dead pixels once per instance into an image at the
  device's pixels, and each tick paint the fill and that image as shaders of the contour path, without `clipPath`; the flow
  lines keep the clip, as one draw. Rejected for now: leaving any decoration out (the owner does not cut what is drawn), and a
  shader for the whole body (B-110, a grey silhouette and an Android gap).
- AC: the picture held: the kit's goldens unchanged, or every moved one listed with its share of pixels and looked at; on
  the phone, by B-111's protocol, the build against the kit and against the build without the four decorations, in ms a
  frame; the cost moved to building the image, in the first frame of a body (B-50).
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyDrawing.kt`
