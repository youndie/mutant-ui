---
id: B-24
title: "NavigationBar: bar body, the active item a 56 × 30 pill"
status: wip
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11]
---

# B-24 — NavigationBar: bar body, the active item a 56 × 30 pill

MVP. Bodies `bar` (height 62–68) and `pill` for the active item, 56 × 30. The label is always visible.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`
