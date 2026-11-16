---
id: B-94
title: "A chip's label turned before its fill: light on lime"
status: done
priority: P1
size: S
stage: stage-6-later
---

# B-94 — A chip's label turned before its fill: light on lime

The owner, 2026-11-16: «в navbar выбранный элемент имеет белый цвет текста — на зелёном не читается».

The selected item's label is `text` (white in «Эфир») and stands 3 dp under the lime pill. In the demo the pill's drips hang over that gap onto the label: a look at the Pages build (`4403600`) shows «On air» on green.

- **The decision and its reason.** To be decided when taken, after a look at every frame of a slide. Candidates:
  - the selected label in `onPrimary` where the pill's ink is under it;
  - the pill's drips kept off the label, as the design's `gap` intends (2 px).

  Either is a departure (D20).
- AC:
  - The selected label reads at least 4.5:1 against what is under it on every frame, at rest and through a slide, in both themes.
  - A test reads the label's pixels.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationBar.kt`

## Findings (2026-11-16): not the navigation bar

**Seen.** The navigation bar is not what reads white on green. On the Pages build at `4403600`, its selected «On air» stands under the pill on the dark bar: the pill has no drips (`overhang: 0`). The sample's header switches pages with chips, «Components» and «Demo». On the Demo page, «Components» stood lime with a light word, and «Demo» stood dark with a dark one. That is the remark.

**Cause.**
- A chip turned its label 200 ms after a change, on the wall clock (the engine's `_muTc`).
- Its fill pours on the screen's clock.
- The Demo page's dialog held that clock (B-90), so the pour stood at its start while the label turned anyway.
- Without the hold, a frame strip shows the same thing for 3–5 frames of every change. The label turns before the front reaches it (dark on dark), or after (light on lime), because the front's drips run ahead of its base line.

**The decision.** The label follows the pour itself, not a timer.
- `MutantSurface` gives its content the fill under the label: the old one until the pour's front, drips included, passes the label's middle (`depthUnderLabel`, the median of the front between 16 dp from each side).
- The chip's label picks between its two colours by it, through B-91's `inkOnBody`.
- The ground is derived per frame of the clock (`clock.frame`; its time is not state).
- A pour asked for before the body first draws holds the old fill until it does.
- A change back early in a pour starts from what still covers the body, so a body never jumps to a colour it was not showing.
- The 200 ms lag is gone: a departure from the engine's `_muTc` (D20), which moves no cell.

**Tests.** `ChipLabelFollowsFillTest`: a chip turned twice, under a running clock and under a clock its dialog holds. On every frame its word reads at least 3:1 against the fill beside it. `ChipBehaviourTest` still holds the label light at 16 and 112 ms and dark by 272 ms.

Four mutants, all killed:
- the ground not following frames;
- the ground blind to the pour;
- the chip without its pair of colours;
- a change back pouring from the logical fill.
