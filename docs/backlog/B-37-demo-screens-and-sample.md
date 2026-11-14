---
id: B-37
title: "The three demo screens and the sample kit on desktop and the web"
status: done
priority: P1
size: M
stage: stage-3-screens
epic: feature-components
blocked_by: [B-15, B-16, B-17, B-18, B-19, B-20, B-21, B-22, B-23, B-24, B-25, B-26, B-27, B-28, B-29]
---

# B-37 — The three demo screens and the sample kit on desktop and the web

The Demo page assembles three screens from the components: the library, a recording, and settings with a dialog, in both themes. The brief requires the demo to be checked in Cyrillic and Latin.

- **The decision and its reason.** The sample shows every component grouped as the pages group them, with a header that switches theme, reduce motion, «Чистый сигнал», language and page; the three demo screens rebuilt from the public API only; wasm on GitHub Pages as the second host. Motion is shown running, not just as states.
- Which language the sample opens in is asked when this item starts (D11). **Answered 2026-11-14: English**; Russian by the header's switch.

- AC (amended 2026-11-14 by the owner, below): each demo screen is rebuilt from the public API as it stands and drawn in both themes, its references kept for the eye with no floor gate; a smoke test walks every section in each theme with the clock held; the sample runs on desktop and in the browser; both languages render without a missing glyph.
- Anchors: `reference/design-system/mutant-ui Демо.dc.html`

**The owner's answer, 2026-11-14.** The Demo page composes its screens with spacing and sizes of its own that the components page does not draw (a 62 dp navigation bar against 68, a tab row of 32 and 4 against 36 and 6, a bar title at 22 against 26, radio labels 10 dp off against 12, a switch's label before it, a progress value read as a time, and more), so the screens could not both be built from the public API only and match their references within the floor. Answered: the components as they are — no parameter is added for the Demo page; each component stays held to its own cell; where a screen departs from the page, the departure is recorded; the screens' references are for the eye.

## Iteration 1 (2026-11-14)

- Done: the sample kit. A header switches «Эфир»/«Копия», reduce motion, «Чистый сигнал», the language (English first, the owner's answer) and the page; the components page holds every component in sections as the design's pages group them, each live (pressed, toggled, typed in, the dialog opened, the snackbar shown), and the Tape runs over the whole. Every word is in `SampleStrings` in both languages, the Russian ones the design's own.
- Held: `SampleAppTest` — every section composes and draws in both themes and both languages with the clock held; the sample opens in English; every word of both languages has its glyphs in all four bundled faces (Handjet and Rubik, two cuts each), with a positive control (a character none carries reads as missing in each).
- Runs: on desktop (the test composes it), and in the browser: `:sample:wasmJsBrowserDistribution` builds, and served locally the page draws the components with no error of its own (the console's one line is a dependency's deprecation notice).
- Left for iteration 2: the three demo screens of the Demo page rebuilt from the public API, and their references and parity. Each departs from the component pages in ways the public API must allow first: a 62 dp navigation bar where the components page draws 68 (the tokens say «62–68»), its items 2 dp apart where they stand 3, chips with a slip of 2, and the Tape over the screen.

## Iteration 2 and result (2026-11-14)

- The Demo page's three screens — the library, a recording, settings under a dialog — rebuilt in the sample from the public API as it stands (`DemoPage.kt`), at the page's 360 × 720 with its 28 dp corners, black ring and the Tape over each, in both themes and both languages. Their references (`Demo_{Library,Recording,Settings}_{Efir,Kopiya}`, from the Demo page with keyframe loss off) are for the eye: `DemoScreensTest` draws each screen and writes the page's beside the kit's to `sample/build/demo/`.
- Where a screen departs from the page, as the owner decided: the components keep their own sizes and spacing (the navigation bar 68, the tabs' row, the bar's title at 26 and its divider, the radio's 12 dp, the switch's label after it); the progress value reads a percentage; the dialog's window is centred; the on-screen-display lines are set in the platform's face, the kit having no public text styles — [B-55](B-55-public-text-styles.md).
- Tests: `SampleAppTest` (3: every section in both themes and languages with the clock held; it opens in English; every word's glyphs in all four faces, with a positive control), `DemoScreensTest` (1: twelve screens drawn, sheets written).
- Runs on desktop and in the browser (the wasm distribution served locally, the components drawn, no error of its own). GitHub Pages waits for the public repository (B-41).
