---
id: B-88
title: "The keyframe loss copies its whole picture for every block it drags"
status: wip
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-87]
---

# B-88 — The keyframe loss copies its whole picture for every block

Found by [B-87](B-87-ticks-on-the-one-thread.md) on 2026-11-16, in headless Chrome 157 on an Apple M1. With the ticks fixed (`6e3faa5`), the Library screen alone still had 7 % of its intervals over 33 ms. Almost all of them stood in the first two seconds of a scene and in the last second of each ten. That is page age ≈ 3.5 s and ≈ 12.5 s, when [B-48](B-48-keyframe-loss.md)'s loss runs (3.5 s into each 9 s window). With B-87's builds at background priority (`085a110`), the screen is grey at 3.23 %, but 10–12 long animation frames a round remain, 0.73–0.92 s of each ten ([log](../../logs/b-87/2026-11-16-browser-final.log)). Over a run that spans a loss, `SkiaBackedCanvas.drawImageRect` takes 12.6 % of the busy main thread.

Each step (every 125 ms) drags each block of the band by drawing the picture onto itself (`MoshScene.advance`). That is about 150 blocks on a 360 × 720 dp screen. Compose 1.12.1's `SkiaBackedCanvas.drawImageRect` makes an `Image.makeFromBitmap` of the whole source bitmap for every call, and closes it after; that is read in its bytecode. For a bitmap that is still drawn into, Skia copies the pixels to make the image. That copy is inferred from Skia's documented behaviour, not traced: 720 × 1 440 px × 4 bytes at 2×, about 4 MB a block, about 600 MB a step. The JVM pays it too, on its main thread: the Library screen's work p99 is 15.9 ms there, and its maximum 32.9.

- **The decision and its reason.** To be decided when the item is taken. The blocks cannot share one copy a step. The engine draws each from its canvas as the blocks before it left it: a block of row *r* pulls into row *r* + 1 before that row's blocks read it, and the halves of a block pull into each other. The parity frames hold that order. The candidates:
  - copy the block's own rectangle only, then draw it back;
  - keep the band in a pixel buffer, drag and tint the blocks there, and hand the picture to Skia once a step.
- AC:
  - On B-73's bench, the Library screen alone has no long animation frame in a round, and intervals over 33 ms under 1 %.
  - `KeyframeLossParityTest`, `KeyframeLossBehaviourTest` and `KeyframeLossGoldenTest` stay green, their residuals unchanged.
  - The cost of a step is measured before and after, on the JVM and in the browser.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`, `scripts/frame-bench-browser.mjs`
