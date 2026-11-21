---
id: B-160
title: "A frame built in a frame is kept until the next tick"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: [B-155]
---

# B-160 — A frame built in a frame is kept until the next tick

A body redrawn on every frame — sliding home (the navigation bar's pill at every tab switch, 330 ms), pouring a new
fill, boiling under a press — asked the generator for its tick's frame at every frame, though the frame changes only at
the next tick.

- **The decision**: `FrameMemo` keeps the last frame built in a frame for its instance, time, heat, press and paths, and
  hands it to the frames after it until one of them changes.
- AC: every test, parity and golden unchanged; measured against the arm before it.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyAhead.kt`

## Findings

### 2026-11-21

- Phone proxy, tab switches: no difference above the run's spread (longest p95 150 against 133 ms, 28 taps a side). It is
  kept as it builds nothing twice; the time was elsewhere (B-162).
