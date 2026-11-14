---
id: B-53
title: "Text button: its tracking divider is drawn unscaled at a density above 1"
status: open
priority: P1
size: S
stage: stage-2-components
epic: feature-components
blocked_by: [B-15]
---

# B-53 — Text button: its tracking divider is drawn unscaled at a density above 1

Found in B-23. `TextButtonBody` draws its divider with `drawTrackingDivider(seed, size.width / density, 2f, colors)` straight in the `Canvas`'s pixels: the generator's dp numbers land as pixels, so at density 2 the divider covers half the label's width and stands 1 dp tall. Every test runs at density 1, which hides it. The top app bar's divider (B-23) draws inside `scale(density)`.

- AC: the text button's divider spans its label's width at densities 1, 2 and 3 (a test at 2 that fails without the fix); its parity cell is unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantButton.kt`
