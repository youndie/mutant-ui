---
id: B-149
title: "A body drawn in its first frame"
status: wip
priority: P1
size: L
stage: stage-6-later
blocked_by: []
---

# B-149 — A body drawn in its first frame

The owner, 2026-11-20, a new round: «И нужно уйти от пропуска рендера на первом кадре».

Since [B-50](B-50-first-frame-off-the-frame.md) a body's first instance is built off the frame: the frame it first
appears in draws its content and nothing of the body (`MutantSurface.kt`, `return@drawWithCache onDrawBehind {}`), and
the body appears from a later frame. In the browser "off the frame" is a task on the page's own thread
(`AheadDispatcher.wasmJs.kt`), so there the body comes as late as that queue lets it. With one group of the Components
page at a time (B-148) every switch shows a page of bare content first.

**What it costs today** ([log](../../logs/b-149/2026-11-20-appear-desktop.log), B-50's `AppearBench`, this M1, JDK 25,
`main` at `66d9c57`, ten 328 × 80 cards appearing in one frame, arms interleaved, rounds 2–6): built in the frame,
**138.4 ms** for that frame, 113.6 of it drawing; off the frame, 18.9 ms. B-50 read 85.8 and 12.8 on the same machine on
2026-11-14: the first build has grown with the performance loop (B-82's tick image, B-117's still layers), while warm
frames have not.

- **The decision.** To be decided from the numbers. A body is to be drawn in the frame it first appears in, on every
  platform, at a cost a frame can carry; a route is chosen from where the first build's time goes.
- **Pre-registered, the first measurement, before any change.** The in-frame arm of `AppearBench` sampled by a
  profiler on the JVM (JFR, or async sampling), ten cards: the share of frame N under `gen.instance`, the dead pixels,
  the first `gen.frame`, `pictureOf`, the still layers' image, the tick image and the vector draw. Each a share of frame
  N with its log.
- Routes known before it, none chosen:
  - the first build made cheaper where it is expensive, the generator's numbers unchanged (its goldens hold them);
  - the first build split: what the first frame must draw built in it, the rest from the next frame without a change
    a viewer can see;
  - on platforms with threads, every body of a frame built in parallel from its layout and joined at its draw.
- AC: the profile, with its log; a route measured by `AppearBench` (and the browser bench in the phone proxy, the phone
  being away) against `main`; every body drawn in its first frame, held by a test that fails on `main`; the goldens,
  parity and numeric goldens unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/bench/AppearBench.kt`
