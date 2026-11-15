---
id: B-48
title: "Keyframe loss: a band of the screen breaks into 16 and 8 dp blocks every 9 s"
status: done
priority: P3
size: M
stage: stage-6-later
blocked_by: [B-13, B-06]
---

# B-48 — Keyframe loss: a band of the screen breaks into 16 and 8 dp blocks every 9 s

Every 9 s a band of 9–18 block rows breaks into 16 and 8 dp blocks for 900–1400 ms and recovers in 375 ms; the browser does it with a raster snapshot. The handoff calls it optional (D10).

- **The decision and its reason.** Deferred until the Tape and the frame cost are known; in Compose it is a `GraphicsLayer` snapshot or a shader. Its flash budget is in the tokens.

- AC: the owner decides to build it; then it matches the demo references and stays inside the measured flash budget.
- Anchors: `reference/design-system/mutant-tokens.json`, `reference/design-system/mutant-engine.js`

**Answered 2026-11-15 by the owner: build it.**

## Findings (2026-11-15)

**Built.** `MutantKeyframeLoss(modifier, ground) { content }` wraps a screen. Every 9 s, 3.5 s into the window, one loss runs:
- a band of 9–18 rows of 16 dp (fewer on a shorter screen) breaks into blocks 16, 32, 48 or 64 dp wide, three in ten split into halves 8 dp tall;
- each block drags the picture by its own pull, every 125 ms from its own start;
- one block in ten is tinted once in magenta, acid or cyan, and half the blocks of the band's first and last rows are cut out;
- after 900–1400 ms the band recovers from the top in 375 ms.

The loss stops with the clock (reduce motion, a held or frozen screen) and under «Чистый сигнал». The sample gives it to the Demo page's first screen: the engine loses one screen at a time.

**The plan is the engine's.** `moshPlan(seed, n, w, h)` (`gen/KeyframeLoss.kt`) is `moshStart`'s numbers, drawn from `rng(seed + '/' + n, 'mosh')` in the engine's order of calls. `scripts/gen-goldens.mjs` now runs the engine's own `moshStart` in Node on a stub canvas and writes `GoldenMosh.kt`, which `KeyframeLossGoldenTest` holds number for number on every target. The port matched on its first run.

**The layering is the browser's.** The engine draws the loss on a canvas between the page's bodies (its SVG) and its HTML, so the words stand over it. The kit draws it the same way.
- The band's picture is drawn under the screen's content, by the wrapper.
- Every `MutantSurface` inside gives the scene its body's drawing and cuts the band out of its own (`moshBehind`).
- The engine clears half the blocks of the band's first and last rows (`gone`) on its canvas, so the bodies show through them, under whatever a block's drag drew in since. The kit leaves those blocks out of the cut, and draws the band's picture over the body inside them: clear where the body shows, the drag's pixels where they cover it.
- Whatever the screen draws besides the bodies stands over the band, as HTML does over the canvas: words, plain backgrounds.
- The snapshot is the bodies alone, drawn into a bitmap at the screen's density, as the engine rasterises its SVG.
- Each step the bodies' clean zones, and 3 dp round them, are restored from the clean snapshot, as the engine restores them.
- The content stands in a layer of its own, so a step redraws the band and not every body.

**Differences from the engine, stated.**
- The engine's canvas lies over its bodies, so a block dragged below the band shows over a body there. The kit's lies under them: below the band only the ground shows the drag.
- The kit runs one loss per wrapper, where the engine round-robins over a page's visible stages.

**Parity** (`KeyframeLossParityTest`, «Эфир»): the specs page's four frozen frames — before the loss, 45 % and 95 % of it, and 1.2, recovering.
- The script now records each element crop's stage name (it seeds the bodies) and the loss's plan as the engine keeps it (`st.m`). The fixture finds which loss of the page the stage's was by that plan.
- Every frame's band, blocks, pulls and tints lie on the page's. Outside the words the frames differ by 75 px before the loss: the bodies' own residual. At 45 %, 95 % and recovering it is 300, 205 and 181 px: the same residual copied along by the blocks a few steps, as the page copies its own. Recorded and never to be raised.
- **The cleared blocks.** Inside them the frames are held at 2 levels, not 16: a body's edge there is a few levels off the ground. 37, 31 and 15 px lie further than 2 from the page at 45 %, 95 % and recovering, the bodies' own edges. The first build cut the bodies out of the whole band and showed the ground in those blocks instead. It read 114 px at 45 %, and its 86 px that this changed now lie within 2 of the page, against 9 before. Drawing the bodies in them without the drag's pixels over them read 58.
- The words stand as the kit drew them before the loss. Their glyphs, found as where the frame differs from itself with its words transparent, move by no more than 34 levels at their edges, from the snapshot's raster under them. A smeared word moves by far more.

**Behaviour** (`KeyframeLossBehaviourTest`):
- a loss starts 3.5 s into the 9 s window, is over after its run and 375 ms, and comes again in the next window;
- none runs under reduce motion or «Чистый сигнал»;
- **through a cleared block the screen shows as it is**: on a cleared block's first row, which no drag reaches, the frame is the screen's without a loss, with its body standing and once the body has gone;
- **the flash rule.** No pixel changes its relative luminance by 0.1 or more, in alternating sign, more than three times a second: 0 px² against the threshold of 21 824 px² (tokens `flash.rule`, WCAG 2.3.1). The share of the band changed a step is printed: 1.2–3.2 % on this small, dense stage, where the design measured at most 2.9 % on its demo screens. The algorithm is the engine's, so the share is the stage's, not a departure.

**Mutants** (each one line of `MutantKeyframeLoss.kt`, killed only where the aimed test is named among the failures):

| Mutant | Killed by |
|---|---|
| a block pulls the wrong way (`x − vx`) | `KeyframeLossParityTest` |
| no clean-zone restore | `KeyframeLossParityTest` |
| the bodies not cut out of the band | `KeyframeLossParityTest` |
| no recovery from the top | `KeyframeLossParityTest` |
| the loss 2.5 s into its window | `KeyframeLossBehaviourTest` |
| a loss under reduce motion | `KeyframeLossBehaviourTest` |
| no tint | `KeyframeLossParityTest` |
| no ground under the band | `KeyframeLossParityTest` |
| the cleared blocks not cleared | `KeyframeLossBehaviourTest` (it survived every frozen frame, and found that the first build hid the bodies under them) |
| the drag's pixels not drawn over the bodies in them | `KeyframeLossParityTest` (the cleared blocks) |
| the cleared blocks not left out of the cut | `KeyframeLossBehaviourTest` |
