---
id: B-136
title: "The Tape's tracking band moves on every frame"
status: done
priority: P2
size: S
stage: stage-6-later
---

# B-136 — The Tape's tracking band moves on every frame

The owner, 2026-11-18: the noise over the screen is limited by the kit's ticks and goes in steps; it should run on
every frame, as the transitions do since [B-100](B-100-motion-on-every-frame.md). «Помехи» is the owner's word for the
Tape ([B-101](B-101-demo-tape-twice.md)).

`Modifier.mutantTape()` drew the whole Tape at `sceneTick · 125 ms` (`MutantTape.kt`), eight times a second, as the
engine's `stageStep` does. The grain and the head's noise are random per tick by design; the tracking band is the one
part that moves. Stepped, it rose `0.06 · (H + 30)` dp at a time: 49 dp a step on an 800 dp screen, in 17 jumps over
its 2.1 s pass. On every frame at 60 Hz it rises 6.6 dp a frame.

- **The decision and its reason.** The band is movement, so under B-100's rule it runs on every frame of the screen's
  clock; what the Tape looks like stays on its tick, as a body's boil does (D6, D22).
  - `tapeTimeMs(clock, steps, h)` gives the draw the time now and reads `clock.frame` only while the band passes. It
    reads the scene's tick always, and the band starts on one (7000 ms is 56 ticks), so at rest, 4.9 s of every 7, the
    Tape is still drawn once a tick and asks for no draw between.
  - The grain's offset and the noise's stream are `floor(t / 125)` of that time, which is the scene's tick: they step
    as before. The noise inside the band rides with it.
  - Under `MutantTheme(steppedMotion = true)` the whole Tape is drawn at the tick's start, as before and as the
    design draws it; the sample's «Stepped motion» hint now names the band.
  - Reduce motion (D7) is unchanged: the clock stands and asks for no frame, so the band stands where it was.
- Rejected:
  - the whole Tape on every frame, grain and noise included: they are a new random picture each step, and at 60 a
    second they would be a different, harsher texture than the design's eight, which the owner did not ask for;
  - the band read on every frame always, which redraws the screen's root layer at 60 Hz through the 4.9 s of each
    cycle the band is away, for a picture that changes eight times a second.
- Not covered: the keyframe loss ([B-48](B-48-keyframe-loss.md)): its blocks drag a copy of the picture once per
  125 ms step, cumulatively, so a step is part of its look, not a limit on its motion.

- AC:
  - with the default theme, while the band passes, every frame of the Tape differs from the one before and its layer is
    drawn again, inside one scene tick too;
  - at rest, between bands, the Tape changes on the scene's tick only and its layer is not drawn between ticks;
  - under `steppedMotion` the Tape changes on the scene's tick only, the band included;
  - reduce motion, clean signal, density and the band's cycle hold as in [B-13](B-13-tape.md); no golden moves (they
    draw on frozen clocks).
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantTape.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/surface/MutantTapeTest.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/SampleStrings.kt`

## Findings, 2026-11-18

- **Built.** `tapeTimeMs` in `MutantTape.kt`; `mutantTape()` reads the theme's stepped motion.
- **Measured** (`MutantTapeTest`, 40 frames of 16 ms, a 250 × 130 dp Tape on a clock of 24 resting ticks a second):
  - from 200 ms, the band passing: 40 of 40 frames differ from the one before and redraw the layer;
  - the same under `steppedMotion`: the picture changes only on the 5 scene ticks crossed;
  - from 2000 ms, the band leaving: once it is gone, no draw inside a tick, a new picture on each tick.
- **What moved.** No golden, parity cell or reference: every one draws the Tape on a frozen clock, at its own time.
  `MutantTapeTest`'s «changes on the scene's tick and not between» started inside the band; it now starts at 2000 ms
  and checks draws as well as pixels.
- **Mutants**, each run after the commit and each killed:
  - the band on the scene's tick again (`steps` taken as always on): «while the band passes…» fails;
  - `clock.frame` read at rest too: «at rest…» fails on its draw count;
  - `steps` ignored (never stepped): «under stepped motion…» fails;
  - the time now without reading `clock.frame`: «while the band passes…» fails, the band standing still between
    ticks.
