---
id: B-44
title: "SearchBar: a focused field with suggestion chips and result rows"
status: done
priority: P3
size: M
stage: stage-6-later
epic: feature-components
blocked_by: [B-20, B-17]
---

# B-44 — SearchBar: a focused field with suggestion chips and result rows

Later (after publication, by the brief). Bodies `field`, `chip` and `icon`: a focused TextField, suggestion chips and result rows. State: focused.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты позже.dc.html`

## Findings (2026-11-15)

**Built.** Four public parts, as the later page composes them (`l-search`, 328 dp):

- **`MutantSearchBar`.** It holds the focused text field (`ПОИСК`, the block caret, `data-calm=".3"` and the brackets come from the field itself), a clear icon at its end, and a column of whatever comes under it, 24 dp apart. The text field's body gained an internal trailing slot, keyboard actions and an outside focus requester for it.
- **`MutantSearchSuggestions`.** Filter chips with the page's 14 dp label padding (a filter chip's is 16), 6 dp apart and wrapping 8 dp apart. Each body is keyed by its place (`sg1`, `sg2`…).
- **`MutantSearchResult`.** A list row 60 dp tall (a list row is 64) whose headline draws the first match of the query, case aside, in `textAccent`. The list row's body now takes a styled headline and a row height; a disabled or bare row draws it plain.
- **`MutantSearchResultLoading`.** The page's cocoon row: a 40 dp tile and bars of 70 % and 45 %, 14 dp tall and 8 dp apart.

**Designed here (D15).** The page draws one still state; these numbers are the kit's.

The clear icon:
- shows only while the query is not empty;
- empties the query and hands focus back to the field. Pressed, it had taken focus and left with the query, so the field lost it; a test caught it.
- draws the focus brackets 3 dp around itself from the keyboard;
- shows the hand, and is named by `clearLabel` (`Очистить`) to a screen reader.

The keyboard's action is Search, and `onSearch` hears it. The page's chips toggle, but a suggestion here hands its words over instead. `selected` can mark the one taken.

**Two layout causes found by parity:**
- **A chip is 32 dp drawn in a 48 dp touch box.** Rows of boxes put the second row 16 px low. The suggestions lay out the drawn rows instead: boxes overlap by 8 dp between rows, and the 8 dp a box reaches above the first row and below the last stay out of the height. Every chip keeps a 48 dp target.
- **Chrome lays a chip at its label's fractional width**, and the engine stands the next body on the nearest pixel (`sg2` at 167). Compose rounded the label up, so the second chip stood a pixel late, 771 px of outline over the floor. Each chip now advances the row by that rounded width and is drawn at its own: 0 px in «Эфир».

**Parity** (`SearchBarParityTest`, both themes).
- The bare twin hides words, icons and the caret, as the text field's does. The reference script hides the caret with the words for this cell.
- Bare: 0 px in «Эфир». In «Копия» it is 6 px against a floor of 5, all single speckle pixels over the result's tile under the print slip (research §1.15). That residual is recorded and never to be raised.
- The words and icons are held where the page puts them, within 1 px on every side and their colour within 16: the prefix, the query with its caret, the clear icon, the chips, the tile's icon and the result's two lines. The query and its caret stand a pixel right of the page's, because the prefix's width is rounded up the same way. That is within the check, and not corrected.

**Behaviour** (`SearchBarBehaviourTest`):
- clear shows, empties and keeps focus;
- Search reaches `onSearch`;
- Tab reaches the clear icon, its brackets show and Enter clears;
- a suggestion hands its words over;
- a disabled bar clears nothing;
- the match is the first, case aside, and none when there is none;
- results and loading rows are 60 dp, suggestion rows 40 dp apart, and every chip target is at least 48 dp.

**QA.** The QA kit gained the bar, the suggestions, a result and the loading row, so the constraint, long-content and font-scale sweeps run over them. The keyboard sweep and the hover test gained the bar, a suggestion and a result.

**Reduce motion.** Every body here is a `MutantSurface` or a cocoon under the screen's clock, so the pause (D7) is theirs; nothing here animates of its own.

**Sample.** A «Search bar» section on the components page.

**Mutants.** All ten killed:
- `SearchBarParityTest`:
  - the chip advancing by its own width;
  - the label padding 16;
  - the match without its accent.
- `SearchBarBehaviourTest`:
  - suggestion rows 48 dp apart;
  - no focus handed back;
  - a case-sensitive match;
  - the clear icon with an empty query;
  - no `onSearch`;
  - no brackets;
  - the result row 64 dp.

  The last one was first aimed at the parity test, whose fixture lays the row at 60 itself, and survived there; the behaviour test kills it.
