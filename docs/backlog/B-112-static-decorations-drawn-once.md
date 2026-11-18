---
id: B-112
title: "A body's still decorations drawn once, and painted without the clip"
status: dropped
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

## The measurement, set before it ran (2026-11-17)

The owner asked for the Mac rather than the phone: B-107's phone proxy (`PHONE=1`, Chrome headless at 390 × 844 and 3x,
the main thread slowed four times), on mains. Three builds in one rotation: the kit before this item (`ebf6d4c`), this
item, and the kit without the four decorations (B-111's arm); `TenCards8` and `Blank`, six rounds of ten seconds, round 1
not counted. The reading is ms a frame, `1000 / fps`, per counted round, beside the frame's work and the busy share.
**This item buys its cost back** if every counted round of it is cheaper than the kit, by more than twice the spread of
`Blank`'s rounds, and **all of it** if its mean is within that spread of the build without the decorations.

**Amendment, before the first number:** the Mac was on battery (94 %, discharging) when the run was due, and the research
records that on battery its render cost moved between runs of identical code. The run goes ahead on battery, the builds
paired within it as above, and says so; a run on mains is what would confirm it.

## Results, 2026-11-18: it buys nothing, and the code is reverted

Logs in [logs/b-112/](../../logs/b-112/). Every run is the phone proxy on the Mac, on battery as amended; the builds
paired in one rotation.

**The first cut drew the still layers once into the tick's image only** (`f00f428`). On the proxy it moved nothing: per
counted round, the frame's work moved by +2.9, +2.2, −1.9, +0.1, −2.5 ms, and frames a second not at all
([run 1](../../logs/b-112/2026-11-17-proxy.log)). At 4.9 frames a second the main thread is saturated, so the image a tick
is built ahead is rarely ready, and a body is drawn in the frame.

**The second cut drew them once in the frame too** (`ae584c8`), on an image of the surface's own grid. It was dearer: the
frame's work rose by 3.9, 2.9, 0.8, 1.9, 1.4 ms ([run 2](../../logs/b-112/2026-11-18-proxy.log)). The build without the four
decorations, in the same runs, saved 25 and 29 ms a frame.

**The lever was connected.** A build drawing the image's area red instead of the image shows every card red under the 4×
slowdown ([the control](../../logs/b-112/2026-11-18-proxy-red-control.png)), and a probe on the desktop counted one image
per body and path, built once, with no miss of its cache.

**On the proxy the parts cost what they cost on the phone, and add up** ([the parts](../../logs/b-112/2026-11-18-proxy-parts.log)):
leaving out the speckle saves 12.7 ms of the frame's work, the flow lines 7.8, the dead pixels 4.2, all four 25.0, every
counted round the same way. So the speckle is dear, and one image standing for it is as dear: what costs is not the number
of draws but, as far as these runs show, the pixels they cover. One image over the body's box blends every pixel of it,
about 1 000 × 400 at the proxy's 3x; the squares cover a small part of that. That is a reading, not yet a measured
mechanism.

**The picture.** `StillLayersOnceTest` held both paths within 0.012 % of pixels over 16 levels at density 1 and 2.625, and
a one-pixel shift of the image failed it at 0.3–0.96 %. But the frame path failed four parity tests of the design
(`DrawerParityTest`, `IconButtonParityTest`, `SheetParityTest`, `KeyframeLossParityTest`) on references drawn under a
frozen clock, by 1.7–2.7 % of pixels: bodies drawn in a moved picture or off the surface's grid. Not chased, as the change
bought nothing.

**What it leaves.** The speckle (12.7), the flow lines (7.8) and the dead pixels (4.2) are where the time goes. A cheaper
way to draw them has to cover fewer pixels or none on the CPU: the speckle as a tile the GPU repeats, or drawn only where
the clip lets it show. Neither is tried here.
