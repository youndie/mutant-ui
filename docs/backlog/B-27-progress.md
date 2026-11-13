---
id: B-27
title: "LinearProgress and CircularProgress"
status: open
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-07, B-08]
---

# B-27 — LinearProgress and CircularProgress

MVP. Linear: no body, segments 6 + 2 dp, determinate and indeterminate (a running band). Circular: a 36 dp reel stepping 30° per tick, indeterminate.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`
