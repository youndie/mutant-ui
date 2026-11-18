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

## Run 1, 2026-11-18: the lever is not reached on the proxy

[The run](../../logs/b-113/2026-11-18-proxy.log), the Mac charging: the frame's work moved by +2.1, +2.2, −0.6, −0.3,
+2.0 ms against the kit, `Blank` by up to 1.2; it does not buy its cost by the rule. A build drawing every body that comes
from its tick's image as a red box answers why: unslowed, every card is red; slowed four times, **none is**
([×1](../../logs/b-113/2026-11-18-proxy-image-path-cpu1.png), [×4](../../logs/b-113/2026-11-18-proxy-image-path-cpu4.png)).
On a saturated thread the builds ahead, queued at background priority, never finish in time, so every body is drawn as
vectors in the frame, which saturates the thread further. Neither this item nor B-112 could act there. This item stays on the
branch, to be measured again over [B-114](B-114-a-tick-image-built-in-the-frame.md), which makes the image path reachable.
