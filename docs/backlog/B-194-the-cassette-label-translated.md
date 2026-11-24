---
id: B-194
title: "The cassette label is a translated string that fits"
status: wip
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-194 — The cassette label is a translated string that fits

The cassette illustration's label is «БЕЗ ПОДПИСИ» in every language and on every tape (F-screens-04). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 19 ([решение]).

- **The decision and its reason.** The label is a string a consumer translates (through the theme's strings or a parameter). A translation wider than 164 dp at size 20 drops to 16; if it still does not fit, the label carries no text but three lines.

- AC:
  - a short translation draws at 20; one wider than 164 dp at 20 draws at 16; one too wide at 16 draws three lines and no glyph
  - the default draws as today: its goldens unchanged
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/illustration/MutantIllustration.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/theme/MutantTheme.kt`
