---
id: B-36
title: "PullToRefresh: the tongue, refreshing by boil, done by a clean pill"
status: open
priority: P2
size: M
stage: stage-4-v1
epic: feature-components
blocked_by: [B-10, B-11, B-43]
---

# B-36 — PullToRefresh: the tongue, refreshing by boil, done by a clean pill

v1. Bodies `btn` (the tongue: r 5–7, `over` 14–24, `cf` 0–1) and `pill` (done, clean). States: pulling, refreshing (boil), done (clean). The design shows three frames; the gesture is [B-43](B-43-undesigned-behaviour.md)'s and must work with a mouse drag.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты v1.dc.html`
