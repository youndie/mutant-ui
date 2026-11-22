# B-168 — search: MutantSearchBar, MutantSearchSuggestions, MutantSearchResult, MutantSearchResultLoading

Group 6 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), 2026-11-22, on the desktop test runtime of this Mac.
The components' own item: [B-44](../../docs/backlog/B-44-search-bar.md) (the bar's 24 dp column, the clear icon shown
only with a query and handing focus back to the field, its brackets from the keyboard, the suggestions' 14 dp padding and
`sg1`… keys, the 60 dp result row with its first match in `textAccent`, the loading row's bars). The design: the «Search
bar» stage `l-search` of `mutant-ui Компоненты позже.dc.html` (one still state: the focused field `data-calm=".3"
data-brackets` with `ПОИСК`, `ночь`, the block caret and the clear svg; two `chip` suggestions `data-press
data-mu-toggle`; a result row, a divider and a loading row of three cocoons), the handoff's row `SearchBar` (`field` +
`chip` + `icon`, state focused), the brief's motion table (TextField `focus` → `lock-on` 200 ms; Кокон «настраивается»
в компонент того же seed). The text field inside the bar is the text-field group's.

## What was looked at

Frames in `/private/tmp/b168-search/` (scratch tests, deleted). A sheet is a frame a cell, 16 ms apart, labelled with
the clock and the event; `*-diff.txt` counts the pixels more than 16 levels apart between consecutive frames. The
stock kit's `s-flow-efir`, `s-flow-kopiya` and `d-efir` frames, which the findings below cite, moved to `before/` when
those three were recorded again after the fixes.

| Sheets | What it composes | States and transitions |
|---|---|---|
| `s-flow-{efir,kopiya,reduce,clean,stepped,d2,ru}-p*.png` | `MutantUiSample(group = Selection)`, the Search section (its two variant bars, cropped), in each header setting, density 2, Russian | focus in by mouse; typing one letter every 64 ms; a suggestion chosen by mouse; the clear icon by mouse; focus out; clearing the bar that has a result under it; a query typed, then a long one past the bar's end; Tab to the clear icon, Tab to the result row |
| `s-appear-efir-p*.png` | the same | the group switched away and back, 40 frames, then two seconds at rest |
| `d-{efir,kopiya,reduce}-p*.png` | the demo's phone (`DemoScreenFrame`), its Find tab | focus by mouse with the placeholder; `n` (five results), `i` (two), `zz` (the «NO SIGNAL» line); clear by mouse; the suggestion «rain» by mouse; a query replaced and emptied |
| `seams-{d1,d2,kopiya}-p*.png` | a bar with its suggestions, two results and two loading rows, in the browser's way of drawing (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true, `LocalMutantBodyAsImage` false) | a suggestion by mouse; loading rows, then results arriving; clear; typing; the bar removed and composed back (the pool) |
| `zone-{kit,engine}-{Efir,Kopiya}.png`, `zone.txt` | the page's bar as `SearchBarParityTest` composes it (328 dp, `ночь`, key `search`, stage `l-search`, words hidden), drawn once by its own clean zone and once by the page's | — |
| `z1.png`, `z3.png` | crops of `s-flow-efir-p1.png`, ×3 | the clear icon over the mosaic; the caret over a new letter |

Held as the design and B-44 have it, and looked at: the bar's focus calms the field and draws its brackets and block
caret; the clear icon comes with the first letter and goes with the last; the match is drawn in `textAccent` (green in
«Эфир», blue in «Копия»), case aside, and follows the query; a disabled or bare row draws it plain; the result row's tile
heats under the mouse (the list row's hover); the suggestions keep their bodies when a new set comes; the loading rows
and the result rows stand at the same heights, so nothing below moves when results arrive; the group opened again draws
every body from its first frame; reduce motion stands every body and the caret; «Чистый сигнал» takes the Tape only;
`steppedMotion` moves nothing of this group's; density 2 draws every part in proportion; Russian words fit. At rest the
per-frame diffs change on the bodies' 8 ticks a second and the Tape's band only.

A mouse press in the test window leaves the pointer where it pressed, so a row or chip that comes under it after the
press heats as hovered (`d-efir-p3.png` 3472, `seams-d1-p0.png` 780): the hover is right. In the browser's way a body
composed again with another look stands in as a plain slot for one frame (`seams-d1-p1.png` 1480): that is B-149's
stand-in, already reported (F-buttons-03).

## Findings

### F-search-01 — the clear icon is not in the bar's clean zone: the edge mosaic and flow lines run under it (local)

The engine's clean zone is the box of the slot's text, every `svg`, every `[data-mu]` and every `[data-keep]`
(`mutant-engine.js`, `textZone`), so on the page the clear icon's svg is inside it and no mosaic block, flow line or
dead pixel stands under the cross. The kit marks the prefix, the value and the caret (`MutantTextField`), but not the
trailing clear icon, so its zone ends 32 dp short of the page's: the body draws its edge mosaic under the cross, a white
block beside and under its right arm in the sample's first bar (`s-flow-efir-p1.png` 2012–2348, `z1.png`; «Копия»
`s-flow-kopiya-p3.png` 2808–2920). On the page's own bar the kit's own zone draws **570 pixels** («Эфир») and **577**
(«Копия») more than the page's, every one of them right of x = 290, under the icon (`zone.txt`,
`zone-kit-Efir.png` against `zone-engine-Efir.png`). `SearchBarParityTest` could not see it: its fixture hands every
body the engine's zone (`LocalMutantTextZone`), so the kit's own zone is never drawn there. D5: decorations never enter
the clean zone. **Local**: `components/MutantSearchBar.kt` (`ClearIcon`).

### F-search-02 — a suggestion chosen takes the focus from the field, and in the demo leaves it nowhere (local)

A chip takes focus when pressed outside touch mode (a mouse on the desktop and in the browser, Enter from the keyboard).
Choosing a suggestion hands its words to the query, and the field loses its focus: the brackets, the calm and the caret
go in that frame (`s-flow-efir-p2.png` 2552; `s-flow-reduce-p2.png` 2552), so a user who picked «night air» to go on
typing types into a chip. In the demo the suggestions leave the screen as the query fills, the focused chip with them,
and **nothing** is focused afterwards (`d-efir-p2.png` 3152; the scratch test read zero focused nodes after the choice in
all three themes, against the field before it); the next Tab starts again at the top of the screen. B-44 decided this
very case for the clear icon («Pressed, it had taken focus and left with the query, so the field lost it»): it hands
focus back to the field. The suggestions, the other control in the bar that writes the query, do not. **Local**:
`components/MutantSearchBar.kt` (`SuggestionsBody`). On a touch screen the chip takes no focus and the field keeps it;
not seen here, no phone.

### F-search-03 — the caret placed by the last layout: withdrawn, the text-field group's

Seen through the bar, all in `MutantTextField.kt`'s `Caret`: the block over a just-typed letter (`s-flow-efir-p1.png`
2076–2268, `z3.png`; three letters at once, `seams-d1-p1.png` 1352), the 2 dp bar inside a text no longer there on the
frame the clear icon empties it (`seams-d1-p1.png` 1192), the block over a placeholder's first letter
(`d-efir-p0.png` 2024–2184) and over the last letter of a query past the bar's end (`s-flow-efir-p6.png` 4352–4560).
The text-field group has changed `TextFieldBody` on its branch (the line scrolls itself and the caret is placed in the
same measure; a bar, not the block, before a placeholder), not yet on `main`; this worktree still draws the old caret.
Not reported again; the bar's frames should be looked at once both branches are in.

### F-search-04 — the result's match follows the query late, and under reduce motion until something else redraws (reported: shared, `MutantText.kt`; a new face of F-buttons-02)

`MutantSearchResult` hands the row a new `AnnotatedString` whose spans differ and whose text and size do not.
`MutantText` measures it again, but its `drawBehind` reads the kept layout and nothing invalidates the draw, so the old
match stays on screen until another redraw of the row: **1 frame** after the clear icon (`s-flow-efir-p5.png` 3760),
**4 frames, 64 ms** after a query stops matching (`s-flow-efir-p6.png` 4336–4384), and under reduce motion, where the
clock draws nothing, **15 frames, 240 ms**, until the Tab's focus change happened to redraw it (`s-flow-reduce-p7.png`
4336–4576); on a still screen it stays. F-buttons-02 is the same cause for a colour-only change.

### F-search-05 — results arrive as a cut from the cocoons (reported: a look the design does not settle; the card-cocoon group)

The loading rows give way to the result rows between two frames (`seams-d1-p0.png` 876 → 972; nothing moves, the heights
agree). The brief says the cocoon «настраивается» into the component of the same seed, and its motion table has `tune-in`
(mosaic 16 → 8 → 4 → clean, 320 ms) for a card's appearance; the kit has no tune-in anywhere, and the search's loading
row is a separate composable from the row it stands for. Whether a result should tune in from its cocoon is the owner's,
and kit-wide (Card, Cocoon).

### F-search-06 — the bar drops its focus look when focus moves to its own clear icon (reported: the owner; a new face of F-text-field-08)

Tab from the field to the clear icon moves focus within the bar, yet the bar drops its focused look in that frame: the
drips grow back from the 0.3 calm and the field's brackets give way to the icon's (`s-flow-efir-p7.png` 4642, 1119
pixels apart from the frame before). The page's focused bar is one state for the whole line (`data-calm=".3"
data-brackets` on the slot that holds the svg). The cut itself is F-text-field-08 (focus in and out swap the calm in one
frame, `s-flow-efir-p0.png` 1692, `s-flow-efir-p4.png` 3380); new here is that it happens while focus stays inside the
bar. Whether the bar keeps its look while focus is anywhere in it is the owner's.

### F-search-07 — several loading rows flicker alike unless keyed, and the KDoc does not say so (local)

`MutantSearchResultLoading` keys its cocoons `[key]0`…`[key]2` with `key = ""` by default, the page's. A screen of three
loading rows — the usual way to show results coming — draws three identical mosaics changing in step: a cocoon's blocks
and its ticks are seeded by its key and stage alone (`MutantCocoon`: «Several on one screen each take their own key, or
they flicker alike»). The default is the page's (B-44) and stays; the KDoc says nothing of it. **Local**:
`components/MutantSearchBar.kt`, KDoc only.

## Fixes

### F-search-01 — fixed in `f9cb893`

- **Cause and fix.** `ClearIcon`'s box now carries `mutantCleanZone()`, as the icon button's and the button's icons do:
  the field's zone runs from the prefix to the cross, as the engine's from the text to the svg. It is marked only while
  the icon shows (with a query), as the icon itself.
- **Test.** `SearchBarParityTest`, «the bar's own clean zone takes in the clear icon, as the page's does»: the page's bar
  (328 dp, `ночь`, words hidden, frozen at the page's time) drawn by the zone the kit measures itself and by the zone
  the engine measured (the parity fixture's `LocalMutantTextZone`, withheld for the field only): 0 of 17 056 pixels
  apart in each theme, where the stock kit read 570 and 577.
- **Mutant**, run on the committed code, the `<failure>` read from the JUnit XML, the file restored: the marker dropped
  from `ClearIcon` → killed («Efir: 17056 pixels of the bar, 570 apart / Kopiya: … 577 apart»).
- **After.** `s-flow-efir-p1.png`, `s-flow-efir-p2.png`, `s-flow-kopiya-p*.png` recorded again (the stock ones in
  `before/`): the cross stands on the plain fill, the mosaic block that stood beside and under its right arm is gone;
  the bar without a query keeps its mosaic at the edge, as before. The zone now grows by the icon on the frame the
  first letter is typed and shrinks with the last one deleted: the same frame in which the field's zone already changes
  (F-text-field-07), so no frame of change is added. No golden, reference or parity cell moved:
  `SearchBarParityTest`'s page cell passes unchanged (its fixture hands in the engine's zone).

### F-search-02 — fixed in `f9a1b0c`

- **Cause and fix.** A chip takes focus when pressed outside touch mode, and `SuggestionsBody` only handed its words
  over. `SearchBarBody` now provides its field's `FocusRequester` to its content through a private local, and a
  suggestion taken under a bar hands focus back to it after `onSuggestion`, as the clear icon does (B-44). Suggestions
  outside a bar are untouched. The KDoc of `MutantSearchSuggestions` says so.
- **Test.** `SearchBarBehaviourTest`, «a suggestion taken by the mouse or the keyboard hands focus back to the field»: a
  bar whose suggestions leave as the query fills (the demo's way), a suggestion clicked by the mouse — the query is
  filled, the suggestions are gone and the field is focused; then suggestions that stay (the sample's way), one focused
  and taken by Enter — the field is focused.
- **Mutant**: the `requestFocus` dropped from the suggestion's click → killed («Failed to assert the following:
  (Focused = 'true')», the field with `EditableText = 'ночной эфир'`, `Focused = 'false'`).
- **After.** `s-flow-efir-p2.png` 2552–2792: after «night air» the field keeps its brackets, calm and caret (the caret's
  first frame stands inside the new words: F-search-03); `d-efir-p2.png` 3152–3200: after «rain» the demo's field is
  focused, the scratch test read it as the one focused node in all three themes. On a touch screen the field already
  kept its focus; there it is asked for again and stays.

### F-search-07 — fixed in `195c8d7`

KDoc only: `MutantSearchResultLoading` says that several rows on one screen each take their own key. No behaviour, so no
test and no mutant.

## Gate

`./gradlew :mutant-core:desktopTest :sample:desktopTest :mutant-core:ktlintCheck :sample:ktlintCheck --continue` on
`195c8d7`, this Mac: exit 0; the JUnit XML reads `mutant-core` 427 tests, 0 failed, 0 skipped, and `sample` 22 tests,
0 failed, 0 skipped; both lint checks pass. The public API did not change (a private local and KDoc), so no ABI dump.
The text-field group's change to `TextFieldBody` is not on this branch: the bar passes it `trailing` and
`focusRequester` as before, and should be looked at once both are in.

## Not verified

- A touch screen: no phone was used. That a touch on a suggestion leaves the field focused, and that the soft keyboard
  stays up after F-search-02's request, is read from Compose's touch mode, not seen.
- The browser itself: the browser's way was drawn on the desktop through the surface's seams, not in Chrome or on the
  Pixel; nothing in this group's fixes touches the shader.
- The seams run at density 2 timed out in its teardown after its frames were written (`seams-d2-p*.png`, looked at);
  the density-2 sample flow (`s-flow-d2-p*.png`) ran whole.
