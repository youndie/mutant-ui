---
id: B-63
title: "Desktop: the hand cursor and a hover look"
status: done
priority: P2
size: M
stage: stage-5-product
blocked_by: [B-40]
---

# B-63 — Desktop: the hand cursor and a hover look

Found in [B-40](B-40-qa-pass-and-real-window.md) (F13), and confirmed in the real window: over `Play` the cursor stays the arrow and the button does not change. No component sets `pointerHoverIcon` or reads hover. [B-43](B-43-undesigned-behaviour.md) decided that what the design does not draw is designed here (D15), and hover is in that list.

- **The decision.** Every clickable component shows the hand cursor. A hover look is designed here, for `PointerType.Mouse` only, with every number the design does not state listed in this item; its goldens are recorded when built and revised in the owner's review round. The rejected alternative is the cursor alone: a mouse user then learns what is live only by pressing.
- AC: the cursor read through `pointerHoverIcon` in a test per component; the hover look held by a pixel test; a finger's press leaves no hover.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantButton.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/OverlayAndMouseTest.kt`

## Findings (2026-11-15)

**Built.**
- **The hand.** Every live control shows the hand cursor, through one internal modifier (`mutantHand`). The list: the buttons, the icon button and the FABs, the chip, the checkbox, radio and switch rows, the clickable card and list row, the tabs, the navigation items, the snackbar's and the dialog's actions, the segmented button's segments, and the slider's row. A disabled one keeps the arrow. The dialog's and the sheet's scrims and windows are no controls and keep it too.
- **The hover look (designed here, D15).** A body under the mouse simmers: its heat rises to 0.3, a press's boil being 1, at the press's own rate (320 ms to full), and cools at the release's (480 ms), so it boils at the hot tick and its fill turns a third of the way to amber.
  - **Mouse only.** Compose's `clickable` reports hover for a mouse alone, and a body hears it from the control's own interaction source. Checked first: a mouse gave Enter and Exit, a finger only Focus, Press and Release.
  - **What simmers.** Every control whose body shares its interaction source with its click: the buttons, the icon button and FABs, the chip, the checkbox, radio and switch, the card. Controls with no body of their own (the text button, the tabs, the navigation items, the snackbar's and dialog's actions, the segmented button's segments, the list row) show the hand alone. Their look on hover is left to the owner's review round.

**The cursor in a test.** Compose gives a test no way to read a node's hover icon, so the modifier that sets the icon also marks the node in its semantics (`MutantHandCursor`), and the test reads the mark.

**Tests.** `HoverAndCursorTest`:
- the hand over each of 16 controls and none over their disabled forms;
- under the mouse a button boils at the hot tick (11 or more changes a second against 9 or fewer at rest) and its acid fill gives way to amber (under half the lime pixels), and it settles once the mouse leaves;
- a finger's tap leaves no simmer.

Parity is unchanged: no fixture hovers.

**Mutants.** Killed:
- hover not heard;
- a hover heat of 0. Killed only once the test read the fill as well as the rate: the hot tick alone ran without heat;
- no hand;
- the hand over a disabled control.
