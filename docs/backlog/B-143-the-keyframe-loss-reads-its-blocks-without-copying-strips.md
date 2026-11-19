---
id: B-143
title: "The keyframe loss reads its blocks without copying whole strips"
status: open
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
