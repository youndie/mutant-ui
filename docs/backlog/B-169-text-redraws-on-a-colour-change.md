---
id: B-169
title: "A word whose colour alone changes is redrawn in the next frame"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-169 — A word whose colour alone changes is redrawn in the next frame

[B-168](B-168-look-and-motion-pass.md)'s pass found one defect five times, each reported against `MutantText.kt`:

- F-buttons-02 ([report](../../logs/b-168/buttons.md)): a text button disabled in place keeps its accent label, and
  enabled again keeps the grey — 307 accent pixels and 0 grey either way;
- F-checks-switch-02: `enabled` flipped live on a checkbox, a radio button or a switch, their labels keep the text colour;
- F-search-04: the search result's match, a span in the accent, follows the query 1–4 frames late, and under reduce
  motion stays where it was;
- tabs: a click left the old tab in the accent once nothing else redrew the row — on `main` the bar, drawn inside the
  row, redraws it; the tabs branch draws the bar outside, and hands the label its colour as a producer;
- list item: disabled in place, its words keep their colours; the list branch tints each word at draw.

**Why**: [B-164](B-164-the-kits-text-layouts-kept.md)'s `MutantText` drew its kept layout in a `drawBehind` lambda that
captured the layout's holder, the colour producer and the overflow, and nothing it drew by. A new style whose only change
is its colour (or new words whose only change is a span's colour) remeasured to the same size; the lambda, memoised, was
the same, so no draw was invalidated, and the layer replayed its old picture until something else in it redrew.

- **The decision**: the layout is kept by the style without its colour, and drawn by a modifier node, `TextDraw`, keyed
  by what it draws by — the words, the whole style, the producer, the overflow. Any change of those updates the node and
  the update invalidates its draw; the draw lays on the producer's colour where given (it still wins, as the tabs branch
  needs) and the style's otherwise. So a change of the style's colour alone measures nothing again and is drawn in the
  next frame; a span's colour lives in the paragraph's runs, so a change of a span lays the words out again — as
  `BasicText` does — and the node, keyed by the words, redraws them.
- **B-164's saving, held**: two layouts of the same words that differ in colour are one entry of the shared measurer
  (its cache never held the colour as a key either), and a change of the style's colour no longer even remeasures the
  label: the measure policy is remembered by the colourless style, so `onTextLayout` is not called again. Before, it
  remeasured once, a cache hit. Nothing kept is laid out more often than before.
- Rejected: reading `style.color` in the `drawBehind` lambda (B-168's temporary patch) — it redraws through the
  compiler's lambda memoisation, an implicit key that drops the span case (the words are not read in the draw) and still
  remeasures on every colour change. Rejected: invalidating the draw from the measure policy when a new layout arrives —
  it needs the remeasure the colour change should not cost.
- AC:
  - every face above draws its new colour in the first frame after the change, under reduce motion and without it:
    `TextRedrawsOnColourTest` (a `MutantText` whose style colour alone changes, one whose span colour alone changes, and
    a text button disabled and enabled in place in both palettes, with `mainClock.autoAdvance = false` and nothing else
    redrawing), all three failing on `main`; the checkbox, radio button, switch, list item, tabs and search result
    checked against a fresh composition in the new state by a throwaway test (0 label pixels differ; on `main` 784, 784,
    784, 1791, 0 and 721);
  - a colour change alone does not call `onTextLayout` (the same test);
  - every golden, parity and behaviour test unchanged; the browser's build compiles.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantText.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/components/TextRedrawsOnColourTest.kt`
