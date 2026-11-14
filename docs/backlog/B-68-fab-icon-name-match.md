---
id: B-68
title: "FAB: a consumer's icon is swapped for the kit's on a name match"
status: open
priority: P3
size: S
stage: stage-5-product
blocked_by: [B-40]
---

# B-68 — FAB: a consumer's icon is swapped for the kit's on a name match

Found in [B-40](B-40-qa-pass-and-real-window.md) (F19). `MutantIcons.stroked` looks icons up by `name.removePrefix("Mutant.")`, so a consumer's own `ImageVector` named `Home`, `Add` or `Done` is replaced by the kit's in `MutantFab` and `MutantExtendedFab`.

- **The decision.** The kit's stroked form is found by the icon's identity (the kit's own instances), not its name. The rejected alternative is documenting the reserved names.
- AC: `ComputedValuesTest.F19` passes without its `@Ignore`.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/icons/MutantIcons.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/ComputedValuesTest.kt`
