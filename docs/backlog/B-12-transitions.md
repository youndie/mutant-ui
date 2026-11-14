---
id: B-12
title: "Transitions: pour, melt, slide, and the venom with its soft joins"
status: done
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-04, B-11]
---

# B-12 — Transitions: pour, melt, slide, and the venom with its soft joins

A colour change pours down (`clamp(260 + 5·h, 300, 620)` ms at 12 fps); an indicator melts in or out (330 ms); a switch thumb slides 330 ms with a 40 % stretch; a selection moves by venom (720 ms at 24 fps) whose fat tentacle and masses are softened by a blur of σ 3.2 dp and an alpha threshold `a' = 18·a − 8.2` (H4).

- **The decision and its reason.** Each transition is a closed form of elapsed time on the kit clock, its geometry from [B-04](B-04-transition-geometry.md). The soft joins are a layer with a blur `RenderEffect` and a colour-matrix filter where the platform has them; on Android below 12 the venom draws its geometric shapes as they are, which the handoff allows.
- Under reduce motion every transition lands in one frame (D7).

- AC: each transition's frames at its tick boundaries match the motion page's references within the floor; the threshold layer reproduces the page's venom within the floor on desktop; on API < 31 the venom still draws; reduce motion lands each in one frame.
- Anchors: `reference/design-system/mutant-ui Анимационные спеки.dc.html`, `reference/design-system/mutant-engine.js`, `reference/design-system/mutant-generator.js`

## Result (2026-11-14)

- `BodyTransitions.kt`: `stepped` (whole 83.33 ms steps from the event), `pourDuration`, `PourFront.toPath`, `slid` (ease `1 − (1 − p)²`, stretch `sin(πp)` about the centre, rounded as the engine writes it), `mutantMelt` (330 ms in and out; follows its target without writing state in composition, reads the clock's frame only while running), `gooLayer` and `drawVenom` (ink with a doubled outline under the pill, the fill over it, the fat shapes through a graphics layer blurred 3.2 dp with the alpha matrix `18a − 8.2`), `venomSpring`. `drawBody` takes a pour and hides the highlight while it runs. `MutantSurface`: a change of `fill` pours, a new `SlideRequest` slides, `pourAt` / `slideAt` fix a progress; running transitions read `MutantClock.frame` (new), as the engine steps them per frame; under reduce motion all land at once.
- References: the spec page's chip and check pours (p 0, 0.3/0.33, 0.6/0.66, 1), the switch's track pouring under its sliding thumb, a boil at heat 0.5 and t = 5166 — every slot of each stage recorded and drawn — and twelve venom probes built like the engine's `venom()`. `BodyParityTest` now draws every body of a stage, clips to a clipping stage, and states the floor in pixels (5): every cell 0–3 px. `VenomParityTest`: 0.24–0.88 %, edges only, floor 0.9 %.
- Tests: `SurfaceTransitionsTest` (4: a pour mid-way and done; at once when paused; a slide away and home; a melt by steps and at once when paused), `TransitionTimingTest` (2), `VenomSpringTest` (2, every target).
- Mutants, each fails a named test: the slide's ease made linear; its stretch 0.4 → 0.3; the pour 5 → 4 ms per dp and the step 83.33 → 41.67 (both first survived and are now held by `TransitionTimingTest`); the highlight drawn while pouring; the pour not landing at once when paused; the venom without its goo (11.6 %), its threshold −6.2 (2.9 %), its blur σ 2.4 (0.95 %, just over 0.9).
- Not covered: the pill's own venom choreography in Segmented, Menu and Drawer ([B-31](B-31-segmented-button.md) and on); the check mark's melt against the spec page, which needs the icon ([B-14](B-14-icons-illustrations-patterns.md)); Android below 12 and every non-desktop target for the goo.
