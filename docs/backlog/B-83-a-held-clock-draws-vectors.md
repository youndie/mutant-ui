---
id: B-83
title: "Bodies under a held or paused clock keep drawing vectors on the render thread"
status: open
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
