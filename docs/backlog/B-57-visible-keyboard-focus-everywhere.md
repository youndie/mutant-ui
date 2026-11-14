---
id: B-57
title: "Keyboard focus: every Tab stop visible, and the dialog's Tab cycle"
status: done
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

## Result (2026-11-14)

| Finding | Fix | Held by |
|---|---|---|
| F02 | `TextButtonBody` draws the corner brackets around its 48 dp target under keyboard focus, as a dialog's action does: it has no body to draw them on | `KeyboardFocusSweepTest.F02`, un-ignored |
| F03 | `MutantSurface`'s clean frame (Success) draws the brackets when focused | `KeyboardFocusSweepTest.F03`, un-ignored |
| F22 | The scrim and the sheet are no focus targets (`canFocus = false`); Tab past the last action is redirected to the first in the dialog's `onExit`; Shift+Tab on the first action goes to the last, intercepted before it reaches the dialog's own focus target | `DialogFocusCycleTest.F22`, un-ignored: from open, Tab visits Cancel, Erase, Cancel, …; Shift+Tab comes round the other way |

One choice was tried and turned back: opening the dialog with focus on its first action, which removes the dialog's
own stop. Desktop starts in keyboard input mode, so Cancel showed its brackets the moment a mouse opened the dialog,
and the open dialog's parity cells failed on them. Focus opens on the dialog itself as before, which draws nothing;
it is a stop only before the first Tab. Actions register their focus requesters with their dialog in composition
order; the dialog keeps which one holds focus.

Mutants, each killed by the aimed test: either `canFocus = false` removed, the Next redirect removed, the Shift+Tab
interception removed, the text button's brackets removed, the Success brackets removed. The dialog's parity,
behaviour and overlay tests stay green; no golden moved.
