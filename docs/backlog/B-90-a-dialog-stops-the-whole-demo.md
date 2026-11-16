---
id: B-90
title: "A dialog open in the demo stops every screen and the Tape"
status: done
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

## Findings (2026-11-16)

**Seen.** On the Pages build the demo opens with the Settings screen's dialog showing, and nothing behind it moves.
- One clock, the theme's, ran everything on the page: all three phones, the page's Tape and the header.
- The dialog held that clock, so the Library screen never reached the 3.5 s of a 9 s window where its keyframe loss starts.
- `ScreenClockTest`'s control reproduces it: without a screen clock, a dialog stops the theme's clock and both screens (0 ms moved in 2 s).

**The decision.** A screen gets a clock of its own: `MutantScreenClock { }`, public. It follows the theme's reduce motion, and a dialog, bottom sheet or drawer opened inside holds it alone. A consumer showing two screens side by side, or one over another, meets the same need the demo has. The demo gives each phone one, so the Settings dialog stands the Settings screen and nothing else.

Rejected: the dialog closed by default. That hides the problem in the demo and leaves it for a consumer.

**Tests.**
- `ScreenClockTest`, three cases:
  - a dialog holds its own screen's clock alone, while the screen beside it and the theme's clock move 2 s in 2 s;
  - a keyframe loss beside a screen held by its dialog runs within 12 s;
  - without screen clocks, everything stands (the control).
- `DemoClocksTest`: on the demo page, with the dialog open, the Library screen's pixels change over a second.

A mutant making `MutantScreenClock` provide no clock of its own fails three tests: the first two cases above and `DemoClocksTest`.

The public API adds one function; the ABI dump is updated.
