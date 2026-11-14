---
id: B-13
title: "The Tape («Лента»): one overlay over the screen"
status: done
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-07, B-09]
---

# B-13 — The Tape («Лента»): one overlay over the screen

Scanlines, grain, a tracking band 22 dp tall passing for 2100 ms every 7000 ms, and head noise at the bottom edge, per theme (the tokens' `tape`): «Эфир» black scanlines 7.5 % one in three dp, «Копия» none (D10).

- **The decision and its reason.** One layer over the screen root, stepping by ticks, drawn from small tiles where it can; off under «Чистый сигнал», still under reduce motion. Its cost is part of [B-06](B-06-frame-cost.md).
- Rejected: the Tape per component, and a full-screen shader as the first version (the brief's own first retreat).

- AC: the Tape matches the demo page's references at fixed times within the floor; the tracking band passes on the 7 s cycle; contrast under it is measured at the worst tick ([B-38](B-38-accessibility.md)).
- Anchors: `reference/design-system/mutant-tokens.json`, `reference/design-system/mutant-ui Демо.dc.html`, `reference/design-system/mutant-engine.js`

## Result (2026-11-14)

- `MutantTape.kt`: `drawTape` (scanlines, the band with its two borders, the grain tile clipped and translated per tick, the head's noise from `rng('hd', stage + tick)`, both themes), `tapeBandTop`, and the public `Modifier.mutantTape()` — drawn over its content in dp at any density, on the clock's new `sceneTick` (fixed 125 ms, the engine's `fi`), off under «Чистый сигнал», standing under reduce motion. The demo screens put it on their root when they are built ([B-37](B-37-demo-screens-and-sample.md)).
- References: the spec page's drift frames at 3000, 7350, 8050 and 8900 ms, card under the Tape (`Tape_Drift*_Efir`). The first render showed no band and no noise: the reference script's font rebuild left each frozen stage's tick unchanged, so the engine never stepped the Tape again (research §1.12); it now forgets the tick, and only these four references changed. `BodyParityTest`: 0 px in each.
- Contrast: `TapeContrastTest` reads the kit's own pixels. Under the band's body every declared pair stays on the design's side of 4.5:1 («Копия» `text-disabled / background` 4.43, declared 4.4); the declared figures differ by up to 0.17, from their own rounding (≤ 0.12) and 8-bit pixels (≤ 0.08). The band's 1 dp edge and the noise go far lower in «Эфир» (`text / background` 2.50 and 1.68) — not judged here, handed to [B-38](B-38-accessibility.md).
- Tests: `MutantTapeTest` (6: the band's cycle and steadiness; scanlines in both themes; clean signal; still when paused, stepping when running; the scene's tick at a 24 fps resting tick; density 2), `TapeContrastTest` (1), `BodyParityTest` (+1).
- Mutants, each fails a named test: scanline alpha 0.075 → 0.1 (first survived parity, under the 16-level threshold; now the scanline test, on white); band height 22 → 20; grain step 37 → 36; noise `W / 10` → `W / 12`; grain stroke 3 → 2; the band's share 0.3 → 0.35; clean signal ignored; the resting tick instead of the scene's; no density scale; band alpha 0.07 → 0.1 («Эфир») and 0.05 → 0.08 («Копия»); the scene tick never published (first hung the tick test; its wait is now bounded).
- Found on the way: `main`'s native test compilation was red since B-12 (a comma in a test name); fixed on its own branch before this one merged.
- Not covered: «Копия» against a reference (the spec page draws «Эфир»; the colours are the engine's own); the Tape's cost, which [B-49](B-49-frame-cost-on-a-pixel-6a.md) and the demo screens measure; a screen resized under a frozen clock, which the engine itself leaves bare.
