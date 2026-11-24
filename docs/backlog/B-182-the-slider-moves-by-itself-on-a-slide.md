---
id: B-182
title: "The slider moves by itself on the slide curve"
status: wip
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
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSlider.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSwitch.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyHeat.kt`
