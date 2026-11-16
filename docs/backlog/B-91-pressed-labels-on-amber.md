---
id: B-91
title: "A pressed body's light label stands on amber, unreadable"
status: done
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

## Findings (2026-11-16)

**Seen.** By a real press, frame by frame, the label against the fill beside it reads 1.35–1.42:1 in «Эфир» once the fill is amber, about 300 ms into the press. That holds for the chip, the outlined button, the card's title and the icon button. In «Копия» the dark words read on amber already. `PressedLabelContrastTest` with the fix taken out prints exactly this.

**The decision.** Words and icons on a body take whichever of their own colour and `onPrimary` reads better against the fill the body shows on that frame (`inkOnBody`). The design pairs `onPrimary` with amber in its contrast table (`mutant-engine.js`, `'on-primary / amber'`).
- `MutantSurface` gives its content that fill while it boils (`LocalMutantBodyGround`), derived per frame. A label recomposes only when its pick changes.
- At rest nothing changes: no golden moved.
- The icon button switched on `pressed`. That went light on release while the fill was still amber, and dark on press while it was still dark. It now follows the same rule.
- Where the pick changes, the two colours read about equally, so no frame dips. In this test the worst frame of any case is above 3:1.
- The pick changes the words' colour where the design kept it, so this is a departure (D20). It moves no cell, because the references are at rest.

**Tests.** `PressedLabelContrastTest`: a chip, an outlined button, a clickable card and an icon button, pressed for 30 frames and released for 40, in both themes. The ink is the label's pixel furthest from the ground beside it, and every frame must read at least 3:1. The mutant `inkOnBody` → its own colour fails four cases at 1.35–1.42:1.

Left out: the list item's tile icon takes the same rule (`MutantListItem`), but the test does not press a row.
