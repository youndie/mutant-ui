---
id: B-114
title: "A resting body's tick image built in the frame when the one ahead is late"
status: dropped
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

## Results, 2026-11-18: dearer, and reverted

[The run](../../logs/b-114/2026-11-18-proxy.log), the Mac charging. Against the kit, every counted round: this item
**+75.4 to +79.9 ms** of frame work (3.6 frames a second against 4.9), and with B-113's freeze +75.4 to +80.1; the kit
without the four decorations −23.8 to −29.0. Red by its rule; the code is reverted (`3d3f164`).

Why: on the proxy a frame lasts about 210 ms and a resting tick 125, so every frame meets a new tick, and building the image
in it adds a CPU raster of every body to a frame that already builds them. There are no "other frames of the tick" to
repay it. The phone, at about 48 ms a frame (B-111), has two or three frames a tick, so this regime is the proxy's, not
the phone's: **the proxy slowed four times is about four times slower than the phone**, and measures a state the phone is
not in. The next run calibrates the slowdown to the phone.

The gate also failed two tests the change moved on purpose (`StandingClockImageTest`: the first ticks now drew images;
`SnackbarBehaviourTest`, timing). Not chased.

**Kept from it**: an image cut on whole scene units stood off the body's vectors by up to half a pixel at a fractional
density — 2.9 % of pixels more than 16 levels apart at 2.625, the Pixel 6a's — the image path's own defect since B-82,
unseen because `BodyAsImageTest` ran at densities 1 and 2 only. It is [B-115](B-115-a-body-image-on-the-surface-grid.md).
