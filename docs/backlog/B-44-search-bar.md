---
id: B-44
title: "SearchBar: a focused field with suggestion chips and result rows"
status: wip
priority: P3
size: M
stage: stage-6-later
epic: feature-components
blocked_by: [B-20, B-17]
---

# B-44 — SearchBar: a focused field with suggestion chips and result rows

Later (after publication, by the brief). Bodies `field`, `chip` and `icon`: a focused TextField, suggestion chips and result rows. State: focused.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты позже.dc.html`
