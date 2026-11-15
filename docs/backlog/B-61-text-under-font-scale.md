---
id: B-61
title: "Font scale: fixed heights cut text, and illustration labels leave the art"
status: wip
priority: P2
size: M
stage: stage-5-product
blocked_by: [B-40]
---

# B-61 — Font scale: fixed heights cut text, and illustration labels leave the art

Found in [B-40](B-40-qa-pass-and-real-window.md).

| Finding | Priority | What happens |
|---|---|---|
| F09 | P2 | At font scale 3 every fixed-height component cuts its text top and bottom: buttons and the text button, the extended FAB, chips, checkbox/radio/switch labels, the field, tabs, the snackbar, navigation labels (a button label 54 dp in 48; a chip 45 in 32) |
| F10 | P2 | The top app bar's 26 sp title is cut already at font scale 2 (58 dp in 56) |
| F17 | P3 | Illustration labels are in sp over art fixed in dp: at font scale 2 a label spans −17…254 dp over a 236 dp illustration |

- **The decision.** Heights become minimum heights, so a body grows with its text and the generator draws the grown slot; the touch target is unchanged or larger. An illustration's words are part of the picture and are drawn in dp, at the art's scale. The rejected alternative is shrinking text to fit, which undoes the user's font scale.
- AC: `FontScaleDensityTest.F09`, `.F10` and `.F17` pass without their `@Ignore`; parity cells at font scale 1 unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantButton.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTopAppBar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/illustration/MutantIllustration.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/FontScaleDensityTest.kt`
