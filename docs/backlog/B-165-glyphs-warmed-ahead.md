---
id: B-165
title: "In the browser, a screen's first frames warm the kit's glyphs"
status: dropped
priority: P1
size: S
stage: stage-6-later
blocked_by: [B-164]
---

# B-165 — In the browser, a screen's first frames warm the kit's glyphs

From [B-151](B-151-a-real-screen-under-a-finger.md)'s round three: a tab's first visit composes it in about 56 ms of the
phone's main thread, a part of it Skia's work the profile cannot name (its module carries no names), with GPU programs
compiled — a tab's first visit rasterises every letter at every size it has not drawn yet. The owner named a pre-warm.

- **The decision**: in the browser the Tape (`mutantTape`, over every Demo screen) draws, in its screen's first frames,
  the letters the kit's words are made of — Latin, Russian, digits, the design's punctuation — in each size the design
  gives each role (display 22–28, OSD 14–20, text 11–16 in both weights), one size a frame, at alpha 1/255 and laid out
  to the screen's width so none is culled; then nothing. Elsewhere it draws nothing (`firstFrameWaits`).
- AC: every test and golden unchanged (the desktop draws nothing more); the screen looks as before; measured by
  B-151's protocol on the phone against B-164.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/GlyphWarmUp.kt`

## Findings

### 2026-11-22

- Phone proxy (2 counted rounds of 20 s, against B-164's arm): a tab's first visit's longest interval 67, 67, 83, 67,
  50, 50 ms against 67, 50, 50, 50, 50, 50; the screen in its first second (a screenshot) shows nothing of it.
- **Not held on the phone**, [by B-151's protocol](../../logs/b-151/2026-11-22-round3-1.log) (valid, against B-164 in the
  same run): a first visit's longest interval 66, 83, 66, 100, 83, 100, 83, 100, 83 ms against 83, 83, 83, 66, 83, 83,
  83, 100, 83 — the same. The proxy's frame it saved was the Mac's: the phone's first visit is not in its glyphs. Taken
  out; the item stays as the record.
