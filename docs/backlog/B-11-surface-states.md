---
id: B-11
title: "Body states: boil-over, loading, success, disabled, error, focus"
status: open
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
