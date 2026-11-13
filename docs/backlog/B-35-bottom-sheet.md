---
id: B-35
title: "BottomSheet: a blob of r 22 with a divider above"
status: open
priority: P2
size: M
stage: stage-4-v1
epic: feature-components
blocked_by: [B-10, B-43]
---

# B-35 — BottomSheet: a blob of r 22 with a divider above

v1. Body `blob` (fill = surf, r 22, `over` 0, `wob` 1.6) and `btn`; a divider above the sheet. State: open. Sliding in and dragging are [B-43](B-43-undesigned-behaviour.md)'s.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты v1.dc.html`
