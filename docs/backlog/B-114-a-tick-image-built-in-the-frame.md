---
id: B-114
title: "A resting body's tick image built in the frame when the one ahead is late"
status: wip
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-114 — A resting body's tick image built in the frame when the one ahead is late

Found by [B-113](B-113-an-image-not-copied-each-frame.md): on the phone proxy, slowed four times, no body is drawn from its
tick's image; the builds ahead never finish, and every body draws its vectors, recorded once a tick and replayed on every
frame.

- **The decision to test.** When a resting body's image for this tick is not ready, rasterise it in the frame, once for the
  tick, and draw the image, as a ready one is drawn; vectors stay for what draws them today (a pour, a slide, a hot body, a
  frozen clock). Rejected: raising the builds' priority, which takes the same thread's time from frames in any case.
- **Measured before it ran.** As B-113: the phone proxy, in one rotation the kit (B-111's), this item without B-113's
  freeze, this item with it, and the kit without the four decorations; `TenCards8` and `Blank`, six rounds of ten
  seconds, round 1 not counted. **It buys its cost** if every counted round is cheaper than the kit, in the frame's work p50
  and in ms a frame, by more than twice the largest move of `Blank`'s work between the same builds.
- AC: the logs; `BodyAsImageTest` (image against vectors) green, extended to a body whose image is built in the frame;
  the goldens unchanged; a mutant that skips the in-frame build fails a test.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`
