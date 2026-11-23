---
id: B-170
title: "The illustrations draw in dp at any density and keep their size in a narrow slot"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-170 — The illustrations draw in dp at any density and keep their size in a narrow slot

[B-168](B-168-look-and-motion-pass.md)'s empty-state pass ([report](../../logs/b-168/empty-state.md)) reported two
defects in `illustration/`, both of [B-14](B-14-icons-illustrations-patterns.md)'s `MutantIllustration`:

- **F-empty-state-02**: the cassette's reels (`Art.reel`) and the set's two rods (`Art.television`) are drawn in a
  `Canvas` in pixel numbers — the disc's radius 13 and centre (13, 13), the holes' radius 3 and corners, the rods'
  rectangles and their pivots. At density 2 each reel is 13 dp across in the top-left quarter of its 26 dp box and the
  rods are half as long, at half their place, over the set's left shoulder; on a phone at 2.625 a reel is 38 % of its
  size. Every other part of the six is laid out in dp (bodies, labels, the ink boxes), so a frame at density 2
  down-sampled to 1 differs from the frame at 1 only at the reels (876 px), the rods (523–753 px) and the glyph edges
  of the labels (0–194 px), in both palettes. No test saw it: `IllustrationBehaviourTest` holds a pattern at density 2
  and the reels at 1, and the parity runs at 1.
- **F-empty-state-03**: `IllustrationArt` sizes the art with a plain `Modifier.size`, which a smaller constraint wins
  over: in a slot narrower than the art the box is squeezed to the slot and the bodies, placed from its left
  (`position: absolute`), run off to the right alone. B-168 fixed it locally in `MutantEmptyState` with a
  `requiredSize` on its art; `MutantIllustration` used alone still runs off.

The look before the fix: the six illustrations at density 1, 2 and 2.625 in «Эфир» and «Копия», with a diff of each
density-2 frame against the density-1 frame, and each in a 120 dp slot (`/private/tmp/b170/`, not in the repository).

- **The decision and its reason.** The reels and the rods are drawn inside one scale by the density
  (`illustrationInDp`, a `scale(density)` about the canvas's origin), with the page's numbers kept as they are — the
  skill's rule for an engine port's drawing: one scale, not a conversion per number, which is how two of them were
  missed. The parts are fills, with no stroke for the scale to thicken. At density 1 the scale is the identity, so not
  a pixel moves there. `IllustrationArt` sizes the art with `requiredSize`: given less, the art keeps its size, centred
  in its slot, overflowing both sides alike — the skill's rule for a form squeezed under its size and B-168's for the
  empty state, which now takes it from `IllustrationArt` instead of setting it itself. The `MutantIllustration` KDoc
  says so; its signature is unchanged.
- Rejected: converting each literal with `.dp.toPx()` (or `* density`) — eleven numbers in two places, each a fresh
  chance of the slip that made this item, with nothing to tell a forgotten one at density 1. Rejected: scaling the art
  down to fit a narrow slot — it would thicken nothing but shrink the outlines' and labels' weight with it, and text
  never scales (the skill's §1); a consumer that wants a smaller picture has no such size in the design.
- Not a slip, left: `Art.ink` rounds a box up to whole dp (`ceil(w).dp`), Chrome's snap to whole pixels as measured at
  density 1; at another density it stays the same dp, so the picture scales whole. No reference exists at another
  device pixel ratio to say whether Chrome snaps it to the device's pixel instead.

- AC:
  - at density 2 and 2.625 the cassette's two reels and the set's two rods stand where density 1 puts them, scaled by
    the density: their drawn extents, read from the pixels, equal density 1's times the density within 2 px —
    `IllustrationBehaviourTest`, failing on the old code by many times that;
  - `MutantIllustration` in a slot 120 dp wide draws the same picture as in one of the art's width centred at the
    same place — `IllustrationBehaviourTest`, failing on the old code;
  - at density 1 nothing moves: `IllustrationParityTest`, `EmptyStateParityTest` and `EmptyStateBehaviourTest` pass
    unchanged, no golden or reference re-recorded;
  - one mutant per fix killed; `:mutant-core:compileKotlinIosSimulatorArm64` compiles.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/illustration/MutantIllustration.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantEmptyState.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/illustration/IllustrationBehaviourTest.kt`

## Result (2026-11-23)

- `illustrationInDp` draws the reels and the rods; `IllustrationArt` sizes the art with `requiredSize`, and
  `MutantEmptyState` passes it no size of its own.
- Density 1 unmoved: the six illustrations in both palettes at density 1 are the same PNG, byte for byte, before and
  after; at density 2 and 2.625 only the cassette and the set changed. `IllustrationParityTest` (3),
  `EmptyStateParityTest` (1) and `EmptyStateBehaviourTest` (5) pass with no golden or reference touched.
- After: a density-2 frame down-sampled to 1 differs from density 1's at no pixel of the cassette (876 before); the
  set's left differences are its label's glyph edges, as on the test card, which has no hand-drawn part.
- Tests, in `IllustrationBehaviourTest`: the reels (in «Эфир») and the rods (in «Копия») scale with the density, each
  edge within 2 px of density 1's times it (measured: 0.9 px and 1.5 px at most); an illustration in a 120 dp slot
  draws the same picture, to the pixel, as in one of its width centred at the same place.
