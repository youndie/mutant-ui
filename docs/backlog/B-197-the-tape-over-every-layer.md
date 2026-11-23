---
id: B-197
title: "The Tape over menus, tooltips and dialogs"
status: open
priority: P1
size: L
stage: stage-6-later
blocked_by: []
---

# B-197 — The Tape over menus, tooltips and dialogs

The Tape does not cover popups: an open menu or a tooltip floats above it (F-menu-10); over the Settings phone it stands still while its dialog is open (F-screens-10). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 22 and 23 ([ошибка]).

- **The decision and its reason.** The Tape is the window's topmost layer, over menus, tooltips and dialogs, and it runs while a dialog is open.
- A popup is its own root, a separate window on Android, so a Tape over it is either drawn in every popup's root in register with the window's, or by one overlay above them all. The Tape's cost was measured on the phone (B-140, B-141), and this changes what it draws over; it is not closed until the phone measures it again, which waits for the owner's word to use the phone.

- AC:
  - with a menu, a tooltip and a dialog open, the Tape's band and noise are drawn over their pixels, in register with the Tape under them
  - the Tape runs while a dialog is open
  - the phone's frame cost of a page with a menu open is measured against B-141's, on the owner's word
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantTape.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantMenu.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTooltip.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDialog.kt`
