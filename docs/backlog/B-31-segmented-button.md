---
id: B-31
title: "SegmentedButton: the selection moves by venom"
status: wip
priority: P2
size: M
stage: stage-4-v1
epic: feature-components
blocked_by: [B-10, B-12]
---

# B-31 — SegmentedButton: the selection moves by venom

v1. Body `btn` (fill = surf2, `over` 14) and the selection window `pill` (`calm` 0.2, r 9). States: rest, selection (venom), disabled.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты v1.dc.html`
