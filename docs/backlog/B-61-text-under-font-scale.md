---
id: B-61
title: "Font scale: fixed heights cut text, and illustration labels leave the art"
status: done
priority: P2
size: M
stage: stage-5-product
blocked_by: [B-40]
---

# B-61 — Font scale: fixed heights cut text, and illustration labels leave the art

Found in [B-40](B-40-qa-pass-and-real-window.md).

| Finding | Priority | What happens |
|---|---|---|
| F09 | P2 | At font scale 3 every fixed-height component cuts its text top and bottom: buttons and the text button, the extended FAB, chips, checkbox/radio/switch labels, the field, tabs, the snackbar, navigation labels (a button label 54 dp in 48; a chip 45 in 32) |
| F10 | P2 | The top app bar's 26 sp title is cut already at font scale 2 (58 dp in 56) |
| F17 | P3 | Illustration labels are in sp over art fixed in dp: at font scale 2 a label spans −17…254 dp over a 236 dp illustration |

- **The decision.** Heights become minimum heights, so a body grows with its text and the generator draws the grown slot; the touch target is unchanged or larger. An illustration's words are part of the picture and are drawn in dp, at the art's scale. The rejected alternative is shrinking text to fit, which undoes the user's font scale.
- AC: `FontScaleDensityTest.F09`, `.F10` and `.F17` pass without their `@Ignore`; parity cells at font scale 1 unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantButton.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTopAppBar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/illustration/MutantIllustration.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/FontScaleDensityTest.kt`

## Findings (2026-11-15)

**Fixed.** Heights became minimums in:
- the button, outlined and text buttons;
- the chip (target and body);
- the checkbox, radio and switch row;
- the text field;
- the top app bar's row;
- the tabs;
- the navigation bar;
- the snackbar.

Each grows with its text. An illustration's words are drawn at font scale 1, in dp at the art's scale. At font scale 1 nothing moves: every parity test is unchanged.

What each needed beyond `height` → `heightIn(min)`:
- **Tabs.** Each tab keeps its 11 dp under the label's row, so the bar stays 6 dp under a label grown with the font. The bar stands 5 dp over the row's foot rather than at a fixed 43.
- **Navigation bar.** The bar is as tall as its tallest item (by intrinsic height), at least 68. The `BoxWithConstraints` inside it filled its height, which with a minimum height was the whole window: a second bar under it measured 0.
- **Snackbar.** The row is as tall as its content (`IntrinsicSize.Min`), at least 48, so the action's full-height target fills the row and not the window.
- **Text field.** A single-line `BasicTextField` holds its text to a line it measures itself, which can be a pixel short of the text. Measured over font scales 1–3, the value never fits that line on cssLine's rounded line, and always does on the face's own. So above font scale 1 the value takes the face's own line; at font scale 1 cssLine stays, for parity. An empty field's box can be a pixel under its empty line, which cuts nothing.

**The checks.** `FontScaleDensityTest.F09`, `.F10` and `.F17` run, their `@Ignore`s gone. Their cut is now a box shorter than its non-empty text. Compose's `didOverflowHeight` also counts a line ended by an ellipsis, which is the kit's rule for a word wider than its room: the top app bar's title and the snackbar's message beside a long action at font scale 3. `BarTabsBehaviourTest`'s bar at font scale 2 now expects the row grown to the title's 58 dp (84 with its 24 dp inset), as this item decided.

**Mutants.** Killed:
- the bar at a fixed 43, by a new test: at font scale 3 the tabs grow and the bar stands under the label;
- the navigation bar filling its height;
- the snackbar's row without its intrinsic height;
- the illustration's words at the user's scale;
- the field on cssLine above font scale 1.

All but the first were seen failing on the way.
