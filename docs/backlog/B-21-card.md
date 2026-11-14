---
id: B-21
title: "Card: dead pixels in the corner"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11]
---

# B-21 — Card: dead pixels in the corner

MVP. Body `card` with dead pixels in one corner (`deadPx`, zone r = 62). States: rest, disabled. The clean zone covers its text; flow lines and dead pixels stay out of it.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantCard(modifier, enabled, onClick, key, interactionSource, content)`: the `card` body, at least 80 dp tall and 200 wide given no size (the width is D15's: the page sets only the height); the content is a column 16 dp from the sides and 14 from the top, its lines 4 apart (the page's `padding: 14px 16px; gap: 4px`), and its box is the clean zone. `MutantCardTitle` (Rubik 500 at 16, the text colour) and `MutantCardSubtitle` (Rubik 13, the secondary colour) are the design's two lines, one line each with an ellipsis; on a disabled card both grey. Given `onClick` the whole card is a button that fires on release, with the focus look under keyboard input; without it the card has no click semantics. Disabled: no signal, no click.
- References (8): the Card stage's two cells, whole and bare, both themes.
- Parity: bodies 0 px in «Эфир» and in «Копия»'s disabled cell; «Копия»'s rest cell 57 px — the speckle under the print slip, the same few phases recurring every 40 dp of a card 362 dp wide (the button's, research §1.15), listed in `CardParityTest`. Title and subtitle where the page puts them and in its colour.
- The body's frames at the engine's timestamps are the generator's, held by the surface's goldens (B-10, B-11); reduce motion is the surface's pause, held here too.
- Mutants, each fails a named test: padding 12 across and 12 down; gap 6; least height 72 and width 160; the title in the secondary colour and the subtitle in the text colour; a disabled line in its live colour; a disabled card clickable; no focus look; a click on a card given no `onClick`; the overflow visible; the body not disabled; the title at 15 and at weight 400 (parity). Survived at first and now held: the clean zone removed — the parity fixture gives the engine's own text zone, so only a behaviour test sees it (without it 124–545 of the 5054 pixels under the lines change with the seed; with it none do); the overflow made visible — a line's bounds stay inside the card either way, so the layout's overflow is read.
- Tests: `CardParityTest` (1), `CardBehaviourTest` (8: the layout given no size; no click without `onClick`, on release with it; disabled; the colours; the pause; keyboard focus; the sweep; the seed kept out of the lines).
- Not covered: a press look — the page draws none for the card, and none is designed here.
