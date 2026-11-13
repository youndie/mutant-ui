---
id: B-30
title: "Slider: a 14 dp channel, an 8 dp fill and a 34 dp handle with the value inside"
status: open
priority: P2
size: M
stage: stage-4-v1
epic: feature-components
blocked_by: [B-10, B-11, B-43]
---

# B-30 — Slider: a 14 dp channel, an 8 dp fill and a 34 dp handle with the value inside

v1. Handle body `fab` (fill = OSD, r 17, `over` 12) with the value inside; channel 14 dp, fill 8 dp. States: rest, dragging (boil), disabled. Dragging works with a mouse as well as touch.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты v1.dc.html`
