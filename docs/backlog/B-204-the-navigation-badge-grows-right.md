---
id: B-204
title: "The navigation badge grows right and stops at 99+"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-204 — The navigation badge grows right and stops at 99+

The navigation badge grows left from its right edge: «12» covers half the icon, and in 30 dp cells it reaches the next (F-nav-bar-04). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 36 ([ошибка]).

- **The decision and its reason.** The badge grows rightwards from the icon's top-right corner, and caps at «99+».

- AC:
  - badges of 1, 12 and 99+ start at the icon's top-right corner and grow right; none covers the icon's ink
  - a count above 99 reads «99+»
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationBar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantBadge.kt`

## Result

**Decided.** The compact badge's plate stands with its left edge on the 22 dp icon's right edge, 2 dp above the 56 × 30
icon box as before, so any count grows right, away from the icon; its 1.5 dp ring reaches that far into the icon's
22 dp square and none of its ink (the clock's, the lens's and the knobs' ends 2.75 dp inside it). Right to left it is mirrored: the right edge on the
icon's left. The corner is computed from where the box centres the icon, so a 30 dp cell's shrunken box still has the
badge at its icon. A count above 99 reads «99+».

- **How the cap reaches the badge.** `MutantNavigationItem.badge` stays a `String`, and the bar reads it: a text of
  digits only whose value is above 99 (by its digits, so any length caps) shows «99+»; any other text, «-5», «1 000»,
  «НОВОЕ», «99+» itself, as given. No public API changed (`checkKotlinAbi` unchanged). Rejected: a `count: Int`
  constructor on the item or a `MutantBadge(count)` overload — the item already takes the count as text, every consumer
  writes `"${n}"`, and a second way in widens the API for what one rule over the text answers.
- **The parity cell moves**: the page's «3» stands 5 dp further right (plate 129–144 → 134–149 at the page's 328 dp),
  856 px of `MutantNavigationBar_Rest_Efir` against a 250 px line. A departure (research D20's registry), with the
  switch `LocalMutantNavigationBarAsDesigned` that `NavigationBarParityTest` provides; with it the cell holds as before.
- **Widths, Handjet 14, 5 dp in**: «1» and «3» 16 dp, «12» 22 dp, «99+» 28 dp. At the page's 82 dp cells «99+» on the last
  destination ends at 326 dp of the 328 dp bar, its ring at 327.5 (columns 298–325).

**Crowded bars — the badge covers the next icon (not decided here, for the designer).** In twelve 30 dp cells (B-66)
the box shrinks to the cell, the icon stands 4 dp in from each side, and the next icon begins 8 dp past the cell's end.
The plate starting on the icon's right edge, 4 dp before the cell's end, covers the next icon's left 8 dp («1»), 14 dp
(«12») or 20 dp («99+») over the badge's height, the top 10 dp of the next icon. On the last destination it runs past the
bar's end. A badge clears the next icon's 22 dp box when the cell is at least 23.5 dp + its width: 39.5 dp for one digit,
45.5 for two, 51.5 for «99+» — in a 360 dp bar, nine, seven and six destinations. The answer says nothing of narrow
cells, so no rule was invented (shrinking, clipping or moving the badge are each a look the designer has not drawn).
Before this item the badge sat on its own icon in such cells instead (plate 38–53 over the icon at 34–56).

**Tests** (`mutant-core/src/desktopTest`, each failing on the code before, as the mutants below show):

| AC | Test |
|---|---|
| badges of 1, 12 and 99+ start at the icon's top-right corner and grow right | `NavigationBarBadgeTest` «badges of 1, 12 and 99+ start at the icon's top-right corner and grow right, at density 1 and 2»: the amber's first column on the icon's right edge on the clock, the lens and the knobs, rights increasing; «right to left, the badge grows left from the icon's top-left corner»; `NavigationBarBehaviourTest` «the badge is the amber count at the icon's corner» (was «… over the icon box's corner», now from the icon's right) |
| none covers the icon's ink | `NavigationBarBadgeTest` «no badge covers its icon's ink»: every pixel of the bare icon at its ink colour unchanged under «1», «12», «99+», density 1 and 2 (before: «12» took dozens) |
| a count above 99 reads «99+» | `NavigationBarBadgeTest` «a count above 99 reads 99+» (the bar: «100» and a 30-digit count read «99+», «99» and «НОВОЕ» as given), «the cap reads whole numbers by their digits and leaves other text» |
| parity cells unchanged, or the moved ones listed | `NavigationBarParityTest` with `LocalMutantNavigationBarAsDesigned`; the registry entry in research D20 |

`NavigationBarBehaviourTest`'s pill reader moved from row 26 to 28: the badge's ring ends in row 26 and now stands over
the second destination's pill's right end, which cut «a pick while the pill slides …»'s landing reading to 112 for 123.

**Mutants**, after the commit, each aimed test named among the failures in the JUnit XML, the file restored with
`git checkout` and `git status` clean:

| Mutant | Killed by |
|---|---|
| the badge at the page's place (`w - 6 dp - width`) | «badges of 1, 12 and 99+ …», «no badge covers its icon's ink», «right to left …», «the badge is the amber count at the icon's corner» |
| no cap (`return text`) | «a count above 99 reads 99+», «the cap reads whole numbers …» |
| the switch ignored (`if (false)`) | `NavigationBarParityTest` «the navigation bar matches the page»: both cells off, 856 px, item 1's icon part 127–155 against the page's 127–150 |

**Goldens.** None moved: the repository's PNGs are the Chrome references, untouched; the cell holds with the switch.

**Frames looked at** (throwaway sheets, deleted): bars with «3», «12», «99+», «1» on the clock, the lens and the knobs,
and twelve 30 dp cells with «9», «12», «99+», in «Эфир» and «Копия» at density 1 and 2, under reduce motion, before and
after. Before, «12» and «99+» stood over half the clock, lens and knobs; after, every badge begins at the icon's corner.

**Not verified.** No phone and no browser: Android, iOS and wasm were compiled and run only as `./gradlew check` runs
them. The sample's demo bar (`DemoApp`, the queue's size) was not looked at in a window.
