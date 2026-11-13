---
id: B-15
title: "Button: filled, outlined, text; pressed, loading, success, disabled"
status: open
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11]
---

# B-15 — Button: filled, outlined, text; pressed, loading, success, disabled

MVP. Body `btn`. Filled is the acid plate; outlined has fill = background, stroke = accent, a misregistered shadow and `over` 14; text has no body, its label sits over a tracking divider. States: rest, pressed (boil), loading (seek), success (clean), disabled (no signal). The action fires on release.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`
