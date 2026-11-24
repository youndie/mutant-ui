---
id: B-194
title: "The cassette label is a translated string that fits"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-194 — The cassette label is a translated string that fits

The cassette illustration's label is «БЕЗ ПОДПИСИ» in every language and on every tape (F-screens-04). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 19 ([решение]).

- **The decision and its reason.** The label is a string a consumer translates (through the theme's strings or a parameter). A translation wider than 164 dp at size 20 drops to 16; if it still does not fit, the label carries no text but three lines.

- AC:
  - a short translation draws at 20; one wider than 164 dp at 20 draws at 16; one too wide at 16 draws three lines and no glyph
  - the default draws as today: its goldens unchanged
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/illustration/MutantIllustration.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/theme/MutantTheme.kt`

## Result, 2026-11-24

`MutantIllustration.kt` and `MutantEmptyState.kt`, and the sample's three cassettes. On
`feat/b-194-the-cassette-label-translated`, on this Mac, desktop JVM, `runComposeUiTest`.

- **The word is a parameter** (answer 19: «строка ресурсов, переводится»). `MutantIllustration(kind, modifier,
  cassetteLabel = "БЕЗ ПОДПИСИ")`, and `MutantEmptyState(…, text, cassetteLabel, action)` hands it to its art; the
  other five kinds ignore it. The theme carries no strings: the kit's words a consumer translates are parameters with
  the design's Russian as the default (`MutantSearchBar`'s `prefix` and `clearLabel`, `MutantDatePicker`'s
  `monthNames`, `MutantPullToRefresh`'s labels), and this word follows them. Rejected: a strings object in the theme,
  the kit's first, for one word.
- **The fit** (answer 19). The word is measured as the label draws it, Handjet tracked 2 dp, in dp at the screen's
  density and no font scale (the art's words ignore the font scale, B-61): at 20 if it is 164 dp or narrower (the
  180 dp label less 8 dp a side), else at 16 if that fits, else no word. Widths measured here: «БЕЗ ПОДПИСИ» 108 at 20,
  «UNTITLED» 72, «NO LABEL ON THIS TAPE» 183 at 20 and 155 at 16, «THIS TAPE HAS NO LABEL AT ALL» 211 at 16.
- **Three lines** where no word fits: three rules of the page's ink (`#05060c`), 148 dp long, 2 dp thick, 8 dp from one
  to the next, centred where the word stands, keeping the clean zone as the word does and left out with the text in a
  parity fixture. A blank word draws them too: a label with nothing written on it. The answer names the three lines and
  nothing more; their length, weight, pitch and the blank case are designed here (D15), to be revised in the owner's
  review round.
- **The sample** passes its language's word, `SampleStrings.cassetteLabel`: «UNTITLED» in English (the sample opens in
  English, research D11), the design's «БЕЗ ПОДПИСИ» in Russian, on the Recording screen, the Library's empty state and
  the components page's empty state. Every tape still shows the same word, as the answer asks only for a translation;
  a label per tape (F-screens-04's second half) is not in this item.
- **Public API**: both functions gain a `String` parameter (`mutant-core/api/*`, recorded by `updateKotlinAbi`). The
  JVM and klib signatures change, so code compiled against the old ones must recompile; source with named or
  positional arguments up to `text` and a trailing `action` compiles as before.

**Tests** (`illustration/CassetteLabelTest`), each failing on the code before, run with both files at `origin/main`
and the new parameter added and ignored:

| AC | Test | Before |
|---|---|---|
| a short translation draws at 20; one wider than 164 dp at 20 draws at 16 | «a word that fits draws at 20 and one wider than 164 dp at 20 draws at 16»: the size from the word's text layout; the default at 20, «UNTITLED» at 20, «NO LABEL ON THIS TAPE» at 16, both again at density 2 with font scale 2, and «UNTITLED» through `MutantEmptyState` | no «UNTITLED» on the cassette |
| one too wide at 16 draws three lines and no glyph | «a word too wide at 16 draws three rules and no glyph»: no text node; inside the label, rows 20–46 by columns 40–176, the rows nine in ten dark are exactly 24–25, 32–33, 40–41, and fewer than 20 dark pixels lie off them (none measured); the same for a blank word; control: the default word has no such row and over 200 dark pixels | the long word is drawn, no rule row |
| the default draws as today | the goldens below, unchanged | — |

The pixel test reads whole rows of the label, nine in ten of 137 pixels, and counts glyph pixels against a margin of 20
where a word gives hundreds: an anti-aliased edge on another rasteriser moves neither.

**Mutants**, after the commit (`236c4c9d`), one line changed, the aimed test named among the failures in the JUnit XML,
the file copied back, `git status` clean:

| Mutant | Killed by |
|---|---|
| the width in pixels, not dp (`/ d.density` removed) | «a word that fits …»: at density 2 «NO LABEL ON THIS TAPE» measured 310 and drew no word |
| the room 184 dp, not 164 | «a word that fits …»: «NO LABEL ON THIS TAPE» at 20, not 16 |
| two rules, not three (`repeat(2)`) | «a word too wide …»: rule rows 24–25, 32–33 only |

**Goldens and parity cells.** None moved: the default word takes the same `label("БЕЗ ПОДПИСИ", 20, INK, 2.0)` as
before. `Illustration_Cassette_*`, `Illustration_CassetteBody_*`, `MutantEmptyState_Cassette*`, `MutantEmptyState_Stage_*`,
`Demo_Library_*` and `Demo_Recording_*` pass unchanged in `./gradlew check`; no departure, no registry row. The sample has
no goldens; its English cassette now reads «UNTITLED».

**Frames looked at** (throwaway sheets, deleted; frames in `/private/tmp/b194/`): the cassette before, and with
«UNTITLED», «NO LABEL ON THIS TAPE» and «THIS TAPE HAS NO LABEL AT ALL», in «Эфир» and «Копия», at density 1 and 2. The
word at 16 sits centred, 155 of its 164 dp; the three rules stand centred, inside the label's outline,
clear of its edge. The first build left the rules at the label's top-left corner (the label passes its size as
constraints, not a box to align in); seen on the sheet and fixed before the commit.

**Not verified.** No phone and no browser: Android, iOS and wasm were compiled and run only as `./gradlew check` runs
them, and `compileKotlinIosArm64` compiled. The sample in Russian and English was not opened in a window; its words are
held by the compile only. A word with a line break is drawn as given (one label, two lines over a 38 dp label): the
KDoc says the word is one line, and no test holds it.
