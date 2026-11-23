---
id: B-199
title: "No mosaic block in the clean zone, pressed or at rest"
status: done
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-199 — No mosaic block in the clean zone, pressed or at rest

While pressed, the edge mosaic shifts tiles into the clean zone — two of seven keys, every tick, 176–186 px — against D5; the engine does the same (F-foundations-04). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 25 ([ошибка]).

- **The decision and its reason.** A mosaic block that crosses the clean zone is not drawn, at rest or pressed. The designer's next package will do the same; under D20 this is made here first. Only frames with heat move: the generator's goldens for them are listed as this item's known entries, as `design-golden.mjs`'s `KNOWN` lists its own.

- AC:
  - no mosaic pixel inside the clean zone at any heat, over every key of the foundations page
  - the generator's goldens without heat are unchanged; those with heat are named with this item, computed by the test, and looked at — *rewritten 2026-11-23, see the Result:* the generator's goldens, with heat or without, are all unchanged, as they do not carry the running mosaic; the hot golden frames whose running mosaic the change moves are named with this item, computed by a test (none of 27)
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/Shapes.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/CleanZone.kt`, `scripts/design-golden.mjs`

## Result, 2026-11-23

`surface/BodyStates.kt`'s `runningMosaic`, with the zone test shared with the generator.

- **The rule** (answer 25). A pressed body's running mosaic (the engine's `drawItem`, the `data-px` tiles) leaves out a
  tile whose shift along its side makes it meet the clean zone (`cz`: the content's box plus 6 dp across and 2 down, then
  2 dp to spare), by the same test the generator's `edgeMos` uses at rest (`Slot.blockMeetsCleanZone`, `gen/Model.kt`).
  At rest nothing changes: `edgeMos` already places no block in the zone, and the blocks past the contour (B-137) were
  already kept out of it. Every tile still takes its four draws from the `px` stream, so every tile that is drawn is
  the engine's, to the bit. A running tile now carries its row, so `drawMosaicPastContour` takes the edge row by row
  rather than by its index in the instance's tiles.
- **The departure** (D20). `runningMosaic(asDesigned = true)` draws the engine's way; no theme or composition switch,
  as no parity cell moved (below). D20 has a dated note under its registry.
- **The AC on the goldens, rewritten.** The orchestrator's AC expected the generator's hot golden frames to move. They
  cannot: `scripts/gen-goldens.mjs` emits what `mutant-generator.js` returns — the instance (its mosaic blocks and tiles)
  and, per frame, primitives, grid, contour, highlight and flow lines — and the running mosaic is the engine's, drawn
  in `drawItem`, not the generator's. The designer's «векторы изменятся только для кадров с нагревом» is about the
  engine's pressed tiles. So every generator golden stays bit-identical (`gen-goldens.mjs --check`,
  `design-golden.mjs --check` in `make check`: no `KNOWN` entry added), and the test that names the moved frames
  compares the kit's running mosaic with the engine's way over the goldens' instances at their hot frames. It found
  **none of the 27**: at those ticks no tile is shifted into a zone. Its control, the same instances hot over 240
  ticks, finds 31 tiles in the zone the engine's way. `B199_ZONE_FRAMES` is the empty list, held by the test.

**Tests** (each failing on the code before; the mutants after the commit):

| AC | Test |
|---|---|
| no mosaic pixel in the clean zone at any heat, over every key of the Foundations page | `PressedMosaicCleanZoneTest` «no mosaic tile meets the clean zone at rest or at any heat, over every key of the Foundations page»: the page's 15 keys (both languages), a card at 258 and 290 dp with three content widths, a button and a chip, heat 0.05 to 1 in 20 steps over 120 hot ticks; 0 tiles at rest and hot (before: hot tiles in the zone on several card keys, 595 for `theme-card-Эфир`); control: the engine's way puts 14 609 tiles in the zone over the same sweep |
| … as drawn | `PressedMosaicCleanZoneTest` «a pressed card draws no mosaic pixel in its clean zone, in both palettes and at density 2»: a real press through the interaction source, 36 captures over 2.9 s, the card drawn with its mosaic and without it, compared inside the zone inset by 1 px; 0 px on every capture (before: 16–144 px on three or four captures of each run checked) |
| the generator's goldens; the hot frames named, computed | `GeneratorGoldenTest` «only the hot golden frames B-199 names move and only by the tiles it keeps out of the clean zone»: kit and engine equal tile for tile at heat 0; at heat, the kit's tiles are the engine's less those in the zone; the set of such frames equals `B199_ZONE_FRAMES` (empty) |

**Mutants**, each after the commit, the aimed test named among the failures in the JUnit XML, the file restored with
`git checkout` and `git status` clean:

| Mutant | Killed by |
|---|---|
| the zone test off (`false && …`), the engine's way | both `PressedMosaicCleanZoneTest` tests: «theme-card-Эфир: hot 595 tiles in the zone», …; «card-3 d1 Эфир: [… 48 …]» |
| a third-row tile left out too (`… \|\| p.row == 2`) | `GeneratorGoldenTest`'s B-199 test: «btn-a @5000.0: a frame with no heat lost a tile» |

**Goldens and parity cells.** None moved. Under the first mutant every parity test was run and its 330 `_ACTUAL`
images compared byte for byte with the fixed code's: all identical (the pressed cells included: `Button_PressedBody_Efir`,
`MutantButton_Pressed_*`, `MutantButton_PressedBare_*`). No golden was re-recorded.

**Frames looked at** (throwaway sheets, deleted): a pressed card, keys `card-3`, `card-6`, `loss-card`, `theme-c-Копия`,
`theme-card-Эфир`, «Эфир» and «Копия», density 1 and 2, every 80 ms through the rise and at heat 1. Before: a dark or
tinted 8 dp block over the zone's top-right corner, by the label's last letters, on 3–7 of 36 captures (12–312 px).
After: none; the tiles beside the zone still run, recolour and shift as before.

**Found on the way.** F-foundations-04's «one key at rest shows 12 px at the zone's bottom-left corner» is not the
resting mosaic: `edgeMos` places no block that meets the zone, and the sweep above finds none at rest over every key.
Not identified further. And this Mac's shared Gradle daemon lost its `~/.gradle/caches/9.8.2/workerMain` during the
run (every test executor failed with `GradleWorkerMain` not found); the gates ran with `--no-daemon`. Under a load
average of 32 from parallel builds, `AccessibilityTest` timed out once in `./gradlew check` and passed alone.

**Not verified.** No phone and no browser: Android, iOS and wasm were compiled and run only as `./gradlew check` runs
them. The mosaic past the contour (`mosaicPastContour = true`) pressed was not looked at; its edge row now filters by
the tile's row, which no test aims at.

