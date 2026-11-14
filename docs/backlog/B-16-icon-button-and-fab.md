---
id: B-16
title: "IconButton and FAB: 48 and 56 dp bodies, the extended FAB"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11]
---

# B-16 — IconButton and FAB: 48 and 56 dp bodies, the extended FAB

MVP. IconButton: body `icon`, 48 × 48, rest, pressed, disabled. FAB: body `fab`, 56 × 56, extended 136 × 56, drips on every drip (`cf` 1), rest and pressed; the brief's most damaged component.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantIconButton(icon, contentDescription, onClick, …, enabled)` on the `icon` body, 48 × 48; `MutantFab` on the `fab` body, 56 × 56; `MutantExtendedFab(text, icon, …)`, 56 tall, as wide as its content with 24 dp padding (D15: the page draws a fixed 136). The icon is the text's colour at rest, the ink while pressed (the body boils to amber; the page's pressed cell), grey when disabled; the FAB's icon is the ink, drawn at the page's 2.5 dp line when it is one of the kit's (`MutantIcons.stroked`). The design draws no disabled FAB, and the kit offers none.
- References (24): the IconButton · FAB stage's six cells, whole and bare, both themes. A re-render now keeps a committed reference that the fresh one is within the floor of (`make references`), so Chrome's alternating renders stop moving references under other items' tests.
- Parity, on a shared fixture (`ComponentParity`): bodies 0–4 px in «Эфир», «Копия» up to 17 (the speckle under the print slip; the disabled icon button's dot pattern clipped at its rounded corners), listed; every icon and label where the page puts it and in its colour; icon-only content within the floor pixel by pixel (0–3).
- Tests: `IconButtonParityTest` (1), `IconButtonBehaviourTest` (5: release, the disabled one never; the icon darkens while held; 48 / 56 / 56; the pause; the extended FAB's sweep).
- Mutants, each fails a named test: the FAB's line at 2 (first survived where only the icon's place and colour were held; icon content is now held pixel by pixel); no darkening on press; a disabled icon in the text colour; the extended gap 4; sizes 44 and 52; the `btn` body for either; no disabled body.
