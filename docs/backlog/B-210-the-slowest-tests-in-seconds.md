---
id: B-210
title: "The slowest tests run well under their one-minute limit"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-210 — The slowest tests run well under their one-minute limit

`runComposeUiTest` and `runTest` give a test body a fixed minute. Several tests sit close to it, and a slow macos-latest
runner or a loaded Mac turns them red. PR #136's CI failed on the sample's `ComponentsPageTest` «no design stage is
named on either page, in either language», which passed the minute (run 45262647941) and passed on a rerun. A local
full `./gradlew check` under load timed out six: the sample's `AccessibilityTest` (28–55 s after
[B-209](B-209-the-accessibility-test-in-seconds.md)), two `ComponentsPageTest`s, `DisabledContrastTest`,
`MenuEdgesAndChosenRowTest` and one `LongContentSweepTest` cell. Others are known slow: `LongContentSweepTest` 50 s,
`StillLayersOnceTest` 36 s, `ConstraintsSweepTest` 28–30 s.

- **The decision and its reason.** Each test above about 20 s is timed phase by phase, as B-209 did, and sped up where
  its time goes, without dropping anything it reads: pixels read once into an array with lookup tables, a composition
  reused across cases, a capture cut to the region the test reads, or one long test split into several that together
  hold the same cases.
- Rejected: a longer or wrapped timeout (`timeout =`, `runTest(timeout = …)`), which leaves a test a slow runner away
  from red; fewer screens, themes, languages, cells, ticks or assertions; a faster kit clock or a longer frame, which
  moves what is drawn at each captured moment; `@Ignore`; kit code changed only to make a test faster, unless it is a
  pure speed-up held by the existing goldens.

- AC:
  - every test above 15 s on this Mac is listed here with its time, read from the JUnit XML
  - every listed test runs well under 30 s alone on this Mac, before and after recorded here
  - every changed test holds every assertion it held, and kills a mutant of what it guards
- Anchors: `sample/src/desktopTest/kotlin/io/github/youndie/mutant/sample/AccessibilityTest.kt`,
  `sample/src/desktopTest/kotlin/io/github/youndie/mutant/sample/ComponentsPageTest.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/components/DisabledContrastTest.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/components/MenuEdgesAndChosenRowTest.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/LongContentSweepTest.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/ConstraintsSweepTest.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/surface/StillLayersOnceTest.kt`

## Result, 2026-11-24

**What the minute is.** `runComposeUiTest` passes its body to `runTest` with a limit of 60 s, read off the harness's
bytecode (`ComposeUiTest.skiko.kt`, `bipush 60`): the minute is one body's, not one JUnit test's. A test of many short
bodies cannot pass it by its total; a single long body can. Under load the bodies grow several times over: in the full
`./gradlew check --continue` below, with the Mac's load average at 17–38, `AccessibilityTest` took 174.9 s against
25.4 s alone, and two tests failed on the minute, `ComponentsPageTest` «no design stage is named on either page, in
either language» (83.5 s, two bodies) and `DisabledContrastTest` (97.7 s, two bodies).

**Every test above 15 s**, from that full check's JUnit XML (668 tests), with the same test alone, its module's
`desktopTest` run by itself:

| Test | Full check, s | Alone, s |
|---|---:|---:|
| `AccessibilityTest` «active words hold 4_5 to 1 after the Tape at the worst tick, no tick flashes a quarter, the red stays» | 174.9 | 25.4 |
| `LongContentSweepTest` «every component takes long, empty and mixed text without throwing» | 107.4 | 47.0 |
| `DisabledContrastTest` «the disabled words stand where they were measured, under the Tape» | 97.7 | 18.0 |
| `ComponentsPageTest` «no design stage is named on either page, in either language» | 83.5 | 24.2 |
| `ComponentsPageTest` «the page opens at its top though its overlays are shown from the first frame» | 67.0 | 9.8 |
| `ConstraintsSweepTest` «every component in every place at density 3» | 57.2 | 27.9 |
| `ConstraintsSweepTest` «every component in every place at font scale 3» | 53.8 | 27.2 |
| `ConstraintsSweepTest` «every component in every place lays out and draws» | 52.3 | 36.0 |
| `ComponentsPageTest` «each section shows the states the design's table names» | 45.5 | 7.2 |
| `VenomPickMidRunTest` «a pick during the drawer's run changes no frame more than a run does» | 44.2 | 18.4 |
| `DemoTapeOnceTest` «a phone on the Demo page carries one Tape, its own» | 42.6 | 6.3 |
| `MotionEveryFrameTest` «every kind of movement moves on every frame by default and steps with the design's steps» | 38.8 | 7.2 |
| `SampleLayoutTest` «no drop a button lets fall over 30 s reaches the captions under the cells» | 36.7 | 11.9 |
| `StillLayersOnceTest` «the still layers drawn once draw what each tick draws» | 36.0 | 35.6 |
| `BodyReversalTest` «a card's pour changed back at any moment pours over the picture it shows» | 34.8 | 18.9 |
| `MenuEdgesAndChosenRowTest` «30 rows in a 400 dp window stand 16 dp from its top and 34 dp from its foot and scroll to the last» | 34.7 | 14.7 |
| `VenomPickMidRunTest` «a pick during the menu's run changes no frame more than a run does» | 34.0 | 13.5 |
| `HoverKeepsTheFillTest` «under the pause a hover shows its finished tremble at once, still, at the resting tick's time» | 29.5 | 15.8 |
| `TallBodyContourTest` «a sheet's body is stitched between neighbours at every height» | 28.8 | 18.9 |
| `SampleLayoutTest` «the header's hints are lines no wider than their titles, in both languages» | 26.7 | 11.4 |
| `PressedLabelContrastTest` «a pressed body's label reads at 3 to 1 on every frame of its press and release» | 25.1 | 9.0 |
| `DemoScreensTest` «the three screens draw at 360 by 720 in both themes and both languages, and are written beside the page's» | 23.7 | 9.2 |
| `VenomPickMidRunWordsTest` «the drawer's words read on the slime while a run settles» | 22.6 | 10.7 |
| `ComponentsPageTest` «the index chooses a group, and the page composes that group's sections alone» | 22.2 | 8.2 |
| `VenomPickMidRunTest` «a pick during the drawer's stepped run changes no frame more than a run does» | 22.1 | 9.6 |
| `SampleAppTest` «every section composes and draws in each theme and language, the clock held» | 21.8 | 6.6 |
| `VenomPickMidRunTest` «a pick during the segmented button's run changes no frame more than a run does» | 19.2 | 8.0 |
| `CheckboxParityTest` «every checkbox and radio row the page draws matches it, in both themes» | 18.6 | 5.2 |
| `SteadyWebsTest` «a web between still drips does not jump with the boil» | 16.6 | 9.7 |
| `ComponentsPageTest` «every section has its head and labelled rows of captioned specimens» | 16.2 | 7.0 |
| `CalendarShortOfRoomTest` «given less height or width than it wants, no row of the calendar overlaps another» | 15.8 | 3.4 |
| `VenomPickMidRunTest` «a pick during the menu's stepped run changes no frame more than a run does» | 15.7 | 6.9 |

The 25 not changed here are all under 19 s alone. `TallBodyContourTest` runs no Compose test, so no minute.

**Where the time went**, timed phase by phase in throwaway tests (deleted):

- `ComponentsPageTest` «no design stage»: one page per language switched through seven groups and three demo
  screens, a second of the clock at each; each second of the Components page costs 0.6–1.8 s of frames, its first
  composition 2.3 s cold; reading the texts 0–8 ms. A body of 12–18 s alone. `everyGroup` (the heads and the states)
  switched one page through seven groups, a body of 7 s.
- `AccessibilityTest`: after B-209 the frames, as B-209 found; its eight bodies 2–4 s each, one test.
- `DisabledContrastTest`: a frame and its capture each 1.1–1.2 s a cycle in «Эфир», 3.95 s each in «Копия», plus a
  `getRGB` and three `pow`s a pixel for every pixel and again for its box's ground. «Копия» is 3.3× «Эфир» because of
  the kit (findings).
- `MenuEdgesAndChosenRowTest` «30 rows»: 470 frames per density (30 presses, 250 ms apart), 15 ms a frame, 7–9 s a
  body; the presses 2 s.
- `LongContentSweepTest` and `ConstraintsSweepTest`: about 620 and 520 cells of their own `runComposeUiTest`, 70 ms
  each: 29 ms of it starting the harness (a bare `runComposeUiTest` costs 22 ms), 22 ms the first composition, 21 ms
  the 300 ms of clock, 2 ms the capture. No place or component is far above the rest (the slowest cell 0.35 s).
- `StillLayersOnceTest`: 48 bodies of 64 frames, each frame followed by 6 ms for the images ahead: 18.4 s of its 36 s
  is that wait, kept, since what the images ahead deliver in it is what the test compares.

**What changed** (test code only; nothing in `mutant-core` main or the sample's main):

- `ComponentsPageTest`: each place opens a page of its own, as «the page opens at its top» already did, and «no design
  stage» is four tests, the Components page and the Demo page in each language; `everyGroup` opens a page per group.
  Shown equivalent: for all twenty places in both languages, the page opened at a place and the page switched to it
  give the same list of semantics nodes, tag, text and content description each, in order (3 980 nodes).
- `AccessibilityTest`: four tests, one per case, each in both themes, the same eight bodies. The printed table (46
  lines: largest change, red area, the five worst words of each case) is identical to the one before.
- `DisabledContrastTest`: one bulk `getRGB` per frame, a table of the 256 channel values (the same doubles `pow` gave),
  the ground's luminance once a box; and a test per theme. The printed table is identical to the one before.
- `MenuEdgesAndChosenRowTest`: «30 rows» is a test per density.
- `LongContentSweepTest`: a test per text (300 characters with no space, of words, empty, mixed), the same cells.
- `ConstraintsSweepTest`: each sweep is a test for the five widths and one for the five boxes and unbounded places, and
  a test holds that the two are every place, each once.
- `StillLayersOnceTest`: a test per clock and theme, both densities each; the sheets' shares go to
  `build/b-117/shares-<part>.txt`.

**Before and after, alone** (JUnit XML, this Mac, each module's tests run by themselves):

| Test | Before, s | After, s |
|---|---:|---:|
| `AccessibilityTest`, one test → four | 25.4 | 5.4, 5.6, 8.5, 11.3 |
| `ComponentsPageTest` «no design stage», one → four | 24.2 | 2.1, 2.8, 7.6, 8.2 |
| `ComponentsPageTest` «every section has its head…» | 7.0 | 8.4 |
| `ComponentsPageTest` «each section shows the states…» | 7.2 | 8.1 |
| `DisabledContrastTest`, one → two | 18.0 | 3.1 («Эфир»), 12.0 («Копия») |
| `MenuEdgesAndChosenRowTest` «30 rows», one → two | 14.7 | 6.8, 8.9 |
| `LongContentSweepTest` «every component takes…», one → four | 47.0 | 8.0, 10.3, 13.5, 13.5 |
| `ConstraintsSweepTest`, three → six | 36.0, 27.9, 27.2 | 13.5–16.8 |
| `StillLayersOnceTest`, one → four | 35.6 | 8.5, 8.6, 9.2, 9.5 |

The first of a JVM's tests carries its warm-up (`AccessibilityTest`'s 11.3 s, the first to run). The longest single
bodies now: «Копия»'s disabled cycle about 12 s, the menu's 30 rows at density 2 about 9 s, an accessibility case
2–4 s, a page of the Components page about 1–1.5 s.

**Inside a full `./gradlew check --continue`** after the change (686 tests, 0 failures, 12 min against 28 min before;
the Mac less loaded, its load average 5–11): every changed test between 1.5 and 15.2 s, but the sample's first,
`AccessibilityTest`'s Recording, at 28.4 s with the JVM's warm-up. Above 15 s there are seven tests left, none changed
here: `BodyReversalTest` 20.7 s, `HoverKeepsTheFillTest` 18.8, `VenomPickMidRunTest` (the drawer) 17.5,
`PressedLabelContrastTest` 15.7, the two `SampleLayoutTest`s 15.6, and `ConstraintsSweepTest`'s boxes 15.2.

**Mutants**, one per changed class or more, each read from the aimed test's `<failure>` in the JUnit XML, restored
with `git checkout`, the tree clean after:

- `AccessibilityTest`: «Эфир»'s `textSecondary` `AAB4CF` → `6E7488` (B-209's): all four killed, the Library's words
  2.98 and the Recording's 3.91 under 4.5, as in B-209.
- `ComponentsPageTest`: the English «rest» caption → «rest later»: «no design stage is named on the Components page, in
  English» killed, naming Foundations, Actions and the other groups; the Russian and Demo tests pass.
- `DisabledContrastTest`: «Копия»'s `textDisabled` `6F6858` → `9A9384`: «…in Kopiya» killed at 2.48; «…in Efir» passes.
- `MenuEdgesAndChosenRowTest`: the window's foot margin taken as the top's (`MENU_EDGE_TOP + MENU_EDGE_TOP`): both
  «30 rows» tests killed, 384 for 366 at density 1 and 768 for 732 at density 2.
- `StillLayersOnceTest`: the still layers drawn 2 dp off in their image (`stillLayersImage`): both running-clock tests
  killed, 0.8–2.3 % of the pixels past the line of 0.2 %; and the frozen clock's exception dropped (`clock.frozen` out of
  `stillOf`'s condition): both frozen tests killed, «the fixtures' pixels moved». A shift of the image's grid alone
  survived: it is undone where the image is drawn.
- `LongContentSweepTest`: `MutantBadge` throwing on a text of 100 characters or more: the no-space and words tests
  killed, naming `ListItem.Badge` and `Badge`; the empty and mixed tests pass.
- `ConstraintsSweepTest`: `MutantDivider` throwing at width 0 or under an unbounded width: all six sweep tests killed.

**Findings.**

- **The kit's cost, not changed here.** «Копия»'s disabled body draws its halftone a dot at a time: the print branch of
  `scanlines()` in the disabled drawing (`BodyStates.kt`) calls `drawCircle` every 3 dp across the body's box, some
  thousands a frame for the five disabled components. That is 9 ms a frame and 9 ms a capture in the software test
  window against 2.5 ms in «Эфир», and the 12 s body left here, the longest. Rasterised once as a repeating fill, as the
  Tape's grain and scanlines are (B-77, B-107), it would be a pure speed-up only if the goldens hold it, and the fill
  samples nearest on Skiko; an item of its own. The open 30-row menu costs 15 ms a frame.
- **The harness's start.** A `runComposeUiTest` costs 22 ms to open and close with nothing in it: of the sweeps' 41 s
  per 520 cells, 15 s. Composing the cells in one test was not taken: a cell that throws breaks the composition the
  others would share, and the clock would no longer start at 0 for each.
- **The `LongContentSweepTest` cell that passed its minute** under load before this item is not explained by its work:
  a cell is 70 ms alone. It was not reproduced here.

**Gates on this Mac:** `make check`; `make docs-against BASE=origin/main`; `./gradlew check --continue` with the test
tasks' outputs cleaned first (686 tests, 0 failures; ktlint, `checkKotlinAbi` and `compileKotlinIosArm64` ran in it).
No declaration was added to `commonMain`.

**Not verified.** The CI runner's times; no phone.
