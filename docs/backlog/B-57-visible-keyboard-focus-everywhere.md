---
id: B-57
title: "Keyboard focus: every Tab stop visible, and the dialog's Tab cycle"
status: open
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-40]
---

# B-57 — Keyboard focus: every Tab stop visible, and the dialog's Tab cycle

Found in [B-40](B-40-qa-pass-and-real-window.md). Tab moves blind in three places:

| Finding | Where | What happens |
|---|---|---|
| F02 | `MutantTextButton` | Focused by Tab, 0 pixels change: `TextButtonBody` never reads focus |
| F03 | `MutantButton` in `Success` | Still clickable and focusable, but the clean-body branch skips the brackets |
| F22 | `MutantDialog` | Tab stops on the scrim and on the sheet, both invisible (the sheet's swallowing `clickable` and the scrim's are focusable), then Cancel, then Erase, and stays on Erase: `onExit` cancels the wrap. Seen first in the real window, two Tabs with nothing on screen |

- **The decision.** The text button draws the brackets the other controls draw, around its label; the clean body draws them too. The scrim and the sheet are not focus stops (`focusProperties { canFocus = false }`); Tab past the last action wraps to the first, Shift+Tab past the first to the last. The rejected alternative is making a Success button unfocusable: it stays clickable, so a keyboard user must reach it.
- AC: `KeyboardFocusSweepTest.F02`, `.F03` and `DialogFocusCycleTest.F22` pass without their `@Ignore`; a killed mutant for each.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantButton.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDialog.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/KeyboardFocusSweepTest.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/DialogFocusCycleTest.kt`
