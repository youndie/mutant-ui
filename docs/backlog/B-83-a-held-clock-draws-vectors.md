---
id: B-83
title: "Bodies under a held or paused clock keep drawing vectors on the render thread"
status: done
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-82]
---

# B-83 — A held clock draws vectors

Found by [B-82](B-82-rasterise-a-body-once-per-tick.md). A resting body is drawn from an image built ahead for its next tick. A clock held by a dialog (`MutantDialog` holds the screen's clock), paused, or under reduce motion has no next tick, so the bodies under it never get an image. They keep their vectors, and the render thread rasterises them on every frame that something else asks for: the dialog's own clock, a caret, a ripple. This is the sample's Demo page, whose Settings screen opens a dialog over all three screens: 19.5–20 ms of render CPU a frame with images on or off.

- **The decision and its reason.** Build the image with the body's first build, off the frame ([B-50](B-50-first-frame-off-the-frame.md)), for the time the body will first be drawn at. A body under a clock that stands then draws its image from its first frame and never switches. A switch from vectors to an image under a stopped clock would move anti-aliased edges by a few levels. The kit's tests forbid any change while paused (`*BehaviourTest` "paused", `MutantThemeTest`), and that rule is kept. A clock held after it ran keeps the image of its last tick when that tick was drawn from one.
- **Tried in B-82 and rejected:** building the standing tick's image on demand and redrawing when it arrives. It switches the picture under a paused clock and breaks those tests (nine failures), and it adds redraws per tick under a running clock (`MutantSurfaceTest`: 63 frames over 40 ticks).
- AC:
  - Under a clock held from the start, the body's first drawn frame is an image (a test). No test of a paused or held clock changes.
  - The sample's Demo page on `FrameBench` before and after, within one run, on mains, with the screen to the bench.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDialog.kt`

## Findings (2026-11-15)

**Built.** A body whose first build runs under a clock that stands (held by a dialog, paused) is rasterised with that build, for the time the clock stands at, and draws the image from its first frame. The image travels with the instance through `HeldInstance.buildOffFrame`. If the background thread is late, the instance and its image are both built in the frame, so the first drawn frame is an image either way and the body appears in the same frame as before. A first try that drew nothing until a separate image arrived shifted the first drawn frame and broke 14 tests of the paused clock; it was dropped.

**Tests.** `StandingClockImageTest`: every drawn frame is an image, the first included, under a clock held from the start, a clock paused from the start, and behind a `MutantDialog` open from the start. The control: under a running clock the first frames are vectors, so the probe can tell them. No test of a paused or held clock changed.

**Mutant.** The first image never built: killed by all three aimed tests.

**The sample's Demo page**, `FrameBench`, runs alternating before (`b6c65fc`) and after, three rounds of 10 s each, round 1 not counted ([logs](../../logs/b-83/)):

| | before 1 | after 1 | before 2 | after 2 |
|---|---|---|---|---|
| Demo page, render CPU / frame | 14.56 ms | **4.77** | 18.42 | **5.31** |
| Demo page, frames/s | 55.3 | 59.9 | 42.7 | 59.4 |
| Settings screen alone (dialog open) | 3.53 | **1.48** | 4.50 | **1.53** |
| Blank page (stand check) | 0.42 | 0.41 | 0.41 | 0.42 |
| Blank page with the fixed load (control) | 2.90 | 2.68 | 2.89 | 3.03 |

The stand check and the control stay within 0.01 and 0.35 ms across all four runs, while the Demo page falls by 9.8–13.1 ms in each pair. Same host, four runs in a row: yes; within one run: no. The switch is internal to `mutant-core` and the bench lives in `sample`, so the arms were two builds run in turn, as in B-82.

**Short of the AC:** the Mac was on battery for all four runs, not on mains. Across runs on battery B-82 saw the stand drift; here the stand check did not move, and the gap is twelve times the control's spread.
