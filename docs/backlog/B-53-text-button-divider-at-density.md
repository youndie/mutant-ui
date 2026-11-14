---
id: B-53
title: "Text button: its tracking divider is drawn unscaled at a density above 1"
status: done
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

## Result (2026-11-14)

`TextButtonBody` draws the divider inside `scale(density)`, as the top app bar's divider does. Held by
`ButtonBehaviourTest`: at densities 1, 2 and 3 the strokes reach past three quarters of the button's width and stand
2 dp tall; a mutant drawing them unscaled again fails it at density 2. The parity cell is unchanged (density 1).
B-40's density sweep did not see this: it compares a component's ink box at densities 1 and 2, and the label sets
that box, so a divider short by half moved nothing.
