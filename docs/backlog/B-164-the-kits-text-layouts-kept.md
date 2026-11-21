---
id: B-164
title: "The kit's text layouts are kept between compositions"
status: wip
priority: P1
size: M
stage: stage-6-later
blocked_by: [B-163]
---

# B-164 — The kit's text layouts are kept between compositions

From [B-151](B-151-a-real-screen-under-a-finger.md)'s round three, a profile on the phone of the frame that composes a
tab: a return to Settings is about 26 ms of the main thread, its layout 10 of them and text 6; a first visit about 56,
text 11. Of Settings' words three are the Demo's and the rest its 19 components' labels. `BasicText` keeps a layout in
its node, and a tab composed anew lays every label out again, the paragraph shaped by Skia each time.

- **The decision**: the kit's words go through `MutantText`, which lays them out through one `TextMeasurer` per density,
  direction and font resolver — its cache keyed by the words, the style and the constraints — and draws the layout's
  paragraph as `BasicText` does: the same size, baselines, intrinsic widths, clipping, colour and semantics (the words
  and the layout). A font arriving late changes the style, and so the key. Rejected: a cache of `Paragraph`s of the
  kit's own, which would re-implement the measurer's.
- AC:
  - every golden, parity and behaviour test unchanged (the full `check`, 420 desktop tests, the browser's);
  - measured by B-151's protocol on the phone against `main`.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantText.kt`

## Findings

### 2026-11-22

- Phone proxy (2 counted rounds of 20 s, against `main`): no change above the spread (tab switches over 50 ms in 3
  against 4 of 28). The phone, where Settings' returns are, was disconnected before its run.
