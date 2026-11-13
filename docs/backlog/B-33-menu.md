---
id: B-33
title: "Menu: 36 dp items, the selection by venom"
status: open
priority: P2
size: M
stage: stage-4-v1
epic: feature-components
blocked_by: [B-10, B-12, B-43]
---

# B-33 — Menu: 36 dp items, the selection by venom

v1. Body `btn` (fill = surf, r 8, `over` 18), items 36 dp, the selection window `pill` r 6. States: open, selection (venom). Opening and closing are [B-43](B-43-undesigned-behaviour.md)'s.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты v1.dc.html`
