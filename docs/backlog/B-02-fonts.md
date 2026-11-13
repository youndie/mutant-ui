---
id: B-02
title: "Fonts: Handjet and Rubik as static OFL instances, coverage and metrics pinned"
status: open
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
