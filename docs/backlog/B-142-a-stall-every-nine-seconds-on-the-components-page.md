---
id: B-142
title: "A stall every nine seconds on the resting Components page"
status: open
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-142 — A stall every nine seconds on the resting Components page

Found by [B-141](B-141-the-tape-draws-in-a-layer-of-its-own.md)'s 60 s load on the Pixel 6a in Chrome
(`logs/b-141/2026-11-19-pixel-60s/frames.csv`): with the Tape fixed, the resting Components page holds 6–10 ms of work
a frame, but one second in nine (0, 9, 18, … 54 s) draws 7–12 frames at a median of 94–165 ms. `main` before the fix
shows the same period (`logs/b-140/2026-11-19-rest-60s/frames.csv`: dips at 8, 17, 26, … s).

- **The hypothesis**, not yet tested: the keyframe loss ([B-48](B-48-keyframe-loss.md),
  `MutantKeyframeLoss.kt`) fires every 9 s (`EVERY_MS = 9000.0`), with a step each 125 ms, and its blocks drag a copy of
  the picture on each step (B-136's note). Nothing else in the kit or the sample was found with a nine-second period.
- **The decision.** To be decided from the numbers; first the mechanism, by one variable: the same build with the
  keyframe loss left out, against it, interleaved, 60 s loads read per second.
- AC: the stall's period and depth measured with and without the keyframe loss; the mechanism named only where that
  one variable moved it; a route, if any, measured on the phone with B-73's control.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`
