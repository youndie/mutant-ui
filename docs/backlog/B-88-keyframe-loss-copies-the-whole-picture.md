---
id: B-88
title: "The keyframe loss copies its whole picture for every block it drags"
status: done
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-87]
---

# B-88 — The keyframe loss copies its whole picture for every block

Found by [B-87](B-87-ticks-on-the-one-thread.md) on 2026-11-16, in headless Chrome 157 on an Apple M1. With the ticks fixed (`6e3faa5`), the Library screen alone still had 7 % of its intervals over 33 ms. Almost all of them stood in the first two seconds of a scene and in the last second of each ten. That is page age ≈ 3.5 s and ≈ 12.5 s, when [B-48](B-48-keyframe-loss.md)'s loss runs (3.5 s into each 9 s window). With B-87's builds at background priority (`085a110`), the screen is grey at 3.23 %, but 10–12 long animation frames a round remain, 0.73–0.92 s of each ten ([log](../../logs/b-87/2026-11-16-browser-final.log)). Over a run that spans a loss, `SkiaBackedCanvas.drawImageRect` takes 12.6 % of the busy main thread.

Each step (every 125 ms) drags each block of the band by drawing the picture onto itself (`MoshScene.advance`). That is about 150 blocks on a 360 × 720 dp screen. Compose 1.12.1's `SkiaBackedCanvas.drawImageRect` makes an `Image.makeFromBitmap` of the whole source bitmap for every call, and closes it after; that is read in its bytecode. Whether Skia copies the pixels for that image was inferred, not traced; *Results* below say what the measurements showed.

- **The decision and its reason.** To be decided when the item is taken. The blocks cannot share one copy a step. The engine draws each from its canvas as the blocks before it left it: a block of row *r* pulls into row *r* + 1 before that row's blocks read it, and the halves of a block pull into each other. The parity frames hold that order. The candidates:
  - copy the block's own rectangle only, then draw it back;
  - keep the band in a pixel buffer, drag and tint the blocks there, and hand the picture to Skia once a step.
- AC:
  - On B-73's bench, the Library screen alone has no long animation frame in a round, and intervals over 33 ms under 1 %.
  - `KeyframeLossParityTest`, `KeyframeLossBehaviourTest` and `KeyframeLossGoldenTest` stay green, their residuals unchanged.
  - The cost of a step is measured before and after, on the JVM and in the browser.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`, `scripts/frame-bench-browser.mjs`

## Results, 2026-11-16

**The cost of a step.**
- **On the JVM** (`keyframeLossBench`, headless, 360 × 720 dp at 2×, eight cards, 140 blocks): a step cost 4.70 ms before (median of 11 counted rounds, 3.68–7.61; [log](../../logs/b-88/2026-11-16-desktop-before.log)). After, it is below the bench's resolution: median −0.29 ms, effect under 0.4 ms ([log](../../logs/b-88/2026-11-16-desktop-after.log)). That is about 25 µs a block before: the JVM did not copy 4 MB a block, so the copy above was the browser's.
- **In the browser** (B-73's bench, the Library screen alone):

  | | before (`085a110`) | after (`2a5896b`) |
  |---|---|---|
  | frames/s | 57.1 | 59.5 |
  | intervals over 33 ms | 3.23 % | 1.38 % (1.25 in the control run) |
  | longest interval | 100 ms | 50 ms |
  | work p95 | 26.8 ms | 8.6 ms |
  | long animation frames a round | 10–12 (0.73–0.92 s) | 7–8 (0.40–0.47 s) |

  Logs: [the bench](../../logs/b-88/2026-11-16-browser.log), [the control](../../logs/b-88/2026-11-16-control.log). Every other scene is as B-87 left it.

**What changed**, each step measured in the browser:
1. **A first try made it worse**, and is not kept (logs `-pixels`): read each block's pixels out (`readPixels`), then draw them from an image of their own. Long frames rose to 1.25–1.48 s a round. In wasm every array crosses between the Kotlin and the Skia modules, and 140 small crossings a step cost more than the copies they replaced.
2. **The picture is kept as strips** one row tall over the band and a row under it (`StripPicture`). A block is drawn from the strips it lies in, read out whole into a part of its own first, as drawing a bitmap onto itself reads it. Every operation stands for the one it replaces on one bitmap, pixel for pixel: the parity frames read the same 75, 300, 205 and 181 px, and 37, 31 and 15 px in the cleared blocks.
3. **The snapshot is drawn straight into the strips.** Filled from one whole picture, each strip copied the whole of it: a loss's first frame took 83–100 ms. At 95 % one pixel more now lies within 16 levels of the page (204 against 205).
4. **At a step only the bodies near a cleared block are drawn again.** Every body in the band read the step's counter, though a cleared block lies over few of them. Intervals over 33 ms went from 2.0 % to 1.3 %, measured twice.
5. **Rectangle clips in place of the path cut out of the band** were tried, and are not kept. A body was drawn once outside the band and once in each block, and intervals over 33 ms went back to 2.04 %.

**The control.** A Library screen without its loss (`?scene=DemoLibraryNoLoss`), in the same runs: no long animation frame and 0.00 % of intervals over 33 ms. So all of the screen's long frames are the loss's.

**Mutants**, each killed by `KeyframeLossParityTest`:

| Mutant | |
|---|---|
| a part drawn while it is read, not read out first | killed |
| no row of strips under the band | killed |
| a part drawn onto its first strip only | killed |
| no reach around a body for the cleared blocks (64 → 0 dp) | killed |

## Question for the owner, 2026-11-16

The whole-picture copies are gone. The AC is not met: there are 7–8 long animation frames a round, where it asks for none, and 1.3 % of intervals over 33 ms, where it asks for under 1 %.
- **Where they are.** All of them stand in the first loss after the page opens, 3.5 s in, one frame of about 60 ms a step for about a second. The second loss, at 12.5 s, is nearly clean: 2 intervals over 33 ms in five rounds.
- **What the profile shows.** Over each loss's 3 s, the two losses sample the same functions at the same shares, and the loss's own code is not in the top forty of either. So the first loss is not doing more work on the main thread.
- **What is left unproven.** The difference is in what costs more the first time:
  - the step's code not yet optimised by the browser;
  - the bodies not yet drawn as images 3.5 s after the page opens (B-82), so the snapshot and the redraws draw vectors;
  - the GPU's first use of these draws.

  None of the three is shown.

The choices:
1. **Accept it and close B-88** with these numbers: the first loss of a page stutters for about a second, and later ones do not.
2. **Keep looking**, starting with which of the three it is.
3. **Start a page's first loss later** than the engine's 3.5 s. That is a departure from the design, and yours to make.

**Answered 2026-11-16 by the owner: accept it and close.** The residual stands as measured and is never to be counted as met: the first loss after a page opens drops about seven frames of about 60 ms over its run in the browser (Library screen alone, 1.3 % of intervals over 33 ms); later losses are nearly clean.
