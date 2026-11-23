---
id: B-186
title: "The caret restarts, sits beside the letters, and the mosaic ignores the text"
status: open
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-186 — The caret restarts, sits beside the letters, and the mosaic ignores the text

The caret blinks on the screen's tick and does not restart on focus or typing (dark up to 375 ms after a click, F-text-field-04); its 2 dp bar hides a letter's right stem (F-text-field-05); the mosaic moves when the first letter is typed or the last deleted (F-text-field-07). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 8, 17 and 18 (all [ошибка]).

- **The decision and its reason.** The blink restarts on its visible phase at focus and at every input; the cycle is 5 ticks on, 3 off. At the end of the line the caret is a 9 × 20 dp block 2 dp after the last character; inside the text it is a 2 dp line centred in the gap between letters. The field's clean zone is its input area (the slot less 14 dp each side), never the text, so the mosaic does not depend on it.
- The search bar shares the field and takes the same.

- AC:
  - the caret is visible on the frame after focus and after each keystroke, then 5 ticks on and 3 off
  - at the end of the line: a 9 × 20 dp block 2 dp after the last glyph's ink; inside: a 2 dp line whose centre is the gap's centre, no glyph pixel covered
  - the mosaic's pixels are identical for an empty field, one letter and a full line
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTextField.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSearchBar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/CleanZone.kt`
