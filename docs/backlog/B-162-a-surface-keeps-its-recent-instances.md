---
id: B-162
title: "A surface keeps its recent instances, and in the browser builds a new one between frames"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: [B-156]
---

# B-162 — A surface keeps its recent instances, and in the browser builds a new one between frames

A log of every instance built in a frame, over eight tab switches in the phone proxy: the navigation bar's body was built
anew at most switches. Its clean zone is the union of its content's, the pill's among them, and the pill's box reaches
past the icons' at either end: the zone, so the signature, so the instance, changed as the pill went to or from the first
and the last tab — a 344 × 68 body's static field, boil noise and frames rebuilt in the frame. At the screen's start the
chips and the card did the same as the font arrived a frame late and their words widened.

- **The decision**: a surface keeps the last three instances it held, by signature, each with its latest frame done, and
  takes one back when its signature returns (`BodyKeep.of`): the bar's three zones are built once. In the browser, a
  signature the surface never held is built between frames while the instance it has goes on drawing (B-153's way), never
  in the frame. The bar's look is unchanged: its zone still follows the pill, as the engine's does.
- AC: every test, parity and golden unchanged; measured against the arm before it.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyAhead.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`

## Findings

### 2026-11-21

- Phone proxy, 2 counted rounds of 20 s against the arm before it (B-161): tab switches longest p50 17 against 67 ms,
  p95 67 against 183, missed 0.7 % against 3.1 %; scroll held on both.
