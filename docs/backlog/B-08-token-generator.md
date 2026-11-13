---
id: B-08
title: "Tokens generated from mutant-tokens.json, read a second time in a test"
status: open
priority: P1
size: S
stage: stage-1-materials
blocked_by: [B-01]
---

# B-08 — Tokens generated from mutant-tokens.json, read a second time in a test

`mutant-tokens.json` holds both themes' 26 colours, contrast, the Tape, the flash budget, type, grid, genes, 13 type profiles and 20 motion events (research §1.5).

- **The decision and its reason.** `scripts/tokens.mjs` writes one committed Kotlin file; the gate runs it with `--check`; a test reads the JSON independently and recomputes the 28 contrast pairs. The generator's `static TH` and `static K` are compared with the tokens' `color` and `profile` in the same test, so a disagreement between the two sources is a failing line, not a silent pick (D8).
- Rejected: hand-typed tokens. Not covered: the theme object ([B-09](B-09-theme.md)).

- AC: the generated file regenerates byte-identically and the gate fails when it is stale; every colour, profile and motion value is reachable from Kotlin by name; the contrast pairs recompute within 0.05 of the claim; a changed value in the JSON turns the check red.
- Anchors: `reference/design-system/mutant-tokens.json`, `reference/design-system/mutant-generator.js`
