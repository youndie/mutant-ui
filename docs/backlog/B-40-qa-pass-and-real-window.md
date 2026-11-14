---
id: B-40
title: "A QA pass over the finished MVP and a real window driven by AWT events"
status: done
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-37]
---

# B-40 — A QA pass over the finished MVP and a real window driven by AWT events

Per-component proof holds what the design draws, not what a consumer does to it: in plakat-ui one adversarial pass over a finished kit found 40 defects, and a real window found one more.

- **The decision and its reason.** An agent told to break, not to confirm, over the MVP on the desktop test runtime: constraints, long content, computed values, font scale 2, keyboard focus, hidden input, overlays and focus return, the mouse. Findings numbered and triaged P0–P3 into items; the sweeps stay as tests. Then a throwaway driver posts AWT mouse, wheel and key events to the real window over the sample.

- AC: a report with numbered findings and repros; items for every finding the owner keeps; the sweeps in the test suite; hover, Tab order, the wheel and resize checked in a real window.
- Anchors: `docs/research/research-architecture.md`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/`

## Result (2026-11-14)

**The QA pass.** An agent told to break and not to fix ran over all 41 public subjects (every component and variant,
the illustration, the icon, the pattern and the Tape) on the desktop test runtime, about 3 900 composed cases. 21
findings; the real window added one more (F22). Every finding with a test is an ignored test asserting the correct
behaviour, named by its number, and each was run once without its `@Ignore` and failed with the defect it names. The
sweeps stay in `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/`: 39 tests, 21 passing, 18 ignored.

| F | P | What | Repro (test) | Item |
|---|---|---|---|---|
| F01 | P0 | The navigation bar throws in unbounded width | `ConstraintsSweepTest.F01` | [B-56](B-56-navigation-bar-unbounded-width.md) |
| F02 | P1 | The text button shows no keyboard focus | `KeyboardFocusSweepTest.F02` | [B-57](B-57-visible-keyboard-focus-everywhere.md) |
| F03 | P1 | A button in Success shows no keyboard focus | `KeyboardFocusSweepTest.F03` | [B-57](B-57-visible-keyboard-focus-everywhere.md) |
| F04 | P1 | The text field takes clicks only on its value's line | `OverlayAndMouseTest.F04` | [B-58](B-58-text-field-target-and-caret.md) |
| F05 | P1 | The block caret stands after the text, not at the cursor | `OverlayAndMouseTest.F05` | [B-58](B-58-text-field-target-and-caret.md) |
| F06 | P2 | A dialog with a long text lays its actions out at 0 × 0 | `LongContentSweepTest.F06` | [B-59](B-59-dialog-actions-always-reachable.md) |
| F07 | P2 | A long first action squeezes the primary to 0 width | `LongContentSweepTest.F07` | [B-59](B-59-dialog-actions-always-reachable.md) |
| F08 | P2 | Tabs past the row's width are 0 × 0 and still focusable | `LongContentSweepTest.F08` | [B-60](B-60-tabs-that-do-not-fit.md) |
| F09 | P2 | At font scale 3 every fixed-height component cuts its text | `FontScaleDensityTest.F09` | [B-61](B-61-text-under-font-scale.md) |
| F10 | P2 | The top app bar's title is cut at font scale 2 | `FontScaleDensityTest.F10` | [B-61](B-61-text-under-font-scale.md) |
| F11 | P2 | A focused button that turns to Loading drops focus | `KeyboardFocusSweepTest.F11` | [B-62](B-62-focus-through-loading.md) |
| F12 | P2 | The caret is drawn over right-to-left text | `OverlayAndMouseTest.F12` | [B-58](B-58-text-field-target-and-caret.md) |
| F13 | P2 | No hand cursor and no hover look | the real window, below; no `pointerHoverIcon` in the code | [B-63](B-63-hover-and-hand-cursor.md) |
| F14 | P3 | Weighted parts collapse in unbounded width | `OverlayAndMouseTest.F14` | [B-64](B-64-weighted-parts-in-unbounded-width.md) |
| F15 | P3 | Tabs and the navigation bar do not mirror in RTL | `OverlayAndMouseTest.F15` | [B-65](B-65-tabs-and-navigation-in-rtl.md) |
| F16 | P3 | The navigation pill overruns 30 dp cells | geometry (12 items in 360 dp) | [B-66](B-66-navigation-pill-in-narrow-cells.md) |
| F17 | P3 | Illustration labels scale out of the art | `FontScaleDensityTest.F17` | [B-61](B-61-text-under-font-scale.md) |
| F18 | P3 | The field's error has an empty description | code: `error("")` | [B-67](B-67-text-field-error-description.md) |
| F19 | P3 | The FAB swaps a consumer's icon on a name match | `ComputedValuesTest.F19` | [B-68](B-68-fab-icon-name-match.md) |
| F20 | P3 | A 3 000 dp body costs 64–78 ms a tick (suspected, capture included) | none kept | [B-69](B-69-tall-body-tick-cost.md) |
| F21 | P3 | A long snackbar action pushes the message out | `LongContentSweepTest.F21` | [B-70](B-70-snackbar-long-action.md) |
| F22 | P1 | Tab inside a dialog stops on the scrim and the sheet, both invisible, and stays on the last action | `DialogFocusCycleTest.F22` | [B-57](B-57-visible-keyboard-focus-everywhere.md) |

Clean, with tests that pass: every subject at widths 0–120 dp, height 0, a 10 dp box, unbounded either way, at
font scale 3 and density 3, but F01; long, empty, emoji and mixed-script text; tabs and navigation with 0, 1 and 12
items and any index; progress NaN, ±∞ and out of range; ink ×2 at density 2; Tab reaching all 18 controls, rings on
all but F02/F03, Enter and Space; nothing under an open dialog taking the wheel or a key; a closing dialog's and a
leaving snackbar's actions dead; a left click firing 14 controls, a right click and a press dragged off firing
none; the LTR caret clear of its text; the list item, snackbar and top app bar mirroring. Mutants, each killed by
the aimed test: the scrim's `clickable` removed (both dialog tests: a node under the scrim is not on the hit path,
whatever the scrim's `enabled`, so leaving it enabled while closing is equivalent), the leaving snackbar's action
left enabled, the chip's ring removed, NaN unguarded, the caret at offset 0. The dialog's focus trap removed is
killed by `DialogBehaviourTest`, not by the QA pass, whose keys never leave the dialog.

**The real window.** A throwaway driver, not committed, opened `MutantUiSample` in a `ComposeWindow` (JBR 25,
macOS 27.0.1, 1280 × 900) and posted `MouseEvent`, `MouseWheelEvent` and `KeyEvent` through the AWT event queue to
`SwingUtilities.getDeepestComponentAt`, photographing the Skia layer (`SkiaLayer.screenshot()`) after each step.

| Behaviour | What the window did |
|---|---|
| Hover and the cursor | Over `Play` the cursor stays the arrow and nothing changes (F13) |
| A mouse click and the ring | No ring after a click, on a button and on a chip; the chip selects |
| Tab order | The header's chips and switches, `Play`, the Success button (no ring, F03), `Record`, `Reset` (no ring, F02), the icon buttons past the disabled one, the FABs, the chips past the disabled one, and on |
| Tab past the fold | The page scrolls the focused control into view |
| The wheel | 30 px a notch, both ways, over the page |
| Resize | At 480 × 700 the sections narrow and nothing is cut; back at 1280 × 900 the page is as it was |
| A press under the dialog | Held on `Open the dialog` with no dialog, the button shows its press; with the dialog open, the press lands on the scrim, which dismisses, and the button under it does not open the dialog again |
| Escape | Closes the dialog, at once after it opens and after Tabs inside it |
| Tab inside the dialog | Two Tabs with nothing on screen, then `Cancel` with its ring: F22 |

Opening the dialog scrolls the page to bring the dialog's slot into view, and the button under the scrim keeps its
press colour while the dialog holds the screen's clock (D6, B-28): both as built. Not covered: the operating
system's own delivery of the mouse and the keys, a trackpad's phases and inertia, dragging the window's edge. Key
events posted while the window was not the active one were dropped by AWT's focus manager: in two runs Tab stopped
moving after the second press, and in a third every Tab inside the dialog was lost; neither repeated with the window
active, and the test runtime settled the dialog's Tab order.
