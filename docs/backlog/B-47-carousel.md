---
id: B-47
title: "Carousel: 220 × 120 cards, calm side cards, a pill pointer among dots"
status: open
priority: P3
size: M
stage: stage-6-later
epic: feature-components
blocked_by: [B-21, B-43]
---

# B-47 — Carousel: 220 × 120 cards, calm side cards, a pill pointer among dots

Later. Bodies `card` (220 × 120; side cards `calm` 0.5), `dot` (10) and `pill` (the pointer, 26 × 10). State: rest; not interactive in the design.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты позже.dc.html`
