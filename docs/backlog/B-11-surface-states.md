---
id: B-11
title: "Body states: boil-over, loading, success, disabled, error, focus"
status: done
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-10, B-04]
---

# B-11 — Body states: boil-over, loading, success, disabled, error, focus

States change the body, not the content. The handoff defines each: boil-over on press (heat 0→1 in 320 ms, back in 480 ms, tick ×1.5, boil' = boil·(1+2·heat)+0.6·heat, fill to amber by min(1, 1.15·heat), bubbles, no drops, running mosaic), loading (two bands bottom to top, 900 ms), success (a clean rounded rectangle with one highlight), disabled «нет сигнала» (no slime: desaturated fill, scanlines 1 of 2 dp, still snow, one shifted band), error (red outline offset 2.5 dp, corruption blocks), focus (`calm` 0.3 and four amber corner brackets).

- **The decision and its reason.** One state parameter on `MutantSurface`, each state a branch of the frame function read from `drawItem`; heat is an animated value of the press.
- The red never flashes (the tokens' flash rule): the error is static.

- AC: every state matches its reference within the floor in both themes; heat read at 0, 160, 320 and 800 ms equals the engine's; a press triggers no layout; a mutant in each formula is caught.
- Anchors: `reference/design-system/mutant-engine.js`, `reference/design-system/mutant-ui Компоненты.dc.html`, `reference/design-system/mutant-tokens.json`

## Result (2026-11-14)

- `BodyStates.kt`: `boilFill` (`mix` towards amber, lifted 0.2 on odd 333 ms periods past half heat), `runningMosaic` (the `px<round(t / 83.33)>` stream: colour, opacity, shift along the side), `loadingBands` (900 ms, the second band 0.45 behind), `BodyDecor` (error outline at the profile's width offset 2.5 dp, `errBlocks` in danger, ink and background, the seek bands, the brackets), `drawDisabledBody` (`staticHtml`: desaturated fill with the theme's tint and darkening, scanlines — lines in «Эфир», dots in «Копия» — the stage's snow, dropouts outside the clean zone, grain at the bottom, a band of 14 % shifted 4–7 dp), `drawCleanBody`. `drawBody` takes the decor in `itemHtml`'s order.
- `MutantSurface`: `state`, `focused` (wear 0.3, brackets), `interactionSource` (a press boils the body by `HeatMachine`, 320 ms up and 480 down from the tokens, on the screen's clock, redrawn on the hot tick while hot), `heat` to fix it; disabled and success build one still frame and read no tick. A frozen clock draws at its exact time even when hot, as the engine's `tFix` does.
- References: eight state cells of the Components page, text transparent and the field's block caret hidden (it paints a box of its own; `visibility: hidden` keeps the zone the engine measured). `BodyParityTest`: pressed 0.004 %, loading 0.011 %, success 0.000 %, disabled 0.000 %, focused 0.000 %, error 0.005 %, field and card disabled 0.000 %, against the frozen 0.02 %; the pressed button and the disabled field looked at.
- `SurfaceStatesTest` (5): the heat machine at known points; a press turns the body amber within 480 ms and a release cools it within 960 ms; under reduce motion a press changes nothing; focus gives a wear of 0.3 and the brackets in the focus colour; a disabled body builds one frame over 200.
- Mutants, each fails a named test: the heat capped at 0.8; the flicker's parity flipped; the bands' 0.45 → 0.5; the disabled band 14 % → 12 %; the focus wear 0.3 → 0.4; the error outline's offset 2.5 → 2; the running mosaic's shift removed. The shift's threshold 0.5 → 0.25 survives: the tiles it moves on the pressed reference do not show.
- Not covered: the transitions between states (pour, melt, slide, venom: [B-12](B-12-transitions.md)); «Копия» page cells.
