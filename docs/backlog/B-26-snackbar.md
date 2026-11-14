---
id: B-26
title: "Snackbar: a blue bar with a shadow"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11]
---

# B-26 — Snackbar: a blue bar with a shadow

MVP. Body `bar`, fill = blue, misregistered shadow, r 6, height 48. Appearance and dismissal are not drawn beyond a still (R3, [B-43](B-43-undesigned-behaviour.md)).

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantSnackbar(message, modifier, visible, actionLabel, onAction, onHidden, key)`: the `bar` body in the no-signal blue with the deep misregistered shadow, the page's `data-r="6"` and `data-marks="0"`, 48 dp tall across what it is given; the message (Rubik 14, on-blue) 16 dp from its start, cut with an ellipsis before the action; the action (Handjet 20, tracked 1, the on-blue accent) with its word 16 dp from the end, a target the bar's height and at least 48 wide, fired on release. A polite live region. Its clean zone is the engine's.
- Designed here (D15), after plakat-ui's: `osd-in` rides it up from its height and 8 dp below in 240 ms, `osd-out` back down in 200, both stepped at 12 fps and eased out by the cube (the brief gives the durations and nothing of the path); from the moment it starts to leave its action does not answer; once gone it is not composed and `onHidden` is called once — never for a snackbar composed hidden; under reduce motion both are at once (the kit's D7: toggles land at once, where the brief's `fade 100` would need a clock that runs); keyboard focus draws the corner brackets on the action.
- Found and fixed by its test: it asked for frames while the time was under the ride's duration, but the stepped ride lands a step later (250 ms for 200), so it stood at 83 % and never left. The melt has the same pattern: [B-54](B-54-melt-stalls-short-of-its-end.md).
- References (4): the list stage's snackbar, whole and bare, both themes.
- Parity: the body 0 px («Эфир») and 1 («Копия»); the message and the action each where the page's ink lies and in its colour. Listed: the message's top row a pixel high in both themes — the ascender of «б» on the light-on-dark threshold, the message being on-blue in each (B-24's cause); its bottom row is the page's.
- Mutants (26), each failing a named test: the height 44, the corner 8, the sides 12, the sizes and tracking, the fill and the shadow, flow lines on, the two colours, the ride's durations, its 8 dp, its curve, no pause, the action answering while it leaves, no live region, no focus look, the message clipped, staying composed when gone, reporting a leaving never begun, each clean-zone mark, the action's end, its least width (first survived: «ОТМЕНИТЬ» is wide enough without it; now held by a one-letter action).
- Tests: `SnackbarParityTest` (1), `SnackbarBehaviourTest` (8).
- Not covered: a host that queues snackbars and times them out — a screen's, not the component's; and the trace `osd-out` leaves («оставив след»), not drawn.
