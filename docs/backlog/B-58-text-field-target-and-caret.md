---
id: B-58
title: "Text field: the whole body takes the pointer, and the caret marks the insertion point"
status: done
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-40]
---

# B-58 — Text field: the whole body takes the pointer, and the caret marks the insertion point

Found in [B-40](B-40-qa-pass-and-real-window.md).

| Finding | Priority | What happens |
|---|---|---|
| F04 | P1 | Only the value's line (≈18 dp of the 52 dp body, right of the prefix) takes a click; 19 of 20 sampled points on the body do nothing |
| F05 | P1 | The block caret is always drawn after the end of the text; after Tab the cursor is at 0, so typing `Z` gives `Zabc` while the block stands after `abc`. `cursorBrush` is transparent, so there is no other caret |
| F12 | P2 | The caret is placed at `getHorizontalPosition(length) + 8 dp` as if the text ran left to right: over Arabic in an LTR field, over the text's end in an RTL layout |

- **The decision.** A press anywhere on the body focuses the field and places the cursor at the nearest offset (the end when it lands past the text). The block caret stands at the cursor's rectangle (`getCursorRect(selection.start)`), on the side the text runs. The rejected alternative is the platform's thin caret: the design draws a block.
- AC: `OverlayAndMouseTest.F04`, `.F05` and `.F12` pass without their `@Ignore`; the LTR control case stays green; a killed mutant for each.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTextField.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/OverlayAndMouseTest.kt`

## Result (2026-11-14)

- **F04.** A press the text itself does not take — the prefix, the padding, above and below the line — focuses the
  field and puts the cursor at the nearest place on the line. The cursor is the field's own `TextFieldValue`, held
  inside; a value changed from outside puts it at the end, and focus by Tab finds it there.
- **F05.** At the end of the text the caret is the page's block, 9 × 20, 8 dp past the end, as before (parity
  unchanged). Inside the text a block would cover the next letter, so the caret is a bar 2 × 20 dp standing just
  before the insertion point, against the letter already typed, and it may stand in the padding or the gap before
  the text. Designed here (D15): the design draws only the end.
- **F12.** The line takes the text's direction (`TextDirection.Content`): Arabic lines up from the right in a
  left-to-right field. The block stands past the end on the side the text runs, the bar on the side of the letter
  before.

The QA pass's F05 test assumed Tab puts the cursor at the start; it now holds what the finding asked: the caret marks
where the next letter goes, at the end after Tab (`abcZ`) and at the start after Home (`YabcZ`), and the bar covers no
letter. `OverlayAndMouseTest.F04`, `.F05` and `.F12` run un-ignored. Mutants, each killed by the aimed test: the
press not focusing, the bar over the next letter, the end block on the wrong side in right-to-left, the line without
the text's direction, the cursor starting at 0, the bar held inside the text box.
