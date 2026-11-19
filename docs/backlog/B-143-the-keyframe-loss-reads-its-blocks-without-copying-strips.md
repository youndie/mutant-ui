---
id: B-143
title: "The keyframe loss reads its blocks without copying whole strips"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-143 — The keyframe loss reads its blocks without copying whole strips

Found by [B-142](B-142-a-stall-every-nine-seconds-on-the-components-page.md): on the Pixel 6a in Chrome a running
keyframe loss holds every frame at 110–220 ms for its whole run, about a second in every nine, on the resting
Components page. Its profile puts 472 ms of the loss's 735 in `StripPicture.drawPart`'s `drawImageRect`: each block of
each step reads its source by drawing a mutable strip, which Compose on Skiko turns into an image of the whole strip
on every draw (B-113, B-119).

- **The decision to test.** Read a block's part without making an image of a whole strip: its pixels copied out of
  the strips it lies in (`ImageBitmap.readPixels` of the block's rectangle, or the platform's subset), so a step copies
  the blocks, not the strips.
- **Rejected:** one frozen copy of the strips per step, read by every block — the blocks of a step read what the
  earlier ones wrote, as the engine's canvas drawn onto itself does, and a copy taken before the step would not.
- AC:
  - the keyframe loss's parity, goldens and behaviour tests unchanged (`KeyframeLossParityTest`,
    `KeyframeLossBehaviourTest`, `MoshSnapshotOnGpuTest`), every pixel;
  - a test that a step's cost does not grow with the strip's width for a fixed set of blocks, or another check named
    in the item that fails on today's code;
  - B-142's protocol on the phone against `main`: stall seconds and the frames' work inside a loss, with the
    loss-free arm as the floor.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/StripPicture.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`

## Findings

### Iteration 1, 2026-11-19

- `StripPicture.drawPart` reads a block through `Canvas.drawRegion` (`RegionDraw.kt`): on Skiko (desktop, iOS, wasm)
  it draws a subset of the strip's bitmap (`extractSubset`, sharing the pixels), so the image Compose makes of it to
  draw holds the block only; on Android it is `drawImageRect` as before, as Android draws a bitmap without copying it.
  The read comes out whole into the part before anything is written, as before, so a step's blocks still read what
  the earlier ones wrote. Writing the part into the strips is unchanged (the part is block-sized already).
- `StripPictureTest`, new:
  - `a step's blocks cost the blocks, not the width of the strips they lie in`: 120 blocks of 16 px, the same blocks on
    a picture 400 and 12 000 px wide. Before the fix (`a89207a`, which is the mutant restoring the whole strip's read):
    failed, 987 µs against 7496 (× 7.6), and × 8.9 in a first run; with it: green, under × 4;
  - `blocks dragged within the strips land as on one bitmap, every pixel`: 300 blocks, some overlapping, and a tint,
    against one bitmap drawn onto itself. Green before the fix and after it.
  - Found on the way: painted shapes in the strips are not the one bitmap's to the pixel (a channel off by 1 at an
    edge, with each strip drawing moved up to its place), so the test starts the one bitmap from the strips' pixels and
    compares the operations after. B-88's "pixel for pixel" holds for the operations, not for a shape's rasterising.
- The keyframe loss's own tests unchanged and green: `KeyframeLossParityTest`, `KeyframeLossBehaviourTest`,
  `KeyframeLossPopupTest`, `KeyframeLossGoldenTest`, `MoshSnapshotOnGpuTest`.

**Pre-registered before the run**, B-142's protocol: the Pixel 6a in Chrome, on mains, `ComponentsRest`, 3 rounds of
60 s interleaved (round 1 not counted): `main` (`c9ee37f`; its build is B-142's, `fa05977cdbb9be9f`), this branch as
`~b`, and B-142's arm without the loss (`1719bd18bb513da9`) as `~c`, the floor. A stall second has 20 frames or fewer.
Held if, in both counted loads, `main` has 5 or more stall seconds 9 s apart and this branch at most one; then the
frames a second in `main`'s stall seconds are read for this branch against the floor, as measured, with no line.
