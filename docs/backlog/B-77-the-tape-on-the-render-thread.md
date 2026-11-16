---
id: B-77
title: "The Tape costs 3.2 ms a frame on the render thread of the Demo page"
status: done
priority: P3
size: S
stage: stage-5-product
blocked_by: [B-76]
---

# B-77 — The Tape costs 3.2 ms a frame on the render thread

**Lowered to P3 on 2026-11-15**: the owner judged the JVM's frame cost acceptable after B-75, B-82 and B-83, and the components come first.

Found by [B-76](B-76-render-thread-replay-on-the-demo-page.md). On the Demo page the Tape is drawn four times, over the page and over each screen. It costs the render thread 3.2 ms a frame of 19.8 (19.79 ms with it, 16.62 ms under «Чистый сигнал»), on a thread that is 91 % busy.

- **The decision and its reason.** First measure where the Tape's own cost goes, with B-76's bench and its render control:
  - its grain;
  - its noise bands;
  - its lines;
  - the overdraw of four Tapes, against one Tape over the page.

  The Tape steps only on the scene's tick (eight times a second), so one candidate is to draw a step once into an image and replay that image until the next step. Whether that is cheaper on the render thread is what the measurement decides, and the picture must stay byte-identical to the Tape's references. Rejected: a lighter Tape, which changes what the design draws.
- AC: B-76's bench, same stand: the Tape's part of the render cost per frame before and after, with the control's reading. The Tape's parity and goldens are unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantTape.kt`, `sample/src/desktopTest/kotlin/io/github/youndie/mutant/sample/bench/FrameBench.kt`

## Results, 2026-11-16

**The premise still held** after B-82 and B-83, measured before any change on `FrameBench` at `35f74eb` ([log](../../logs/b-77/2026-11-16-desktop-before.log)). On the Demo page the Tape cost the render thread 2.59 ms a frame: 4.10 against 1.51 ms under «Чистый сигнал». On the Components page it cost 1.24 ms.

**Where its cost went.** `tapeRenderBench` is new for this item. It draws the Demo page's four Tapes over plain ground (one over the window, one over each 360 × 720 dp screen), and leaves one part out at a time. The render thread's CPU is read as B-76's bench reads it, with its control. Over a blank page's 0.38 ms, the four Tapes added 3.33 ms ([log](../../logs/b-77/2026-11-16-tape.log)). Leaving out each part saved:

| Part | before | after |
|---|---|---|
| the grain | 2.20 ms | 0.02 ms |
| the scanlines | 0.32 ms | 0.80 ms |
| the noise | 0.27 ms | 0.25 ms |
| the band | 0.04 ms | −0.02 ms |

Each reading is a removal in the same scene, and the parts do not add: the control's fixed load adds 2.50–2.70 ms on a blank page, against 0.72–1.05 over the Tapes before and 1.81–1.93 after. So the scanlines' share rose once the grain was cheap; their own cost did not.

**Refuted on the way**, each measured, each reverted:
- *One clip a grain tile kept the strokes from being drawn together*: the strokes cut at their tiles instead read 4.13 ms against 4.10.
- *A path a stroke, its copies in every tile*: 42.8 ms, rasterising paths of 75 rectangles.
- *The tile as an image, drawn once per tile*: 3.67 ms. Each draw makes an image of the bitmap again.

**The fix.** At a whole density the grain's tile is rasterised once (`grainTile`), and the grain is one fill repeating it (an `ImageShader`), drawn at the device's pixels and moved by the tick's offset. It draws the very pixels the strokes drew: `TapeGrainTileTest` compares the two, at density 1, 2 and 3 in both themes, over three ticks whose offsets differ, and finds 0 pixels off. At a fractional density (Android's 2.625, say) a tile is not whole pixels, and the strokes are drawn as before.

**After**, at `125bef2`:

| | before | after |
|---|---|---|
| `tapeRenderBench`, four Tapes ([log](../../logs/b-77/2026-11-16-tape-after.log)) | 3.71 ms | 1.54 ms |
| `tapeRenderBench`, one Tape | 2.74 ms | 0.79 ms |
| `FrameBench`, the Demo page's render, its Tape's part ([log](../../logs/b-77/2026-11-16-desktop-after.log)) | 4.10 ms, of it 2.59 the Tape | 2.54 ms, of it 1.03 |
| `FrameBench`, the Components page, the Tape's part | 1.24 ms | 0.24 ms |
| The browser's Demo page, work p50 ([log](../../logs/b-77/2026-11-16-browser-after.log)) | 11.7–12.0 ms | 10.2 ms |
| The browser's Components page, work p50 | 5.0–5.1 ms | 4.1 ms |

Same host, consecutive runs, on mains power. The JVM's main-thread work did not move. `FrameBench`'s control: `BlankLoad` adds 2.61 and 2.65 ms over `Blank` in the two runs.

**Mutants** (`MutantTape.kt`), each killed by `TapeGrainTileTest`:

| Mutant | |
|---|---|
| the fill not moved by the tick's offset | killed |
| a tile at a fractional density too | killed |
| the tile repeated across only, not down | killed |
