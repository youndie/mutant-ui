---
id: B-136
title: "The Tape's band and head noise move on every frame"
status: done
priority: P2
size: S
stage: stage-6-later
---

# B-136 — The Tape's band and head noise move on every frame

The owner, 2026-11-18: the noise over the screen is limited by the kit's ticks and goes in steps; it should run on
every frame, as the transitions do since [B-100](B-100-motion-on-every-frame.md). «Помехи» is the owner's word for the
Tape ([B-101](B-101-demo-tape-twice.md)). Having seen the band glide in the sample the same day: «ряб снизу тоже надо
сделать без ограничения по фпс» — the head's noise at the bottom edge too.

`Modifier.mutantTape()` drew the whole Tape at `sceneTick · 125 ms` (`MutantTape.kt`), eight times a second, as the
engine's `stageStep` does:
- the tracking band rose `0.06 · (H + 30)` dp at a time: 49 dp a step on an 800 dp screen, in 17 jumps over its 2.1 s
  pass. On every frame at 60 Hz it rises 6.6 dp a frame;
- the head's noise, `round(W / 10)` strokes in the bottom 12 dp and 14 in the band, was drawn anew from the stream
  `hd` of the stage and the tick, so it changed eight times a second.

- **The decision and its reason.** The band and the head's noise run on every frame of the screen's clock; the grain
  keeps the scene's tick, as a body's boil does (D6, D22).
  - `tapeMoment(clock, steps)` reads `clock.frame` and gives the draw the time now, for the band, and a key for the
    noise's stream, `@` and the whole millisecond of that time: a new draw of the noise each frame, the same one for
    the same time, alike on every target.
  - The grain's offset is `floor(t / 125)` of the time now, which is the scene's tick: it steps as before.
  - Under `MutantTheme(steppedMotion = true)` the whole Tape is drawn at the tick's start with the tick's noise, as
    before and as the design draws it; the sample's «Stepped motion» hint names the band and the noise.
  - The noise is always on the screen, so the Tape now draws on every frame while the clock runs. What it covers is
    drawn in a graphics layer of its own (`mutantTape()` ends in `graphicsLayer()`): a frame of the Tape records the
    Tape and replays the content, rather than drawing the screen again.
  - Reduce motion (D7) is unchanged: the clock stands and asks for no frame, so the band and the noise stand where they
    were. Frozen clocks (fixtures, references) draw the tick's noise, as the engine's `data-mu-t`.
- Rejected:
  - the grain on every frame too: it is the screen's texture, not motion, and the owner kept it after seeing the band;
  - the band alone, reading frames only while it passes (this item's first version), which left the Tape at rest
    drawn once a tick: the owner wanted the noise on every frame as well;
  - the noise keyed by the frame's number: the same time would draw differently from run to run and test to test.
- Not covered:
  - the keyframe loss ([B-48](B-48-keyframe-loss.md)): its blocks drag a copy of the picture once per 125 ms step,
    cumulatively, so a step is part of its look, not a limit on its motion;
  - what drawing the Tape on every frame costs a frame on the JVM, in the browser and on the phone: not measured
    here; `FrameBench` and `tapeRenderBench` (B-76, B-77) read it.

- AC:
  - with the default theme, every frame of the Tape differs from the one before and its layer is drawn again: the
    band while it passes, the head's noise always;
  - the grain changes on the scene's tick only;
  - the content under the Tape is not drawn again for the Tape's frames;
  - under `steppedMotion` the Tape changes on the scene's tick only, the band and the noise included;
  - reduce motion, clean signal, density and the band's cycle hold as in [B-13](B-13-tape.md); no golden moves (they
    draw on frozen clocks).
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantTape.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/surface/MutantTapeTest.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/SampleStrings.kt`

## Findings, 2026-11-18

- **Built.** `tapeMoment` and `drawTape(noiseKey)` in `MutantTape.kt`; `mutantTape()` reads the theme's stepped
  motion and puts what it covers in a layer.
- **Measured** (`MutantTapeTest`, 40 frames of 16 ms, a 250 × 130 dp Tape on a clock of 24 resting ticks a second):
  - from 200 ms, the band passing: 40 of 40 frames differ from the one before and redraw the Tape;
  - from 2200 ms, the band away: the bottom 12 dp differ on 40 of 40 frames, the rows above only on the scene's ticks
    crossed; the Tape is drawn 40 times, a box under it not once;
  - from 200 ms under `steppedMotion`: the picture changes only on the 5 scene ticks crossed.
- **What moved.** No golden, parity cell or reference: every one draws the Tape on a frozen clock, at its own time.
  `MutantTapeTest`'s «changes on the scene's tick and not between» held the noise to the tick; it is now the noise on
  every frame and the grain on the tick.
- **B-38's floor.** With the band on every frame its 1 dp bright edge crosses the disabled button's word «Отключено»
  in «Эфир» for about six frames of each 7 s cycle, and `DisabledContrastTest` reads 3.89 there: it takes the most
  common colour in the word's box as its ground, and for those frames that is the edge. Under the band's body the
  worst is 4.38, as before; the stepped band never stopped with its edge in that box. Left to the owner.
- **Mutants**, each run after the commit and each killed:
  - the band and noise on the scene's tick again (`steps` taken as always on): «while the band passes…» fails;
  - `steps` ignored (never stepped): «under stepped motion…» fails;
  - the noise keyed by the tick in the default mode: «away from the band…» fails, the head's noise standing;
  - no `clock.frame` read: «while the band passes…» and «away from the band…» fail;
  - no layer under the Tape: «the content under the Tape…» fails.
