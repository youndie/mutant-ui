---
id: B-145
title: "The keyframe loss's steps off the slow path"
status: open
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-145 — The keyframe loss's steps off the slow path

From [B-144](B-144-the-keyframe-losss-draw-calls-per-step.md): a running keyframe loss holds the Pixel 6a's Chrome at
8–20 frames a second, a second in nine; 405 ms of a loss's 644 is `StripPicture.drawPart`, 206 reading and 189 writing
its blocks. The owner chose route A there: the band's pixels in Kotlin, handed to Skia once a step.

- **The decision.** To be decided from two checks made before route A is built, both in this item:
  - what handing a band to Skia costs in the browser, as route A needs once a step;
  - whether the blocks' draws take Skia's slow path today, which one line would move.
- AC: each check answered with its log; the route that holds measured by B-142's protocol on the phone against `main`,
  with the loss-free arm as the floor; the keyframe loss's parity, goldens and behaviour tests and
  `StripPictureTest` unchanged, or each moved cell named and looked at.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/StripPicture.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`
