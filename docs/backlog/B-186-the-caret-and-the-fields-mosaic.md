---
id: B-186
title: "The caret restarts, sits beside the letters, and the mosaic ignores the text"
status: done
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
  - at the end of the line: a 9 × 20 dp block 2 dp after the last character's box; inside: a 2 dp line whose centre is the centre of the gap between the neighbouring letters' boxes (the insertion point), drawn over their edges and covering no more of each than half the line (rewritten 2026-11-23 on the owner's answer below; the text's API on Skiko gives a letter's box, not its ink)
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

## The owner's answer, 2026-11-23

«Да, по слову дизайнера»: option 1. Inside the text the caret is a 2 dp line centred in the gap between the letters,
drawn over their edges. «No glyph pixel covered» was the item's own wording, not the designer's, and the AC above is
rewritten to what is decided: the line's centre is the centre of the gap between the neighbouring letters as measured,
2 dp wide, over the letters, covering no more of either than the measurement leaves. Measured, here, by the letters'
boxes, which meet at the insertion point, as `getPathForRange` gives boxes on Skiko; the block at the end of the line
is placed from the last character's box for the same reason, as the designer's «после последнего символа» says.

The two readings above stand as readings the owner did not object to: the clean zone is the slot less 14 dp on all
four sides (24 dp tall in a 52 dp field), and the blink's 5 and 3 are counted on the scene's tick, the restart's own
tick the first lit one.

## Result, 2026-11-23

`MutantTextField.kt` only; the search bar takes all of it through the shared `TextFieldBody`.

- **The blink** (answer 8). `TextFieldCaret` counts five scene ticks lit and three dark from the tick of its last
  restart. It enters the composition with focus, and a change of the `TextFieldValue` (a letter, a cursor move) restarts
  it, read in its draw: lit on the frame after the event, and drawn only on ticks, as before.
- **The caret's place** (answer 17, the owner's word above). The block stands 2 dp past the last character's box
  (`ceil` of its edge, so never closer); inside the text the 2 dp line is centred on the insertion point, where the
  neighbouring boxes meet, over their edges. At the line's start and over a placeholder it stays the bar before the first
  letter (B-58, B-168).
- **The clean zone** (answer 18). A `Spacer` matching the slot, inset 14 dp on every side, is the field's only mark;
  the prefix, the placeholder, the value's line and the caret no longer mark it.
- **The departure** (research D20). The page's caret block stands 8 dp past the text, so the focused parity cells of
  the field and the search bar moved (content 5 px left of the page's). `LocalMutantTextFieldAsDesigned` draws the page's
  8 dp, and both parity tests provide it; D20 now has its departures registry, this the first entry. The parity cells
  take the engine's clean zone, so the zone's change moves none of them; it is listed in the registry as what no cell
  sees.

**Tests** (`mutant-core/src/desktopTest`, each failing on the code before, run with the implementation reverted):

| AC | Test |
|---|---|
| visible on the frame after focus, then 5 lit and 3 dark | `TextFieldCaretBlinkTest` «focus brings the caret lit on the next frame, then five ticks lit and three dark» |
| … and after each keystroke | `TextFieldCaretBlinkTest` «every keystroke starts the blink over lit» (three letters and a Left arrow, each in a dark tick) |
| the block 2 dp past the last character | `TextFieldCaretPlacementTest` «at the end of the line the block stands 2 dp past the last letter», density 1, 2, 3: gaps 2, 4, 7 px from the ink |
| the line centred, over no more than half its width of either letter | `TextFieldCaretPlacementTest` «inside the text the line stands centred …», «Nigh\|t» at density 2 and 3: centres 91.5 / 137.5 against the ink gap's 91.0 / 136.5, over «h» 1 px, over «t» 2 and 3 px (half the line) |
| the mosaic identical for «», one letter, a full line | `TextFieldCleanZoneTest`, the field and the search bar, rest and focused, «Эфир» and «Копия», density 1 and 2: 0 px (before: 38–401 px in the sheets) |

The older placement tests now ask for 2 dp before the block, not 6 px, and the bar inside a long value for a letter
within 8 px before it (the comma before the space before «to»), as the line now stands on the insertion point.

**Mutants**, each after the commit, the aimed test named among the failures in the JUnit XML, the file restored with
`git checkout`:

| Mutant | Killed by |
|---|---|
| the blink on the screen's tick (`tick.mod(8)`) | «focus brings …»: dark on the frame after focus at the screen's tick 5 |
| the blink restarted by focus only (`blink.on == null`) | «every keystroke …» |
| the block on the box's edge (no 2 dp) | «at the end of the line …»: gaps 0 |
| the line before the insertion point (`x - w`) | «inside the text …»: over «h» 3 px at density 2 |
| the input area marked only with a value | «the text field's body …»: 1401 px at density 1 |
| the clear icon's mark 48 dp tall | «the search bar's body …»: 469 px at density 1 |

**Goldens.** None moved: the repository's PNGs are the Chrome references, untouched; the parity cells hold with the
switch above, the bodies 0 px against the page as before.

**Frames looked at** (throwaway sheets, deleted): the blink after focus and three letters, both palettes; the caret at
the end and walked back through «Night to», «Эфир» and «Копия» at density 1 and 2; the bare body under «», «N» and a full
line. At density 1 the line covers a pixel of each neighbour, as decided; «Nigh|t» reads with the stem of «h» whole.

**Not verified.** No phone and no browser: Android, iOS and wasm were compiled and run only as `./gradlew check` runs
them. A real IME and right-to-left text inside a word were not looked at (the right-to-left block and bar follow the
same rules by code, held only by `OverlayAndMouseTest`'s short cases).
