---
id: B-27
title: "LinearProgress and CircularProgress"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-07, B-08]
---

# B-27 — LinearProgress and CircularProgress

MVP. Linear: no body, segments 6 + 2 dp, determinate and indeterminate (a running band). Circular: a 36 dp reel stepping 30° per tick, indeterminate.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantLinearProgress(progress, modifier, showValue)`: a 12 dp black track ringed 2 dp in black, filled to `progress` (clamped to 0…1, NaN as 0) with acid segments 6 dp long and 2 apart, and the value 12 dp after it in the OSD amber (Handjet 20, three digits, `042%`). A progress bar of that value.
- `MutantLinearProgress(modifier)`: the same track with the engine's `rewind` band running through it — 96 dp, a quarter at 15 % acid, a quarter at 40 %, half solid — 12 dp each scene tick, round from beyond its start to beyond its end (`(fi · 12) % (pw + w) − w`). Indeterminate.
- `MutantCircularProgress(modifier)`: the engine's `reel` — a 36 dp spool in the page's paper `#e9f2e4`, ringed 2.5 dp in black, its three 8 dp holes turning 30° each scene tick. Indeterminate.
- The black `#05060c` and the paper are the page's literals in both themes; the acid is both themes' primary.
- Designed here (D15): under reduce motion the band stands where the page's still frame has it (tick 40) — at the clock's paused tick 0 it lay wholly before the track, an empty track (found by its test, fixed); the reel stands at its tick.
- References (4): the list stage's determinate row and its indeterminate row with the reel, as they stand, both themes.
- Parity: the band exact; the value's digits where the page's ink lies and in its colour. Listed: the determinate fill's end, one anti-aliased column of 12 rows (42 % of a track Chrome keeps fractional); the reel 43 and 63 px — Chrome rasterises its `border-radius` circles and turns the holes as a layer, the kit draws its circles turned (stood at the element's 0.19 px it reads 131 and 178: Chrome draws it whole). The reel's ring is a disc under the spool, as `box-shadow` is; drawn as a stroke beside it, it left a seam that read 47 and 121.
- Mutants (25), each failing a named test: the track 10, its ring 1.5 (first survived: a half-covered row read as black, alpha unread), the segments 5 and 3, the value's gap and size and padding, no clamp (first survived: the bar cannot draw past itself; now read in the semantics), no NaN guard, the band 80 and 10 a tick, its quarters at 20 % (first survived: under the pixel threshold; now read) and 50 %, the reel 32, its ring 2, its holes 6 and moved, 45° a tick, its paper (first survived; now read at its centre), the ring in «Копия»'s ink, each semantics, the segments unclipped, the paused band at the clock's tick.
- Tests: `ProgressParityTest` (1), `ProgressBehaviourTest` (5).
