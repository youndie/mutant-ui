---
id: B-29
title: "EmptyState: the six illustrations with a title"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-14]
---

# B-29 — EmptyState: the six illustrations with a title

MVP. Bodies `blob`, `bar`, `pill`, `dot` and `btn` composed into the six illustrations ([B-14](B-14-icons-illustrations-patterns.md)) with a title and an action.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantEmptyState(kind, title, modifier, text, action)`: one of the six illustrations (B-14's `MutantIllustration`, on its own stage, so a kind draws the same picture everywhere), 44 dp under it the title (Handjet 24, a heading) over the text (Rubik 14, secondary) 6 dp apart, all centred and wrapping centred in what it is given.
- Designed here (D15): the action the item names and the page does not draw — a slot 20 dp under the text, a `MutantButton` say.
- References (6): the Components page's empty state (the cassette, «Здесь пока пусто» and its two lines), whole and with its text, kept parts and reels hidden, both themes; and the `illus` stage's cell, whose record gives where the engine stood the cassette's bodies (the fixture draws the art on that stage: `IllustrationArt` takes the stage, its own in the kit).
- Parity: the bodies against the bare twin's 3 px («Эфир») and 70 («Копия», listed: the speckle under the print slip on its 40 dp grid, the cause §1.15 names — the illustration's own parity runs in «Эфир» only); the label and the reels within a pixel, as B-14 holds them; the title and the text's two lines where the page's ink lies and in its colour.
- Mutants (11), each failing a named test: the gaps 40, 4 and the action's 16; the sizes 22 and 13; the two colours swapped; the text left-aligned; the title left-aligned (first survived three times: on one line it cannot show; the wrapped title's ink runs its lines together; and the sweep it was read in had measured nothing — research row of B-29); no heading; the column not centred.
- Tests: `EmptyStateParityTest` (1), `EmptyStateBehaviourTest` (4).
