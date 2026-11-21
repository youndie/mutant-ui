---
id: B-159
title: "The keyframe loss's snapshot is painted ahead, a body a task"
status: done
priority: P1
size: M
stage: stage-6-later
blocked_by: [B-158]
---

# B-159 — The keyframe loss's snapshot is painted ahead, a body a task

With the band in memory ([B-158](B-158-the-loss-band-in-memory.md)) a loss's steps are smooth and its start is the stop:
the bodies meeting the band drawn by paths into it in the frame that starts it, 250–400 ms in the phone proxy. A loss's
start is known ahead (every 9 s, 3.5 s into the window), and so is its band (`moshPlan` of the scene's count).

- **The decision**: from 1.5 s before the next loss, each body its band may meet — with 400 dp of room for a scroll
  meanwhile — paints its picture for the snapshot (its box and its reach, by paths) into a bitmap of its own and reads it
  out, a task of its own between frames. The loss's first frame lays the pieces into the band at the bodies' places then,
  and draws by paths only a body with no piece. In the browser only.
- AC:
  - the keyframe loss's tests and goldens unchanged (the desktop keeps its strips);
  - measured by B-151's protocol against `main` on the phone.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`

## Findings

### 2026-11-21

- The first build read the pieces a byte a call: Compose's bitmaps are Skia's `BGRA_8888` in the browser, which the
  one-block read did not take. With `BGRA` read in one block and its bytes turned, phone proxy (2 counted rounds of 20 s,
  against B-158's arm): scroll **held**, the longest interval 33 ms at p95 and at most, 0.1 % missed (B-158's arm: 350 and
  400 ms, 0.2 %).
