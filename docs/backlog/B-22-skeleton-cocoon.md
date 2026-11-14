---
id: B-22
title: "Skeleton («Кокон»): mosaic of 16 and 8 dp in the shape of the coming component"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-07]
---

# B-22 — Skeleton («Кокон»): mosaic of 16 and 8 dp in the shape of the coming component

MVP. No body: a mosaic of 16 and 8 dp blocks in the outline of the component it stands for, changing about 6 % of blocks per tick (the tokens' flash budget). State: loading.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantCocoon(modifier, key)`: no body and no size of its own — it takes the shape it is given. The engine's `buildStage` and `stageStep` ported 1:1: 16 dp blocks (94 in 100 drawn) inside an edge of 8 dp quarters (4 in 5) within 16 dp of the left or top and 32 of the right or bottom, laid out from `rng(<cassette>/<stage>/coc<key>, 'c')` in `coc1, surf, surf2, coc2, surf, coc1`, clipped to a 10 dp corner (half the height of a thinner one). On each scene tick (125 ms) `ceil(max(2, 6 % of the blocks))` of them are repainted from `rng('coc', <stage><key>/<tick>)` in the four paints; the paints build up tick on tick, a tick is applied once, and a new size lays the blocks out again without re-applying the tick it stands at — all as the engine does (research row of B-22). `key` is the engine's index on its stage: several cocoons on one screen each take their own.
- Designed here (D15): under «Чистый сигнал» it stands, like the other idle glitches; it reads as indeterminate progress.
- References (4): the Card stage's two cocoons, the card's 362 × 80 and the list row's three (40 × 40 and lines of 217 and 140 × 16), both themes.
- Parity: 0 px, the largest channel difference 0, in all four — rects on whole offsets, at the stage's 0.1875 px down the page, rasterise alike.
- Mutants, each fails a named test: the block 15 and the quarter 7; the shares .7 and .9; the layout's paints reordered; three tick paints; 3 % a tick; one least repaint; the corner 8; the tick never applied; the tick's stream renamed; the paint drawn before the block; the edge at 16 from the right; a rebuild re-applying its tick; flicker under «Чистый сигнал»; no progress semantics; one seed for every key. Equivalent, not caught: the corner not limited to half the height — Skia, like SVG, clamps a radius to half the side itself.
- Tests: `CocoonParityTest` (1), `CocoonBehaviourTest` (7: the layout's geometry; 6 % a tick, two at least, building up; a rebuild; keys; the flicker, paused and under «Чистый сигнал»; the semantics; no size, zero sizes and the corner).
- Not covered: the touch target and font scale — the cocoon takes no input and holds no text.
