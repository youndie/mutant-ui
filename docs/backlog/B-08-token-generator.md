---
id: B-08
title: "Tokens generated from mutant-tokens.json, read a second time in a test"
status: done
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

## Result (2026-11-14)

- `scripts/tokens.mjs` reads `mutant-tokens.json` and runs the vendored generator for its `static K` and `TH`, and writes `MutantTokens.kt` in the layout ktlint keeps (checked: `ktlintFormat` leaves it unchanged). Public: `MutantColors` (26 token colours, the engine's comet and cocoon paints, the disabled tint and darkening, the print flag; internal constructor) and `MutantPalettes.Efir` / `Kopiya`. Internal: `MutantType`, `MutantGrid`, `MutantBody` with each `MutantProfile`, `MutantMotion`, `MutantGenome`. The ABI dump records the public part.
- Where the tokens and the engine both speak they must agree, or `--check` fails; the one known disagreement — the pill's "full" radius, which the engine does not flag round — is listed with its cause, and the engine's value is generated (D4).
- `MutantTokensTest` (5) reads the JSON again in Kotlin: every colour of both themes and none missing; the 28 contrast claims recompute within 0.05 and none is under 4.5; the grid, the type scale, every profile, the motion durations and six genes.
- Mutants: the Efir primary one unit off, the pill flagged round, the hot tick at 83.3 — each fails `tokens.mjs --check` and the named test. Renaming the known disagreement fails the check twice, as an unexpected disagreement and as a known one that no longer occurs.
- Findings, in research §1.5: the engine's paints the tokens lack; the pill; the hot tick's 83 against the engine's 83⅓; the Tape's parameters written as prose.
