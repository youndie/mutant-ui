---
id: B-77
title: "The Tape costs 3.2 ms a frame on the render thread of the Demo page"
status: open
priority: P2
size: S
stage: stage-5-product
blocked_by: [B-76]
---

# B-77 — The Tape costs 3.2 ms a frame on the render thread

Found by [B-76](B-76-render-thread-replay-on-the-demo-page.md). On the Demo page the Tape is drawn four times, over the page and over each screen. It costs the render thread 3.2 ms a frame of 19.8 (19.79 ms with it, 16.62 ms under «Чистый сигнал»), on a thread that is 91 % busy.

- **The decision and its reason.** First measure where the Tape's own cost goes, with B-76's bench and its render control:
  - its grain;
  - its noise bands;
  - its lines;
  - the overdraw of four Tapes, against one Tape over the page.

  The Tape steps only on the scene's tick (eight times a second), so one candidate is to draw a step once into an image and replay that image until the next step. Whether that is cheaper on the render thread is what the measurement decides, and the picture must stay byte-identical to the Tape's references. Rejected: a lighter Tape, which changes what the design draws.
- AC: B-76's bench, same stand: the Tape's part of the render cost per frame before and after, with the control's reading. The Tape's parity and goldens are unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantTape.kt`, `sample/src/desktopTest/kotlin/io/github/youndie/mutant/sample/bench/FrameBench.kt`
