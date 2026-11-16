---
id: B-90
title: "A dialog open in the demo stops every screen and the Tape"
status: open
priority: P1
size: S
stage: stage-6-later
---

# B-90 — A dialog open in the demo stops every screen and the Tape

The owner, 2026-11-16, running the sample, gave two remarks with one cause:
- «в демо нет эффекта потери кадра» — the demo shows no keyframe loss;
- «при входе в demo останавливаются эффекты (там диалог открыт)» — entering the demo stops the effects.

The Settings screen's dialog is open by default (`DemoScreenFrame(dialogOpen = true)`). A dialog holds the clock of the screen it stands over (B-83, `clock.holds`). The sample has one clock, the theme's, for all three demo screens, the header and the Tape over the whole page. So the hold stops everything: no tick, no Tape, and no keyframe loss, which runs only on a live clock (`MutantKeyframeLoss`: `clock.holds == 0`).

- **The decision and its reason.** To be decided when taken; candidates:
  - each demo screen its own clock, so a dialog stops only the screen it stands on. This needs a public way to give a subtree a clock of its own, since `ProvideMutantClock` is internal; a consumer showing two screens side by side meets the same thing;
  - the dialog closed by default, opened by a button on the Settings screen. This hides the problem rather than fixing it.
- AC:
  - On entering the demo, Library and Recording tick and the Tape moves while the Settings dialog is open.
  - Library's keyframe loss runs every 9 s.
  - A test holds that a dialog stops only its own screen's clock.
- Anchors: `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/DemoPage.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDialog.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/clock/MutantClock.kt`
