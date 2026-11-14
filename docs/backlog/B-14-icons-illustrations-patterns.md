---
id: B-14
title: "Icons, the six illustrations and the four patterns"
status: done
priority: P1
size: S
stage: stage-1-materials
blocked_by: [B-10]
---

# B-14 — Icons, the six illustrations and the four patterns

Sixteen icons on a 24 grid, line 2, square caps, each one SVG path in the page's `ICONS` array; six illustrations built from bodies; four patterns.

- **The decision and its reason.** A script extracts the icon paths into a generated Kotlin file (`ImageVector`s), checked with `--check`; illustrations are compositions of `MutantSurface` bodies with fixed keys; patterns are drawn, not bundled.
- Icons are never damaged (the brief's «порча смысла»).

- AC: the 16 icons match the page within the floor; the generated file is checked; each illustration matches its reference; no icon passes through the generator.
- Anchors: `reference/design-system/mutant-ui Иконки и заготовки.dc.html`

## Result (2026-11-14)

- Icons: `scripts/icons.mjs` reads the page's `static ICONS` and writes `MutantIconPaths.kt` (`--check` in `make check`); `MutantIcons` builds each as an `ImageVector` (stroke 2, square caps, no fill), `MutantIcon` draws one in one colour. Names are the Material icons they redraw («время» is `Schedule`).
- Patterns: `MutantPattern` (Lines, Snow, Raster, Mosaic) and `Modifier.mutantPattern(pattern, quiet)`, drawn in dp from the page's `patterns()`.
- Illustrations: `MutantIllustration(kind)` for the six (`MutantIllustrationKind`), composed of `MutantSurface` bodies with the page's keys, fills and per-slot overrides — `MutantSurface` now takes a `profile` (`MutantProfile.with`), and keys its held instance by the overridden numbers. The cassette's reels turn 30° a scene tick. The labels are the page's words.
- References: 52 more from the Icons page — each icon at 48 and 16 dp, the eight swatches, and each illustration whole and with its text, kept parts and reels hidden. `scripts/design-references.mjs` crops an element as it stands and records its ground, colour, radius, fraction of a pixel and, for an illustration, its stage slots.
- Parity (research §1.13): patterns 0 px; icons 0 px on 25 cells, the other 7 held to their measured residuals (arcs, coverage); illustration bodies 1–16 px, held likewise (one body's outline over another's fill), drawn through two fixture-only hooks that stand each body where the engine did (D16). The whole illustrations, labels in, are printed, not held: their Handjet text reads 2 px low — [B-52](B-52-display-text-against-chrome.md).
- Tests: `IconParityTest` (2), `PatternParityTest` (1), `PatternSourceTest` (2: the alphas a swatch cannot show, read from the page's `patterns()`), `IllustrationParityTest` (3: bodies, determinism, the whole printed), `IllustrationBehaviourTest` (3: an icon is the same on every cassette and tick; reels turn running and stand paused; a pattern in dp at density 2).
- Mutants, each fails a named test: butt caps; a 1.8 line; snow 119 strokes; raster dot 1.2; its fade 0.05; no density scale; an arm's overhang 16; the amber key moved; a rod at −20°; reels that do not turn; a label outside the clean zone; hidden text not laid out; the per-slot profile ignored; the lines' alpha 0.30 and the quiet mosaic's 0.60 — both first survived the swatches (3 levels on the dark ground) and are now held to the page's source.
- Found on the way: [B-52](B-52-display-text-against-chrome.md), the labels' 2 px.
- Not covered: «Копия» (the page renders «Эфир»; the paints are the theme's); the icons' accent at 16 dp is the design's own check, not repeated; the illustrations' text.
