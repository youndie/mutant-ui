---
id: B-59
title: "Dialog: its actions stay reachable under long text and long labels"
status: done
priority: P2
size: M
stage: stage-5-product
blocked_by: [B-40]
---

# B-59 — Dialog: its actions stay reachable under long text and long labels

Found in [B-40](B-40-qa-pass-and-real-window.md).

| Finding | What happens |
|---|---|
| F06 | Title and text taller than the screen: the actions' row gets the remaining height, 0, and both actions are 0 × 0, unreachable by mouse and keys. Same in a 120 × 120 dp window |
| F07 | A long first action label takes the row, and the primary action after it gets 0 width |

- **The decision.** The text scrolls inside the sheet; it is measured after the title and the actions, which keep their size. In the actions' row each label ellipsizes within a share of the row, so the primary action always keeps its label's minimum. The rejected alternative is capping the text's length in the API.
- AC: `LongContentSweepTest.F06` and `.F07` pass without their `@Ignore`; the dialog's parity cells and goldens unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDialog.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/LongContentSweepTest.kt`

## Result (2026-11-14)

- **F06.** The text takes `weight(1f, fill = false)` in the window's column and a vertical scroll: the title and the actions are measured first, at their size, and a text taller than the room left scrolls inside the window. The decision first said to clip only when the text overflows, for the glyph edges a clip moves (plakat-ui B-62); here the fitting dialog's capture was byte-identical with and without the scroll container, so the scroll is unconditional and the measure that would have switched it is gone.
- **F07.** Each action takes `weight(1f, fill = false)` in the actions' row, through the row the dialog provides to its actions: at most an even share, its own width under it, its label ellipsized in the share. The short pair the design draws stands where it stood.

`LongContentSweepTest.F06` (both actions inside a 600 dp screen at their size under 1 200 characters of text, and the text scrolling) and `.F07` (the primary action at least 48 dp beside a 37-character first label, and nothing to scroll for a text that fits) run un-ignored. The dialog's parity, behaviour and focus tests are unchanged and green. Mutants, each killed by the aimed test: the text without its weight, the text without its scroll, the actions without their share.
