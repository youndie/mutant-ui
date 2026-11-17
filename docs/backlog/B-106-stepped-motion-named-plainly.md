---
id: B-106
title: "The header's «Шаги дизайна» switch says nothing of what it does"
status: done
priority: P3
size: S
stage: stage-6-later
---

# B-106 — The header's «Шаги дизайна» switch says nothing of what it does

The owner, 2026-11-17: «Design's steps — не понял, что это в ките».

The switch is B-100's: `MutantTheme(steppedMotion = true)` steps the transitions as the design's engine does, 12 times
a second (the venom 24), where the kit's default moves them on every frame (D22). Its label named where the setting
comes from, not what it does, and nothing on the page explained it.

- **Decision.**
  - The label says what the switch does: «Движение рывками» / "Stepped motion".
  - On hover, keyboard focus or a long press, the kit's own tooltip says the rest: on, transitions move 12 times a
    second and the venom 24, as the design's engine draws them; off, on every frame; the boil and the drips keep their
    own tick either way.
  - The switch stays: it is how the owner compares the two, and the kit's parameter is public.
- AC:
  - the header names the switch plainly in both languages, and resting the pointer on it shows the explanation
    (`SteppedMotionSwitchTest`); the test fails with the tooltip taken off;
  - the words have their glyphs in both faces (`SampleAppTest`).
- Anchors: `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/MutantUiSample.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/SampleStrings.kt`
