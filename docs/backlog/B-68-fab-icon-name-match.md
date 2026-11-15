---
id: B-68
title: "FAB: a consumer's icon is swapped for the kit's on a name match"
status: done
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

## Findings (2026-11-15)

**Fixed.** `MutantIcons.stroked` finds the kit's icon by the instance, among `MutantIcons.all`, no longer by the name. A consumer's own `ImageVector` named `Home`, `Add` or `Mutant.Done` is drawn as given in `MutantFab` and `MutantExtendedFab`. The kit's own icons still get the FAB's 2.5 line.

**Test.** `ComputedValuesTest.F19` passes without its `@Ignore`: a consumer's black square named `Home` draws apart from the kit's Home. The icon button and icon tests are unchanged.

**Mutant.** The lookup by name again is killed by F19.
