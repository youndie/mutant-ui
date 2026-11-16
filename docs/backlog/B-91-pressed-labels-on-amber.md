---
id: B-91
title: "A pressed body's light label stands on amber, unreadable"
status: open
priority: P1
size: S
stage: stage-6-later
---

# B-91 — A pressed body's light label stands on amber, unreadable

The owner, 2026-11-16: «при клике на кнопку (не primary) текст белый по оранжевому — плохо читается».

A press boils a body: its fill mixes towards `amber` (#FFB000) and reaches it in about 280 ms (`boilFill`). Labels that keep a light colour on it:
- the chip's and the card's `text` (#E9F2E4 in «Эфир»);
- the list tile's icon;
- the outlined button's `textAccent` (acid green).

The icon button already switches its tint to `onPrimary` while boiling (`MutantIconButton`), so the kit has the answer once.

- **The decision and its reason.** To be decided when taken. The candidate: every label and icon on a body turns to `onPrimary` while that body boils past the point where its fill is amber, by one rule shared with the icon button. Read the contrast of every frame of the press and the release (cmp-ui-component §4), not only rest and pressed. It changes what the design draws, so it is a departure (D20).
- AC:
  - No frame of a press or release of a chip, a card, a list tile, an outlined or text button reads under 3:1 between its label and the fill under it, in either theme, by a real press.
  - The test reads the label's pixels.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyStates.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantButton.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantChip.kt`
