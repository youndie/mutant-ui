---
id: B-15
title: "Button: filled, outlined, text; pressed, loading, success, disabled"
status: wip
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

## Iteration 1 (2026-11-14)

- Built: `MutantButton` (idle, loading, success), `MutantOutlinedButton`, `MutantTextButton` over a tracking divider (`drawTrackingDivider`); `MutantSurface` takes a `stroke`; speckle and flow lines are the theme's ink whatever the outline's colour, as the engine draws them (the outlined button read 227 px off before); the still bodies are translated back from a fixture's origin like the moving ones.
- References: the Button stage whole and bare (text hidden), both themes — «Копия» by patching the page's `data-props` in the render tab — and the text button.
- Parity so far (before the bare twins): bodies 0–5 px in «Эфир»; «Копия»'s moving bodies 9–18 px, the speckle under the print slip; labels on Chrome's top row.
- Stopped: the machine's load average reached 22–38 (another session's job), and two renders of the pages no longer agreed — six or seven cells, a different set each run, older references included. The references are to be rendered again on a quiet machine, then the label check moved onto the bare twins.
