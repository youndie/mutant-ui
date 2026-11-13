---
id: B-21
title: "Card: dead pixels in the corner"
status: open
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11]
---

# B-21 — Card: dead pixels in the corner

MVP. Body `card` with dead pixels in one corner (`deadPx`, zone r = 62). States: rest, disabled. The clean zone covers its text; flow lines and dead pixels stay out of it.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`
