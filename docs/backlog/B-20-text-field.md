---
id: B-20
title: "TextField: 52 dp field, focus brackets, error corruption"
status: open
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11]
---

# B-20 — TextField: 52 dp field, focus brackets, error corruption

MVP. Body `field`, height 52. States: rest, focused (`calm` 0.3 and the corner brackets), error (corrupt: red offset outline and blocks), disabled. The caret and the text are plain Compose; the body never moves the text. A slot that grows with its content rebuilds its instance (R1).

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content; the caret stays inside the field.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`
