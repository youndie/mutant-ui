---
id: B-58
title: "Text field: the whole body takes the pointer, and the caret marks the insertion point"
status: open
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
