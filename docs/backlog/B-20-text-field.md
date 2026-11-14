---
id: B-20
title: "TextField: 52 dp field, focus brackets, error corruption"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11]
---

# B-20 — TextField: 52 dp field, focus brackets, error corruption

MVP. Body `field`, height 52. States: rest, focused (`calm` 0.3 and the corner brackets), error (corrupt: red offset outline and blocks), disabled. The caret and the text are plain Compose; the body never moves the text. A slot that grows with its content rebuilds its instance (R1).

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content; the caret stays inside the field.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantTextField(value, onValueChange, …, prefix, placeholder, enabled, isError, keyboardOptions)`: the `field` body, 52 dp tall (the target), 200 dp wide given no width (D15); one line on `BasicTextField`, the OSD prefix in Handjet 20, the value in Rubik 15 and the placeholder in the secondary colour, 14 dp in and 8 apart (the page's row). Focused (any input mode — a field's focus is where typing goes): calm 0.3, the corner brackets, and the design's 9 × 20 block caret 8 dp after the text, blinking five scene ticks in eight (the engine's `fi % 8 < 5`), lit and still under reduce motion, clamped inside the field. The field's own cursor is hidden. Error: the corrupt body and the error in semantics. Disabled: no signal, both texts grey.
- Found by its test and fixed: typing threw — on the frame a value changes the caret asked the previous text layout about the new value's end (`fix(field)`).
- References (16): the TextField stage's four cells, whole and bare, both themes.
- Parity: bodies 0–2 px in «Эфир», «Копия»'s rest and error cells 20 and 29 (the speckle under the print slip, every 40 dp along a row), listed; prefix, value, placeholder and caret where the page puts them and in its colour.
- Mutants, each fails a named test: padding 12; gap 6; the prefix at 18; the prefix and the placeholder in the text colour; the blink four ticks in eight; the caret unclamped; no focus look; no error body; the height 48 (held by the sweep, not by parity, whose fixture gives the slot's 52); the caret 7 wide (first survived: a 2 dp narrower caret moved the text's extent by less than a pixel; the caret's 9 × 20 is now held itself).
- Tests: `TextFieldParityTest` (1), `TextFieldBehaviourTest` (5: typing and focus; the blink's shape, lit when paused; the caret inside the field; disabled and error; the sweep).
- Not covered: selection and its handles (Compose's, not drawn by the design); the caret inside the text when the cursor is not at its end (it stays the block, at the cursor).
