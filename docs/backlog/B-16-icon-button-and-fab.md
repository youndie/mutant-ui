---
id: B-16
title: "IconButton and FAB: 48 and 56 dp bodies, the extended FAB"
status: open
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
