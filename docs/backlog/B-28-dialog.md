---
id: B-28
title: "Dialog: a clean window over a paused background"
status: open
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11]
---

# B-28 — Dialog: a clean window over a paused background

MVP. Bodies `bar` (the window, clean, r 12) and `btn` (actions). The background pauses with a band. Focus moves into the dialog and returns on dismissal.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content; focus returns to the opener on dismissal.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`
