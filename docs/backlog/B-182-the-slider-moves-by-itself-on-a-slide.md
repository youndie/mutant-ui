---
id: B-182
title: "The slider moves by itself on the slide curve"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-182 — The slider moves by itself on the slide curve

What the slider moves by itself — a tap on the channel (up to 234 dp), the settle at release, a value set by the app — is a one-frame cut (F-slider-05, F-slider-06). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 4 ([решение]).

- **The decision and its reason.** Those moves take 330 ms on the `slide` curve, as the switch's thumb does, and the thumb boils for that time as when dragged. An arrow step stays one frame, and the thumb gets a heat impulse of 0.8.
- The answer's «4 шага» are the steps `steppedMotion = true` draws; by default the move runs on every frame, as D22 has the switch's thumb (research D24, this repository's reading).

- AC:
  - a tap on the channel moves the thumb over 330 ms on the slide curve, no step over the curve's bound per frame; under `steppedMotion` four steps
  - an arrow step lands in one frame and the thumb's heat reads 0.8 on that frame
  - a drag still follows the pointer 1:1
  - under reduce motion the finished frame shows at once (research D24)
  - each new test of a move that changes (the tap, the steps, the settle and the app's value, the key's heat) fails on the code before; the two that guard what stays (the drag, reduce motion) hold before and after, as the code before already did both; each kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at *(rewritten 2026-11-24: as first written, a test of behaviour the code already had could not fail before it)*
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSlider.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSwitch.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyHeat.kt`

## Result

### 2026-11-24

- **Looked at first**: throwaway contact sheets (`/private/tmp/b182/before/`, `after/`, not in the repository) of a 300 dp
  slider at −8: a finger's tap at +8, 26 frames; focus and an arrow; the app setting −10, 26 frames; in «Эфир» at density
  1 and «Копия» at density 2, on a running clock, under `steppedMotion` and under reduce motion. Before: the tap moved
  the handle 213 dp in one frame, the app's −10 47 dp in one frame. After: both glide frame by frame with the fill behind
  them, the handle boiling; four steps under `steppedMotion`; reduce motion's sheets byte-identical to `main`'s.
- **The glide** (`SliderGlide` in `MutantSlider.kt`): what the handle does by itself (a press away from it, the settle at
  release, a value the app or a reader's `setProgress` sets) starts a glide from where it is drawn. The way back to it
  closes in `MutantMotion.SLIDE_MS` (330 ms) as `(1 − p)²`: the curve of the switch's thumb (`slid`, `MutantSlideRun`),
  with `p` from the kit's `stepped`, so on every frame by default and in the hot tick's four steps under `steppedMotion`
  (D22, D24's reading of «4 шага»). It runs on the screen's clock (D6) and reads every frame only while it runs. A glide
  re-aimed while it runs starts from where the handle is drawn. No stretch: the switch's thumb is an empty body, and the
  slider's handle carries its number, which never scales (designed here, D15).
- **A drag** moves with the pointer 1:1: its moves never start a glide, the way left (if a glide ran) closing under it. A
  handle taken by itself stops where the pointer took it: the gesture tells the glide (`grab`), as the composition that
  sees the press comes a frame later and the glide would have run on under the pointer for that frame (12 px in the
  test that found it). A mouse pressed away from the handle, which takes it at once, glides it to the pointer.
- **The boil**: the handle's heat is the slider's own `HeatMachine`, on while it is pressed or gliding (answer 4: «ручка на
  это время кипит, как при перетаскивании»). The surface gets the machine instead of the interactions, because a tap's
  release comes before the glide it starts has ended. A hover on the slider's interaction source still reaches the
  outline's tremble (B-193), forwarded by the slider.
- **A key** lands in its frame and gives the handle a heat impulse of 0.8 (`HeatMachine.impulse`, B-201;
  `SLIDER_KEY_IMPULSE`), even mid-glide. Designed here (D15), as the answer names only the arrow's step: Home and End are
  keys too and land in a frame with the same impulse, and a key at the end of the range, which moves nothing, still
  kicks the heat. The new value must reach the slider in the key's own frame to land (the composition after it reads
  the key); a consumer that sets it a frame later sees it glide.
- **Under reduce motion**, and on any clock that stands (frozen, paused, held by a dialog), every move lands at once
  (D24); the impulse does nothing under the pause (B-201's rule).
- **Not changed**: a value the app sets while the handle is held is still shown at the pointer's place (F-slider-05, the
  owner's); the handle shows its new number as the glide starts.
- **Tests**, `SliderGlideTest` (density 1, a clock and the heat in the test's hands through `SliderBody(heat = …)`):
  - a finger's tap and a mouse's press on the channel from −8 to +8: the first frame draws the handle where it stood;
    every frame within 1.5 px of `way · (1 − p)²` on the clock's time; no frame moves it more than the curve's steepest
    stretch (`2 · way / 330` a ms, +1 px); landed at +8; boiling through the glide and let go once landed (AC 1);
  - under `steppedMotion` the same tap moves the handle exactly 4 times, each step on the stepped curve (AC 1);
  - a release 6 dp right of the value settles on the curve, and a value the app sets glides on it (AC 1);
  - an arrow and End: the heat reads exactly 0.8 in the key's frame, and the handle stands at the new value in the next
    frame; an arrow mid-glide lands it (AC 2);
  - a drag of 8 × 7 dp at rest and out of a glide: the handle stands still under the pressed pointer and then moves
    exactly with it, ±1 px (AC 3);
  - under reduce motion a tap and the app's value land in the next frame (AC 4).
- **Before the change** (`main`'s `MutantSlider.kt` with only the test seam `heat` added and passed to the surface), four
  fail: the tap (`the glide's first frame draws it where it stood`), the steps (no move in 30 frames, landed already),
  the settle (`the handle at 150.0, 156.0 on the curve`), the key (heat `0.0`). The drag and reduce-motion tests pass
  there, as the code before already did both (AC 5 rewritten for it).
- **Mutants**, after `87e57bad`, each against its test in `SliderGlideTest`, read from its JUnit XML, the file copied back
  and `git status` clean after each:
  - boiling only while pressed: killed, the tap (`a finger: boiling through the glide`);
  - `stepped(…, false)`: killed, the steps (a move on every frame);
  - a release that lands at once: killed, the settle (`the handle at 150.0, 156.0 on the curve`);
  - no impulse on a key: killed, the key (heat `0.0`);
  - no `grab`: killed, the drag (`glide true: at 134.5 after 7.0, 131.5 wanted`);
  - reduce motion not standing the glide (`still = clock.frozen`): killed, reduce motion (`a tap: at 44.5, 256.4 wanted`).
- **Goldens and parity cells**: none moved. They draw on a frozen clock, where every move lands at once.
  `SliderBehaviourTest` (15) and `SliderParityTest` pass unchanged.
- **Gates on this Mac**: `make check` green; `./gradlew check` green (614 tests in `mutant-core`'s desktop suite and 50 in the
  sample's, none failed, read from the JUnit XML; `checkKotlinAbi` unchanged, as `SliderBody` is internal);
  `./gradlew :mutant-core:compileKotlinIosArm64` green; `make docs-against BASE=origin/main` clean.
- **Not verified**: a phone, a real window and the browser (no device, adb or real browser was used); right to left by a
  test of the glide (the glide works on the fraction from the start, which the RTL test of B-168 already maps, and
  `SliderBehaviourTest`'s RTL test passes under reduce motion only).

