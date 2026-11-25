---
id: B-213
title: "The Demo's phone alone in the browser, for a page to embed"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-213 — The Demo's phone alone in the browser, for a page to embed

The owner, in chat on 2026-11-25: «Добавляй на kotlin.website кит». The site's `/ui` section shows each kit's live demo as the kit's own Pages in an iframe at a phone's size, 390 × 844, as it does for toon-ui and plakat-ui (their `?scene=app`, toon-ui#27 and plakat-ui#18). mutant-ui's browser sample opens either the whole sheet, its header and pages around 360 × 720 phones, or one of B-73's bench scenes by `?scene=<name>`; neither fills a phone.

- **The decision and its reason.** As the sister kits do: `?scene=app` opens the Demo's phone screens alone, the screen filling the viewport under small chips (two short rows, see the result) that pick the screen (Library, Recording, Settings under a dialog) and the palette («Эфир», «Копия»). Nothing else of the sample's sheet is shown. The screen is the Demo page's phone as it is (the app, its clock, its Tape, the Library's keyframe loss), only without the 360 × 720 frame, the ring and the rounded corners: the embedding page draws the phone around it. Reduce motion is off whatever the browser says, as the sample's header defaults it, so the Tape and the bodies run in the embed.
- **Rejected:** the sample's header switches in the embed (reduce motion, «Чистый сигнал», steps, the language), which are the sheet's, not the app's; the 360 × 720 phone centred in a 390 × 844 viewport, which leaves a band of page round it inside the site's own phone.

- AC:
  - the scene shows each Demo screen and each palette
  - the screen fills a 390 × 844 viewport
  - the Tape and the bodies run
  - reduce motion is off
  - a test holds what can be held on desktop: the scene is a `@Composable` shared through commonMain, and a sample test renders it at 390 × 844 in both palettes
  - the bench scenes are unchanged
- Anchors: `sample/src/wasmJsMain/kotlin/io/github/youndie/mutant/sample/Main.kt`, `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/DemoPage.kt`

## Result, 2026-11-25

`sample` only: no golden, parity cell or public API of the kit moves. On `feat/b-213-the-phone-alone-in-the-browser`, on
this Mac.

- **The scene.** `DemoAppScene` (commonMain), opened by `?scene=app` in the browser's `Main.kt` before B-73's names are
  looked up, so every bench scene opens as before and an unknown name still opens the sheet. Its own `MutantTheme`
  with `reduceMotion = false`; the page's ground; then the chips; then the screen filling the rest.
- **The chips stand in two rows**, the three screens over the two palettes: in one row the five chips are 437 dp wide,
  past the embed's 390 (the «Copy» chip was cut at the window's edge). The two rows are 104 dp, so the screen is
  390 × 740. The labels are the sample's English strings (Library, Recording, Settings; «On air» and «Copy» for «Эфир»
  and «Копия»), as the sheet opens in English (D11).
- **The screen** is the Demo page's phone (`DemoScreenFill`): `DemoPage.kt`'s screen box now takes its frame, the
  page's 360 × 720 ring with its corners or the scene's room clipped to its bounds; the app, the screen's clock, its
  Tape and the Library's keyframe loss are shared. Settings opens under its erase dialog, as the page's does.
- **Where to tap**, in CSS px of a 390 × 844 viewport at the scene's first load (from the semantics' bounds): the chips
  Library (8–85, 4–52), Recording (93–190, 4–52), Settings (198–284, 4–52), «On air» (8–77, 52–100), «Copy» (85–149,
  52–100); the screen from y = 104.

AC:

- [x] the scene shows each Demo screen and each palette — `DemoAppSceneTest` taps each chip in each palette and
  captures the screen; in headless Chrome at 390 × 844, six shots looked at
- [x] the screen fills a 390 × 844 viewport — the test holds the screen from 0 to 390 across and from the chips' 104 dp
  to 844 down; in Chrome the viewport reads 390 × 844 with no band round the screen
- [x] the Tape and the bodies run — the test sees the screen change in 250 ms; two Chrome shots 400 ms apart differ in
  the Tape's band and the bodies' drips (PSNR 26 dB)
- [x] reduce motion is off — `reduceMotion = false` in the scene's theme; with it `true` the same test reads 0 pixels
  changed
- [x] a test holds what can be held on desktop — `DemoAppSceneTest` renders the shared `@Composable` in a 390 × 844
  window at density 1, in both palettes; the browser entry point itself is looked at in Chrome
- [x] the bench scenes are unchanged — `BenchScene.kt` is untouched; `?scene=DemoLibrary` and `?scene=ComponentsRest`
  open as before in Chrome

**Tests**, each killed by a mutant after the commit: the screen at 360 × 720 instead of the room (right edge 360, not
390), the theme's `reduceMotion = true` (0 pixels changed), the «Copy» chip picking «Эфир» (mean level 52 in both).

**Gates** on this Mac, on `origin/main` with B-212: `make check` green; `make docs-against BASE=origin/main` clean;
`./gradlew check :mutant-core:compileCommonMainKotlinMetadata` green (787 tests, 0 failures, read from the JUnit XML,
the iOS simulator and wasm browser suites included).
