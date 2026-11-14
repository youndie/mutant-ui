---
id: B-22
title: "Skeleton («Кокон»): mosaic of 16 and 8 dp in the shape of the coming component"
status: wip
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-07]
---

# B-22 — Skeleton («Кокон»): mosaic of 16 and 8 dp in the shape of the coming component

MVP. No body: a mosaic of 16 and 8 dp blocks in the outline of the component it stands for, changing about 6 % of blocks per tick (the tokens' flash budget). State: loading.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`
