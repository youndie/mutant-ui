---
id: B-18
title: "Checkbox and RadioButton: 22 dp bodies, pour and melt"
status: wip
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
