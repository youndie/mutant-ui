---
id: B-113
title: "A body's image not copied each frame it is drawn"
status: wip
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-113 — A body's image not copied each frame it is drawn

Found while closing [B-112](B-112-static-decorations-drawn-once.md): Compose on Skiko draws an `ImageBitmap` by
`Image.makeFromBitmap` on every call (`SkiaBackedCanvas.drawImageRect`, read in `ui-graphics-desktop` 1.12.1). For a bitmap
that is still mutable Skia copies the pixels into a new image, which a GPU canvas then uploads as a new texture: a body's
image of a tick (B-82), drawn every frame until the next tick, may be copied and uploaded every frame. That would also be
why one image for the still layers bought nothing.

- **The decision to test.** Mark a body's image immutable once it is rasterised (Skia's `Bitmap.setImmutable`), so each
  frame's image shares its pixels and its id. Nothing drawn changes. Rejected: holding a Skia `Image` beside the
  `ImageBitmap` and drawing it through the native canvas, more code for the same effect if this works.
- **Measured before it ran.** The phone proxy on the Mac (`PHONE=1`, Chrome headless at 390 × 844 and 3x, the main thread
  slowed four times), three builds in one rotation: the kit (B-111's), this item, the kit without the four decorations;
  `TenCards8` and `Blank`, six rounds of ten seconds, round 1 not counted. The reading is the frame's work p50 per counted
  round, beside frames a second and ms a frame. **It buys its cost** if every counted round of it is cheaper than the kit
  by more than twice the largest move of `Blank`'s work between the same builds.
- AC: the logs; `BodyAsImageTest` and the goldens unchanged; a mutant that freezes the image before it is drawn into fails
  a test.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`
