---
id: B-02
title: "Fonts: Handjet and Rubik as static OFL instances, coverage and metrics pinned"
status: done
priority: P0
size: S
stage: stage-0-risks
blocked_by: [B-01]
---

# B-02 — Fonts: Handjet and Rubik as static OFL instances, coverage and metrics pinned

The tokens name Handjet (display, on-screen text) and Rubik (text), both OFL; the handoff says both have Cyrillic, which nobody here has read (research §1.6, H2).

- **The decision and its reason.** Fetch the variable files from google/fonts at a commit, read their cmap and axes, cut static instances at the weights the tokens and pages use (Handjet 400 and 600; Rubik 400 and 500, 700 if a page draws it), bundle them with their OFL files, `androidResources { enable = true }`. Static, because a `wght` axis is ignored under API 26 and the default instance draws (D9).
- A coverage test holds every family to every string the fixtures render; a metrics test pins advance, cap and x-height.
- Rejected: the variable files as shipped. Not covered: the reference renderer serving them ([B-05](B-05-pixel-references-and-floor.md)).

- AC: the research records the coverage read from each cmap, with the address it was read at; both families draw Cyrillic and Latin on all four targets; the coverage test fails on a string with a missing glyph.
- Anchors: `reference/design-system/mutant-tokens.json`, `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `scripts/fonts.py` fetches `Handjet[ELGR,ELSH,wght].ttf` and `Rubik[wght].ttf` with their OFL texts from `google/fonts` at `2fe1e8c6`, pins them by SHA-256 in `scripts/fonts.lock`, and cuts `handjet_regular` (400), `handjet_semibold` (600), `rubik_regular` (400) and `rubik_medium` (500), every other axis at its default; a second run is byte-identical. `mutantFonts()` builds the `display` and `text` families. `make fonts` reruns it.
- H2 settled: both families carry the whole Russian and Latin alphabets. Rubik's variable default is 300 — a further reason for the statics (research §1.6).
- `MutantFontCoverageTest` (3) holds all four instances to the 167 code points of the ten pages; allowed missing: ▶ ❚ (the pages' pause button), ◄ ► (DatePicker arrows, B-45). `MutantFontMetricsTest` (4) pins Handjet's 647 cap, 471 x-height and 480-unit advance module, identical across weights; Rubik's 700 cap, 520 x-height and its widening with weight; the four weights by `OS/2` and by ink; and the layout through compose resources at the files' advances.
- Mutant killed: `rubik_medium.ttf` replaced by the 400 file fails two metrics tests by name (`expected [400, 600, 400, 500] but was [400, 600, 400, 400]`); restored byte-identically.
- The klib ABI dump gains only the compiler's `$stableprop` entries of the internal `Res`.
- Platforms: desktop loads and lays out the fonts in the test; the Android AAR carries the four fonts and both licences under `assets/composeResources/`; iOS and wasm compile and package them in `check`, and drawing there is first seen in the sample (B-37).
- Finding: the pages set Handjet 500 in four places but load only 400 and 600, so Chrome draws 400 there; Compose's matching for a missing 500 also picks 400.
