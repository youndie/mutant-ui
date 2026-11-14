---
id: B-18
title: "Checkbox and RadioButton: 22 dp bodies, pour and melt"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11, B-12]
---

# B-18 — Checkbox and RadioButton: 22 dp bodies, pour and melt

MVP. Checkbox: body `box`, 22 × 22, checked fill = accent by pour, the mark melts in and out. RadioButton: body `dot`, 22 × 22, the dot melts. States: unchecked/unselected, checked/selected, disabled. The touch target is the 48 dp row, not the 22 dp body.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantCheckbox(checked, onCheckedChange, …, label, enabled)` on the `box` body and `MutantRadioButton(selected, onClick, …, label, enabled)` on the `dot` body, 22 × 22, each in a 48 dp row with its label 12 dp after it — the whole row is the touch target (toggleable / selectable, roles checkbox and radio). Checked, the acid fill pours in and the mark — the tick at a 3 dp line and two burnt blocks — melts in (B-12's melt), each piece on its own; selected, the recording dot melts in with its echo up and to the right at 30 %. The red is the page's `#ff3b30` in both themes, never flashing. Disabled: the box keeps its tick as it stands, without the blocks; the radio shows a grey dot; labels grey.
- References (24): the select stage's checkbox and radio cells, whole and bare, both themes, reaching the label beside the slot. The bare twin hides what the page draws in `currentColor` or marks `data-keep` (the tick, the dot) and keeps what it paints as a background (the blocks, the echo); the kit's twin does the same.
- The shared fixture now places each cell within a pixel where its bare twin matches best (the select stage's layer lands a pixel below its slots), and holds a label beside the slot as text.
- Parity: bodies and marks 0 px (`Копия`'s unchecked box 4); labels where the page puts them and in its colour.
- Mutants, each fails a named test: the «Копия» danger for the record red; the gap 8; the body 24; no acid fill; the label at 14; the disabled dot in the text colour; the tick's line at 2, the echo at 50 % and a block a pixel over (these three first survived: the marks inside the clean zone were not yet held pixel by pixel, and are now); the tick not melting (first survived a test that the pour itself satisfied; the test now counts the tick's own dark pixels).
- Tests: `CheckboxParityTest` (1), `CheckboxBehaviourTest` (4: a click on the row toggles and selects, with semantics; the melt over 330 ms, at once when paused; the 48 dp row, disabled takes no click; the sweep).
