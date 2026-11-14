---
id: B-37
title: "The three demo screens and the sample kit on desktop and the web"
status: wip
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

- AC: each demo screen matches its reference within the floor in both themes; a smoke test walks every section in each theme with the clock held; the sample runs on desktop and in the browser; both languages render without a missing glyph.
- Anchors: `reference/design-system/mutant-ui Демо.dc.html`

## Iteration 1 (2026-11-14)

- Done: the sample kit. A header switches «Эфир»/«Копия», reduce motion, «Чистый сигнал», the language (English first, the owner's answer) and the page; the components page holds every component in sections as the design's pages group them, each live (pressed, toggled, typed in, the dialog opened, the snackbar shown), and the Tape runs over the whole. Every word is in `SampleStrings` in both languages, the Russian ones the design's own.
- Held: `SampleAppTest` — every section composes and draws in both themes and both languages with the clock held; the sample opens in English; every word of both languages has its glyphs in all four bundled faces (Handjet and Rubik, two cuts each), with a positive control (a character none carries reads as missing in each).
- Runs: on desktop (the test composes it), and in the browser: `:sample:wasmJsBrowserDistribution` builds, and served locally the page draws the components with no error of its own (the console's one line is a dependency's deprecation notice).
- Left for iteration 2: the three demo screens of the Demo page rebuilt from the public API, and their references and parity. Each departs from the component pages in ways the public API must allow first: a 62 dp navigation bar where the components page draws 68 (the tokens say «62–68»), its items 2 dp apart where they stand 3, chips with a slip of 2, and the Tape over the screen.
