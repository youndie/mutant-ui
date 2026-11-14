---
id: B-19
title: "Switch: track and thumb, pour and slide"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11, B-12]
---

# B-19 — Switch: track and thumb, pour and slide

MVP. Bodies `track` 52 × 28 and `thumb` 22; off: fill = surf2, `over` 6. States: off, on (pour and slide with a 40 % stretch), disabled. The thumb's echo is drawn separately (the `ghost` gene is not implemented).

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantSwitch(checked, onCheckedChange, …, label, enabled)`: the `track` body (52 × 28) with the `thumb` body (22) nested in it, in the 48 dp control row (role switch, the row the target). Off, the track is the second surface colour with a 6 dp drip and the thumb 3 dp in; on, the acid fill pours in, the thumb slides 24 dp right with B-12's stretch, and the echo — a 2 dp ring of the ink at 35 % where the thumb stood — melts in. The echo is page content, drawn over every body, the thumb included (drawn under it, it read 82 px off). Disabled: both bodies lose their signal; the page's disabled switch is on.
- References (12): the select stage's switch cells, whole and bare, both themes.
- The shared fixture now stands the bodies nested in a cell's slot where the engine did (the thumb; several cells share its key, so only slots inside the cell's box are taken), passes the engine's no-text sentinel (`[-99, -99, 0, 0]`) as no clean zone — passed through, it put the disabled thumb's no-signal band elsewhere — and skips the row above a component placed by its target (the on switch's drips hang into the disabled cell's crop).
- Parity: bodies 0–2 px, thumb and echo 0–5; labels where the page puts them and in its colour.
- Mutants, each fails a named test on the first run: the off drip 12; no off colour; the thumb at 25; the echo at 21 and at 60 %; no slide (travel 0, or no request); the echo not melting; no disabled body.
- Tests: `SwitchParityTest` (1), `SwitchBehaviourTest` (4: a click on the row switches it, with semantics; the thumb slides right over time, at once when paused; the 48 dp row, disabled takes no click; the sweep).
