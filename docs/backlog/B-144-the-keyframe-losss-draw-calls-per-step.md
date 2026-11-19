---
id: B-144
title: "The keyframe loss's draw calls per step"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-144 — The keyframe loss's draw calls per step

Found by [B-143](B-143-the-keyframe-loss-reads-its-blocks-without-copying-strips.md): a running keyframe loss still holds
the Pixel 6a's Chrome at 8–20 frames a second, a second in every nine on the resting Components page, after the
whole-strip copy left its profile. What remains is the blocks' drawing itself: about 360 ms of a loss in
`StripPicture.drawPart`, through `SkiaBackedCanvas.drawImage` into `wasm-to-js`
(`logs/b-143/2026-11-19-profile-fix.txt`). A step draws each block twice (read out, written in) on every strip it
meets, each a call from Kotlin into Skia, with a new bitmap and canvas per block.

- **The decision.** To be decided from the numbers; first count what a step does: draw calls, bitmaps made, and their
  pixels, per step on the Components page's loss at the phone's density, and where in the step the time goes (a profile
  split by `drawPart`'s read and write).
- Candidates, none chosen: a step drawn as one pass over the strips (each strip's blocks in one canvas save, read from
  one frozen copy of that strip where the blocks do not overlap); blocks kept as one picture recorded per step and
  drawn once; fewer, wider strips.
- AC: the count and the split, each with its log; a route measured by B-142's protocol on the phone against `main` with
  the loss-free arm as the floor.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/StripPicture.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`
