---
id: B-15
title: "Button: filled, outlined, text; pressed, loading, success, disabled"
status: done
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

## Result (2026-11-14)

- `MutantButton(text, onClick, …, state = Idle | Loading | Success, icon)`, `MutantOutlinedButton`, `MutantTextButton`; the action fires on release; loading takes no click and reads as indeterminate progress; success shows the check unasked and holds (the handoff: no return after 600 ms); disabled loses the signal. 48 dp tall, at least 48 wide, the label one line cut with an ellipsis. Numbers chosen here (D15): 24 dp padding, the divider as wide as the label, keyboard focus as the field's.
- Fixed on the way, in the shared body: flow lines and speckle in the theme's ink whatever the outline's colour (the engine's); still bodies translated back from a fixture's origin like moving ones; `MutantSurface` takes a `stroke`.
- References (37): the Button stage's seven cells whole and bare in both themes — «Копия» by patching the page's theme in the render tab — and the text button in both. `make references` measures a render that is not byte-identical against the floor instead of failing it (research §1.15).
- Parity (research §1.15): bodies 0–3 px in «Эфир» and the still «Копия» cells; 15–18 px for «Копия»'s moving cells (speckle under the 1.5 dp slip) and 6/11 for the outlined rest cells (outline over shadow), listed and never to be raised; every label on the page's top row; the text button's divider 0 px and its label within a pixel (its line box centred at 11.5 dp).
- Mutants, each fails a named test: the outlined drip 24; no shadow; no accent stroke; the filled label in the text colour (first survived — the label was held by where its ink lies only — and is now also held by its colour where it covers a pixel whole); the text button's gap 8; the icon gap 4; the label at 14; no default check for success; a loading button that clicks; no minimum width on the text button; no keyboard focus; flow lines in the outline's colour. The label without `cssLine` survives as an equivalent mutant at 15 sp Rubik: Blink's baseline (14 + 0) and Compose's (14.025, drawn at 14) coincide at that size; B-52's probes hold `cssLine` where they do not.
- Tests: `ButtonParityTest` (2), `ButtonBehaviourTest` (8: release, loading, disabled ×3, success's check, the pause, the touch target, keyboard focus, the sweep — no width, a long label, font scale 2).
- Not covered: the body's frames are the generator's (B-03, B-10), not re-recorded here; Android and iOS; «Копия»'s keyboard focus colours (the field's); hover (D15, later).
