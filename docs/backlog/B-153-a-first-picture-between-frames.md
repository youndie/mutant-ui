---
id: B-153
title: "In the browser, a body's first picture is built between frames, whole"
status: done
priority: P1
size: M
stage: stage-6-later
blocked_by: [B-151]
---

# B-153 — In the browser, a body's first picture is built between frames, whole

From [B-151](B-151-a-real-screen-under-a-finger.md)'s profile of a tab switch on the phone: of its 543 ms, the
generator's `frame` is 211 and the bodies' brushes 66, most of both in the frames right after the tab's first. B-149
left the browser's first frame a stand-in and started each body's build as a task of its own; but that task waited one
frame only, and a build not done by then was made in the frame instead (`HeldInstance.buildOffFrame`). In the browser
the tasks run at the scheduler's `background` priority, after the frames, so on a tab switch they are late for nearly
every body: the next frame built them all. And what the task built was the instance and a warm frame at the clock's
time; the frame that took it built the tick's frame and the brush again.

- **The decision**: where the frame cannot wait (the browser), the first build is never made in a frame. The stand-in
  stands until the task is done, and the task builds the whole first picture: the instance, the frame of the resting
  tick it will be drawn at (its grid grown) and its brush or image, handed over as the tick's frame built ahead
  (`FrameAhead.preset`). A body's first draw takes that picture even when the clock has moved a tick on since
  (`FrameAhead.take(orEarlier)`), and asks for the next tick's as every draw does. Rejected: keeping the fallback with a
  longer wait, which on a busy thread still builds in a frame.
- AC:
  - `FirstFrameOffTheFrameTest`: where the frame cannot wait, the stand-in stands until the build is done, the body is
    built once and drawn;
  - every other test, parity and golden unchanged (the desktop, Android and iOS frame waits, B-149);
  - measured by B-151's protocol against `main` on the phone.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyAhead.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`

## Findings

### 2026-11-21

- On the phone, [arms 1](../../logs/b-151/2026-11-21-arms-1.log) (a comparison, no floor, throttled): with B-152, tab switches longest p50 83 against B-152's 150 ms
  and `main`'s 216, max 815 against 266 (a first build late behind its stand-in, B-161).
