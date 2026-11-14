---
id: B-05
title: "Pixel references rendered here from the pages at a fixed engine time, and the parity floor"
status: done
priority: P0
size: M
stage: stage-0-risks
blocked_by: [B-01, B-02]
---

# B-05 — Pixel references rendered here from the pages at a fixed engine time, and the parity floor

A reference nobody rendered from the source is a reading of it. The pages need the network (React, ReactDOM and Babel from unpkg, fonts from Google) and run a live clock (research §1.1, §1.2, H5).

- **The decision and its reason.** A Node script drives headless Chrome over each page, serves the bundled fonts in place of Google's and the three runtime scripts from a pinned copy, pauses the engine (`setPaused(true)`) at a fixed time, and photographs each component state into `references/` with a manifest of the page's hash and the engine time. The gate checks the manifest, not Chrome, because CI's documentation job runs on Linux.
- The floor is measured once, on a probe that is the same body drawn by Chrome and by Compose with the content removed, and written into the research.
- Rejected: screenshots taken by hand from the design tool.

- AC: `make references` re-renders every reference; two runs give identical PNGs; the manifest check fails when a page or a font changes without re-rendering; the floor is a number in the research with how it was measured.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`, `reference/design-system/mutant-ui Компоненты v1.dc.html`, `reference/design-system/support.js`, `reference/design-system/mutant-engine.js`

## Result (2026-11-14)

- `scripts/design-references.mjs` renders in headless Chrome 157 over CDP with no dependency: probes on a blank page (the vendored generator's contour at t = 5000, fill and ink stroke) and page cells (the Components page with the engine frozen by `data-mu-t` on every stage and keyframe loss off, reached by wrapping `window.MutantEngine` as the page assigns it). Fonts are the repository's, served in place of Google's. The runtime is fetched from unpkg at its pinned versions rather than vendored: the URLs carry their versions, and a pinned copy would be a second thing to keep.
- Seven references: `Body_Button|Card|Dot_Efir|Kopiya` and `Button_Rest_Efir`, the Components page's resting button with its drips, mosaic and caption, looked at. `make references` renders twice and requires byte-identical PNGs; it did. `make check` runs `--manifest`, which fails when a page, the generator, the engine, `support.js` or a font changes without a re-render: proved by altering one hash in the manifest (exit 1, "mutant-engine.js changed").
- **The floor**: `BodyFloorTest` draws the port's contour in Compose as the probe draws it in Chrome; 0.010 / 0.017 % for the button, 0.000 % for the card and the dot, in «Эфир» / «Копия». Frozen at 0.02 %. Mutants: an outline 20 % thicker 2.644 %, another instance 12.322 %.
- Findings for later items: the engine seeds the outline's tremble with its running slot count (`uid`), a host artefact B-10 replaces; a page shows «Копия» only through its Tweaks control, so page cells are «Эфир» until a component needs the other theme.
- Not covered here: the text floor and each component's cells, which arrive with the components that need them; viddik's design parity, which arrives with the first component fixture.
