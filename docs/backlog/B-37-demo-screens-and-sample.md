---
id: B-37
title: "The three demo screens and the sample kit on desktop and the web"
status: open
priority: P1
size: M
stage: stage-3-screens
epic: feature-components
blocked_by: [B-15, B-16, B-17, B-18, B-19, B-20, B-21, B-22, B-23, B-24, B-25, B-26, B-27, B-28, B-29]
---

# B-37 — The three demo screens and the sample kit on desktop and the web

The Demo page assembles three screens from the components: the library, a recording, and settings with a dialog, in both themes. The brief requires the demo to be checked in Cyrillic and Latin.

- **The decision and its reason.** The sample shows every component grouped as the pages group them, with a header that switches theme, reduce motion, «Чистый сигнал», language and page; the three demo screens rebuilt from the public API only; wasm on GitHub Pages as the second host. Motion is shown running, not just as states.
- Which language the sample opens in is asked when this item starts (D11).

- AC: each demo screen matches its reference within the floor in both themes; a smoke test walks every section in each theme with the clock held; the sample runs on desktop and in the browser; both languages render without a missing glyph.
- Anchors: `reference/design-system/mutant-ui Демо.dc.html`
