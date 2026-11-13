---
id: B-34
title: "NavigationDrawer: 42 dp items, slip 3, a shadow, the selection by venom"
status: open
priority: P2
size: M
stage: stage-4-v1
epic: feature-components
blocked_by: [B-10, B-12, B-43]
---

# B-34 — NavigationDrawer: 42 dp items, slip 3, a shadow, the selection by venom

v1. Body `bar` (fill = surf, r 0, `slip` 3, shadow), items 42 dp, the selection window `pill` r 8. States: open, selection (venom). Sliding in is [B-43](B-43-undesigned-behaviour.md)'s.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты v1.dc.html`
