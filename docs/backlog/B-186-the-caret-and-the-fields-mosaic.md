---
id: B-186
title: "The caret restarts, sits beside the letters, and the mosaic ignores the text"
status: question
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-186 — The caret restarts, sits beside the letters, and the mosaic ignores the text

The caret blinks on the screen's tick and does not restart on focus or typing (dark up to 375 ms after a click, F-text-field-04); its 2 dp bar hides a letter's right stem (F-text-field-05); the mosaic moves when the first letter is typed or the last deleted (F-text-field-07). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 8, 17 and 18 (all [ошибка]).

- **The decision and its reason.** The blink restarts on its visible phase at focus and at every input; the cycle is 5 ticks on, 3 off. At the end of the line the caret is a 9 × 20 dp block 2 dp after the last character; inside the text it is a 2 dp line centred in the gap between letters. The field's clean zone is its input area (the slot less 14 dp each side), never the text, so the mosaic does not depend on it.
- The search bar shares the field and takes the same.

- AC:
  - the caret is visible on the frame after focus and after each keystroke, then 5 ticks on and 3 off
  - at the end of the line: a 9 × 20 dp block 2 dp after the last glyph's ink; inside: a 2 dp line whose centre is the gap's centre, no glyph pixel covered
  - the mosaic's pixels are identical for an empty field, one letter and a full line
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTextField.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSearchBar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/CleanZone.kt`

## Iteration 1, 2026-11-23

On `fix/b-186-the-caret-and-the-fields-mosaic`, on this Mac, desktop JVM, `runComposeUiTest` with the clock driven by
hand. Throwaway contact sheets (deleted, not committed) of the field «ПОИСК Nig» focused and typed into, «Night» with the
caret walked left from the end, and the bare body under «», «N» and a full line, rest and focused, in «Эфир» and «Копия»
at density 1 and 2.

**Before.** As F-text-field-04, -05 and -07 say. The caret came lit on focus for 128 ms and was then dark for 350 ms;
the block stood 8 dp past the text; the bar covered the right stem of «h», «g» and «N» in every look. The bare body under
«N» or a full line differed from the empty field's by 118 px (rest) and 38 px (focused) at density 1, «Эфир», and by
401 and 121 px at density 2, «Копия»: the flow lines and marks in the band right of the placeholder.

**Tried, not committed.** Each settled part works as the item says:

- the blink counted in scene ticks from the tick of the last restart (focus brings the caret into the composition; a
  change of the `TextFieldValue` restarts it, read in the caret's draw, so it is lit on the frame after the event and
  draws only on ticks): lit 5, dark 3, restarted by each letter;
- the clean zone a `Spacer` matching the slot, inset 14 dp, the only mark in the field (the prefix, the placeholder,
  the value's line and the caret unmarked): the bare body is identical, 0 px, for «», «N» and a full line, rest and
  focused, both palettes, both densities. The empty field's own picture changes (its top band loses two flow lines);
- the block 2 dp past the end of the last character's box.

**What the code cannot read.** `TextLayoutResult.getPathForRange` returns the selection boxes on Skiko, not the glyphs'
outlines: the «ink» of every letter it gives is its advance box, gap 0.00 between every pair. A caret placed by a
glyph's ink needs the ink measured another way (a letter rasterised once into a small bitmap and its side bearings
cached, which works in common code), or the AC's «ink» read as the character's box.

## The question

**The AC «a 2 dp line whose centre is the gap's centre, no glyph pixel covered» cannot hold in Rubik 15.** Measured by
rasterising each letter of «Night to Tuesday, Ночь на вторник lmnr fit Find a recording» as the field draws it, the
clear pixels between neighbouring letters' ink:

| Density | Clear pixels between letters | The line | Pairs where a centred line covers ink |
|---|---|---|---|
| 1 | −2 … 2 («ht» 0, «gh» 2, «ig» 0, «to» 0, «Ni» 1) | 2 px | all but «gh», «чь», «mn», «in» (2) |
| 2 | −3 … 4 («ht» 1, «ig» 2, «Ni» 3) | 4 px | all but «gh», «чь», «in» (4) |
| 3 | −3 … 6 («ht» 2, «ig» 4, «Ni» 5) | 6 px | all but «gh» (6) |

The designer's answer 17 says only «линия 2 dp по центру межбуквенного промежутка»; «no glyph pixel covered» is the
item's own wording. In the finding's own case, «Nigh|t», the gap is 0 px at density 1. Which one gives way?

1. **The line centred in the gap, 2 dp, drawn over the letters.** It covers up to 1 dp of each neighbour's edge, never
   a whole stem. The AC becomes «centred in the gap, covering at most half the line's width of each neighbour». This
   is the designer's word as written.
2. **The line under the letters.** Centred in the gap, drawn before the text, so no glyph pixel is covered, but in the
   text's colour it merges with the stems either side («h|t» reads as a thick stroke at density 1).
3. **The line narrowed to the gap**, at least 1 px: no ink covered, but not 2 dp wherever letters stand closer.
4. **A 1 dp line** (the alternative F-text-field-05 named): covers less, still covers in «ht», «ig», «to» at density 1.

Two readings this iteration took and the answer may change; they decide nothing above and are listed so one reply
settles them too:

- **«The slot less 14 dp each side»** read as all four sides: the zone is 24 dp tall in a 52 dp field, about the
  prefix's line, so the flow lines and marks in the top and bottom bands stay. Read as the CSS content box (`padding: 0
  14px`, the full 52 dp), every mark between the 8 dp side strips goes.
- **«5 ticks on, 3 off»** counted on the scene's tick, the restart's own tick being the first lit one, so the first lit
  run after an event is 500–625 ms and the caret draws only on ticks. Counted in milliseconds from the event it would be
  625 ms exactly, but the caret would redraw on every frame while a field is focused.

Who decides: the owner, with the designer if option 1's covering is not acceptable. Nothing is committed but this note;
the tried change is not on the branch.
