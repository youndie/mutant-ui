---
id: B-200
title: "The tooltip stays in the window and wraps"
status: done
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-200 — The tooltip stays in the window and wraps

The tooltip's plate is cut at the window's edges, and wholly above the window for an anchor in its top 58 dp (F-tooltip-02); it never wraps, so a long hint runs off the window (F-tooltip-03). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 26 and 27 ([решение]).

- **The decision and its reason.** The plate shifts along the edge, 8 dp from the window, still anchored to its target; it flips above or below only when it does not fit vertically. It is at most 240 dp wide, up to three lines (30, 44 and 58 dp high), then an ellipsis.

- AC:
  - for anchors along every edge of the window, the plate stands 8 dp or more inside it and is still its anchor's: over or under it, its span taking in the anchor's middle (rewritten: the plate has no arrow; the answer's words are «привязка к цели сохраняется»)
  - an anchor in the top 58 dp shows the plate below it
  - a long hint wraps at 240 dp to at most three lines at those heights and ends in an ellipsis
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTooltip.kt`

## Result, 2026-11-24

In the tooltip's own file (`components/MutantTooltip.kt`); nothing shared changed.

- **At the edges.** The popup is still 0 × 0 at the anchor's top middle (plakat-ui B-72: its bounds would take every press; B-32's «the plate takes no press» stays green). The plate is placed past it from where the anchor stands in the window (`onGloballyPositioned`, unclipped) and the window's size (`LocalWindowInfo`), by `tooltipSpot`: centred over the anchor and 28 (22) dp above it as before; shifted along the edge to stand 8 dp inside the window; below the anchor only where it does not fit above and does below. A popup is a second root: the placement reads the popup's own place in the window (`positionInWindow` in its placement), not the anchor's root.
- **Wrapping.** The plate is at most 240 dp wide, and 16 dp narrower than the window. The plain plate's words wrap to three lines; the amber plate's first line stays one line, its supporting text takes up to two. The plate is 30, 44 or 58 dp tall for one, two or three lines (a taller line at font scale 2 grows it past that), the words centred; the last line ends in an ellipsis. The words are set at `line-height: 1`, as the amber plate's were: the plain plate's one line keeps its baseline (Handjet 18: 20 px centred in 30, or 18 px with a −1 px half-leading, both at 21).
- **Designed here** (D15, written into research D15): below the anchor the plate's top stands 8 dp under it, the room the plain plate's drips leave above it; its drips still hang down, as every body's do (drips hanging up would be a change to the generator's body); where the plate fits neither above nor below, it stays above. The 8 dp, the 240 dp, the three lines and their heights are the answers'.
- **AC rewritten**: «its arrow still points at the anchor» — the plate has no arrow; the answer says «привязка к цели сохраняется». Held as: over or under the anchor, its span taking in the anchor's middle.

**Frames looked at** (throwaway sheets in `/private/tmp/b200/`, the scratch test deleted): eight anchors in the 1024 × 768 window — the left and right edges, the top, the top-left corner, a rich plate 16 dp in from the bottom-right corner, a long hint in the middle, a long supporting text at the top-right, a one-word hint at the top-right — hovered, at density 1 and 2, in «Эфир» and «Копия», under reduce motion. Before: the plate cut at the left, right and bottom-right, gone above the top, the long hint one line past both sides. After: every plate inside the window, over or under its anchor; the long hints three lines with the dots at the end of the third (`after-long.png`, `after-longrich.png`). Below the top-left anchor the icon button's own drips come within a few px of the plate's top; the words stay clear.

**Tests** (`mutant-core/src/desktopTest/…/components/TooltipEdgesTest.kt`, each failing on the code before the change):

| AC | Test |
|---|---|
| 8 dp inside along every edge, still the anchor's | «along every edge the plate stands 8 dp inside the window, over its anchor»: eight anchors (the four edges, the four corners, one 16 dp in), density 1 and 2; the plate (from its words) inside the 8 dp, over or under the anchor, its span over the anchor's middle, and 0 pixels of the plate's fill in the window's outer 6 px (an area, not an edge pixel). Before: the left edge's plate at −12 |
| an anchor in the top 58 dp shows the plate below | «an anchor in the top 58 dp shows the plate below it, lower down above it»: anchors at 0, 10, 30 and 57 dp, plain and amber, the words 8 dp or more under the anchor and centred on it; at 120 dp the plate stays above. Before: no words in the window |
| wraps at 240 dp to three lines at 30/44/58, ellipsis | «a long hint wraps at 240 dp to at most three lines at 30, 44 and 58 dp, and ends in an ellipsis»: the plate alone with one, two, three and more lines, and the amber plate with one and more supporting lines; its slot ≤ 240 dp and 30 + 14 × (lines − 1) dp tall, the lines counted from the layouts, `layoutInput.overflow == Ellipsis` (Skiko's `isLineEllipsized` answers false; the dots were looked at), the longest cut (`didOverflowHeight`); and in the window, the long hint three lines, its words ≤ 216 dp. Before: the ellipsis was not set (`Clip`) |

`TooltipParityTest` now composes the plate with its own width (`fit = true`) instead of the page's slot width: that width is Chrome's floored to a whole pixel, a fraction under the one word, which the plate now wraps and cuts with an ellipsis. With its own width the plate's words lie within the page's by a pixel and the body differs by 0, 4, 0 and 1 px, under the floor. `TooltipBehaviourTest` (B-32) and `BodiesTuneInTest`'s tooltip case are unchanged and green.

**Mutants**, after the commit, each read from the aimed test's `<failure>` in the JUnit XML, the file copied back and `git status` clean after each:

| Mutant | Killed by |
|---|---|
| no shift along the edge (the horizontal `coerceIn` removed) | «along every edge …»: the left edge's plate at −12 |
| never below (`val y = top`) | «an anchor in the top 58 dp …»: no words in the window for the anchor at 0 |
| no line limit (`maxLines = Int.MAX_VALUE` on the plain plate) | «a long hint wraps …»: the plate 72 dp tall for the long hint |

**Goldens and parity cells**: none moved (no kit golden shows a tooltip; the parity cells' numbers as above). This changes live behaviour only.

**Not verified**: on Android a `Popup` is a window of its own and may clip a plate drawn past its 0 × 0 bounds (B-32's note, unchanged here); iOS and the browser are assumed to share the window's canvas as the desktop does. No phone, no browser. An anchor that moves while its plate is shown (keyboard focus during a scroll) re-places the plate from the anchor's new place; no test holds that.
