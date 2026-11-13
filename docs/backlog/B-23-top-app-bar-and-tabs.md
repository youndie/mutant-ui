---
id: B-23
title: "TopAppBar and Tabs: the 56 dp bar with a tracking divider, the 4 dp indicator with an echo"
status: open
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-07]
---

# B-23 — TopAppBar and Tabs: the 56 dp bar with a tracking divider, the 4 dp indicator with an echo

MVP. TopAppBar: no body, height 56, a tracking divider below. Tabs: no body, an indicator 4 dp tall with an echo.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`
