---
id: B-163
title: "The keyframe loss starts within a frame, and a path-drawn body builds ahead"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: [B-159]
---

# B-163 — The keyframe loss starts within a frame, and a path-drawn body builds ahead

From [B-151](B-151-a-real-screen-under-a-finger.md)'s run with all of round one on the phone: what kept a scroll over the
bar were two frames lost together at each loss's start, and what kept a return to Settings over it was 50 ms. A profile
of each, on the phone and in the proxy:

- while a loss ran, every body redrawn at a step combined its own copy of the band less its holes (`Path.combine`), at
  every frame;
- the loss's first frame painted the band's ground through Skia and read the whole band back to lay the pieces over it;
- the first loss of a page found none of its pieces: they were asked for once, 1.5 s ahead, when the screen's bodies were
  still stand-ins with nothing to draw;
- Settings' focused field (its brackets are among the decorations the shader does not draw, B-131) built its frame with
  paths in the frame at every tick, eight a second: its frame built ahead had none.

- **The decision**: the band less its holes is one path in the scene's pixels, made once while the band stands, and a
  body clips by it with its canvas moved to the scene's origin; the ground is filled in the band's memory, and Skia paints
  only a body with no piece; a body's piece is asked for again at every tick until the loss while it has nothing to draw;
  a body whose decorations need its paths builds its frames ahead with them, and the frame draws that one.
- AC: the keyframe loss's tests, parity and goldens and every other test unchanged; measured by B-151's protocol on the
  phone against round one.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`

## Findings

### 2026-11-21

[The run](../../logs/b-151/2026-11-21-round2-2.log), by B-151's protocol on the phone: round one (`main` now) against
this, interleaved, 4 rounds of 30 s, the phone at 34 °C and its big cores held at 1.4 GHz. **Valid**: both floors held.

| load | longest, p50; p95; max | windows over 33 ms | missed |
|---|---|---|---|
| scroll, round one | 17; 50; 50 ms | 6 (the loss's starts at 14 and 31 s into a load) | 0.2 % |
| scroll, this | 17; 50; 83 ms | 3 (the first loss of the page alone) | 0.1 % |
| tabs, round one | 33; 83; 100 ms | 25 | 1.7 % |
| tabs, this | 33; 100; 116 ms | 20 | 1.4 % |

- **Held for the loss's starts after the first**: none of them loses two frames now. The page's first loss, 3.5 s after it
  loads, still loses two to four: the page's first images and bands, its pieces asked for again but its bodies drawn for
  the first time only just before.
- **Not for Settings**: a return to it is 50 ms as before. Its field's frames no longer build in the frame, and what is
  left is the tab's own composing frame (B-151, round 2 owed).
