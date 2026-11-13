---
id: B-46
title: "DataTable: OSD header, 48 dp rows, tracking dividers, row selection"
status: open
priority: P3
size: M
stage: stage-6-later
epic: feature-components
blocked_by: [B-18, B-25]
---

# B-46 — DataTable: OSD header, 48 dp rows, tracking dividers, row selection

Later. Body `box` for selection; header OSD 36, rows 48, tracking dividers. States: rest, row selected.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты позже.dc.html`
