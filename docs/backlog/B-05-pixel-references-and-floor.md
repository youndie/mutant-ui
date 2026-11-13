---
id: B-05
title: "Pixel references rendered here from the pages at a fixed engine time, and the parity floor"
status: open
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
