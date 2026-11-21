---
id: B-155
title: "In the browser, no resting tick is built in a frame"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: [B-153]
---

# B-155 — In the browser, no resting tick is built in a frame

From [B-151](B-151-a-real-screen-under-a-finger.md)'s profile of a tab switch and [B-153](B-153-a-first-picture-between-frames.md):
once a body has drawn, its next resting tick is built ahead only after its field cache has settled — its static layers
and all four boil phases filled — because on the JVM, Android and iOS the build runs on another thread while the
frame's reads the cache (B-75). Until then, and whenever the tick built ahead is not done when the clock reaches it,
the tick is built in the frame. In the browser that is the next four ticks of every new body, in the frames right after
a tab's first, and any tick the thread was too busy to build: frames of 50–170 ms.

- **The decision**: in the browser (the frame cannot wait, `firstFrameWaits` false), the next tick is built ahead from a
  body's first draw, its grid grown in the task: one thread, so nothing reads the cache while the task writes it. And a
  resting tick is never built in a frame there: the latest tick done for the instance is drawn, a tick or more behind
  the clock, while the next waits for the thread (`FrameAhead.take(orEarlier)`). The boil then steps when the thread has
  time. Rejected: a deadline in the frame, which still builds there when it is missed.
- AC:
  - the desktop, Android and iOS unchanged (their frames wait, B-149; every test, parity and golden as before);
  - measured by B-151's protocol against `main` on the phone.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyAhead.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`

## Findings

### 2026-11-21

- On the phone, [arms 2](../../logs/b-151/2026-11-21-arms-2.log), by the protocol (valid: the floor held in every arm), against `main`: tab switches longest p50 50
  against 133 ms, p95 150 against 283, missed 4.1 % against 15.3 %; scroll p95 449 against 1196, missed 1.4 % against
  5.8 %. Not held on either gesture.
