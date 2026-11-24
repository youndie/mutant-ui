---
id: B-205
title: "Every layout mirrored for right to left"
status: done
priority: P2
size: L
stage: stage-6-later
blocked_by: []
---

# B-205 — Every layout mirrored for right to left

B-65 mirrored the tabs and the navigation bar only: the segmented button does not mirror and its arrows are not mapped, the tabs' echo stays right of the bar, the date picker's month arrows keep their left-to-right look; a left-to-right title in a right-to-left layout is cut at its end with the ellipsis drawn at its start (F-segmented-06, F-app-bar-tabs-05, F-pickers-10, F-data-table-06). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 37 ([решение]; the ellipsis [ошибка]).

- **The decision and its reason.** Every component's layout mirrors: the segments, the tabs' echo (its tail points back along the reading direction), the month arrows, the switch's thumb, the slider, the progress, the drawer's side. What does not mirror: the slime's shape and drips, the highlight (the light is always top-left), the offset shadow (4, 4), on-screen digits and time, the Tape, the reel. A left-to-right title's ellipsis follows the text's own direction, at its end.

- AC:
  - a sweep over every public component under `LayoutDirection.Rtl` finds each layout mirrored and the listed parts unmirrored, by `positionInRoot` and pixels
  - arrow keys move the segmented button and the pickers in reading order
  - a left-to-right title in a right-to-left bar ends in its ellipsis on the right
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSegmentedButton.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTabs.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDatePicker.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSwitch.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSlider.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantProgress.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationDrawer.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTopAppBar.kt`

## Result, 2026-11-24

**Decided** (the designer's answer 37, recorded in research D24). Right to left every component's layout mirrors, and
what is drawn rather than laid out does not.

- **The segmented button** (F-segmented-06): segment `i` of `n` stands in column `n − 1 − i` from the left, so the
  first is at the right; the window, the venom's slots (its run from the old window to the new), the press point and
  the words' clean zones go with it, through `SegmentGeometry.column`. Left and Right choose the segment on that side
  (in reading order, Left is the next one), Down and Up the next and the one before in either direction. Each word's
  line box stands at its segment's start, so a reader's box is mirrored too. The body is drawn as it is left to right.
- **The tabs' echo** (F-app-bar-tabs-05): it rests past the bar toward the reading direction, to its left right to
  left; it trails behind a run toward that side and crosses back as the bar lands; a run the other way keeps it where
  it rests.
- **The month arrows** (F-pickers-10): the pair is mirrored with the row: the back arrow at the right of the forward
  one, pointing right, the forward one pointing left, 18 dp between the glyphs as left to right (they stood 30 dp apart,
  as each target was placed unmirrored around a mirrored glyph), each 48 dp target reaching outward.
- **The switch's run**: the thumb already stood mirrored (an `offset` in the row's direction), but its slide came from
  the left, 24 dp off the track; it now slides in from where it stood.
- **Words in their own direction** (the ellipsis, F-data-table-06, [ошибка] in the answer): in a right-to-left layout
  `MutantText` lays a word out by its first strong letter (`TextDirection.Content`) and aligns it where the layout's start
  puts it (`TextAlign.Start` becomes `Right`, `End` becomes `Left`), so a left-to-right title cut to its box ends in its
  ellipsis at its own end, the right. Skia's paragraph had taken the layout's direction and drawn the ellipsis at the
  word's start, `…Эфир, ночь на вто`. A word with no strong letter («12:04», «+3», «042%») still takes the layout's
  direction, as before; a style naming its direction keeps it; left to right nothing changes.
- **Not mirrored, now held**: the time picker's row (it read «30 : 21»; the hours stand left of the minutes in either
  direction, the group at the slot's start, through a left-to-right row inside the consumer's box); the illustrations (the
  TV's knobs and drips and the test card's bars were mirrored with their parts' offsets; a picture now lays its parts from
  the left). Already unmirrored, now held by a test: the bodies' shape, drips, marks, highlight and shadow, the reel, the
  Tape.
- **Already mirrored on `main`**, held by the sweep: the tabs' places and the navigation bar (B-65), the drawer's side and
  drag (B-183), the data table's scroll (B-202), the top bar's `TopAppBarRow` (B-187, it places with `placeRelative`), the
  progress (B-65), the slider with its glide (B-182 places the handle with `placeRelative`, so the glide runs leftward),
  the calendar's days (B-168), every row, list item, snackbar, dialog and sheet.
- Rejected: mirroring a body with the layout (`scale(-1, 1)` as the progress does for its bands), which would move the
  light to the top right and the shadow to (−4, 4); a time picker mirrored with its row.

**The ACs as held.** AC 1's sweep is the QA catalogue's every public component (`qa/QaKit.kt`'s `subjects`, 52 cases)
plus an open dialog and an open sheet; the popups (an open `MutantMenu`, `MutantTooltipBox`'s plate in its popup) and the
hosts with nothing of their own to lay out (`MutantScreen`, `MutantAppear`, `MutantKeyframeLoss`, `MutantTheme`) are not in
it — a popup is a second root the sweep's `onRoot` does not read; the menu's and the tooltip's placement right to left
were settled by their own items (`MutantMenu`'s and `MutantTooltip`'s position providers read the direction). AC 2's
pickers: the calendar's days already moved in reading order since B-168 (`CalendarRightToLeftTest`); the time picker
takes Up and Down only, and stays left to right, so no key of it moves sideways.

**Tests** (`mutant-core/src/desktopTest`, density 1, «Эфир»; every pixel reading is over an area or a run, with margins
far from its threshold):

| AC | Test |
|---|---|
| a sweep finds each layout mirrored and the listed parts unmirrored, by `positionInRoot` and pixels | `qa/RightToLeftSweepTest` «right to left, every component's words, icons and targets stand mirrored»: every text and content-description node of 52 cases (the catalogue's but the time picker and the illustration, with the dialog and the sheet) in a 360 dp stage, left within 2 px of `360 − right` left to right (169 nodes), the TV's words inside the empty state where they were; «right to left, the time and the pictures stand as they do left to right» (the time picker's digits and labels, the illustration's words, within 1 px); «right to left, the slime, its drips, highlight and shadow, the reel and the pictures are not mirrored»: an empty card, the FAB, the icon button, the reel, the Tape over a pattern, the test card and the time picker differ in under 1 % of their inked pixels between the directions and (but the reel, near its own mirror image) in over 3 % from the mirror image |
| … the segments, the echo, the month arrows, the switch, the slider | `components/RightToLeftPartsTest` «… the segmented button's window stands over the first segment at the right» (the lime's middle and «SP»'s box at 257 of 312, 55 left to right); «… the tabs' echo rests left of the bar» and «… a run back toward the first tab trails its echo on the left alone»; «… the month arrows are mirrored and point their ways» (glyphs 15–19 dp apart, back right of forward, each triangle's ink leaning to its flat side); «… the switch's thumb slides in from the right» (48 ms in, ≥ 4 px right of its place, left to right ≥ 4 px left); «… the slider's handle glides toward the left»; `SegmentedBehaviourTest` «right to left, every word stands where the reversed options stand left to right» (B-168's «… where it does left to right» turned over) |
| arrow keys in reading order | `RightToLeftPartsTest` «right to left, the arrow keys move the segmented button in reading order» (Left 0 → 1 → 2, stops at 2, Right → 1, Up → 0, Down → 1); the calendar's `CalendarRightToLeftTest` |
| a left-to-right title ends in its ellipsis on the right | `RightToLeftPartsTest` «a left-to-right title in a right-to-left bar ends in its ellipsis on the right»: the title's box mirrored, its line's last 11 columns of ink under a third of the line's height (the dots), its first 11 the capital's |
| each new test fails before; goldens and parity cells unchanged | run on `main`'s code (the changed files from `origin/main`, the tests from the branch): every new test failed but the slider's (below); `./gradlew check` green with every golden and parity test untouched |

The slider's test passed on `main`: B-182's handle is placed with `placeRelative`, so its glide already ran leftward. It is
kept as the guard the brief asks for ("mirror its direction along with it"), and the mutant below shows it can fail.

**Mutants**, after the commit, each run against its aimed classes, the failure read from the JUnit XML, the file copied
back and `git status` clean for it:

| Mutant | Killed by |
|---|---|
| `SegmentGeometry.column(i) = i` | «… window stands over the first segment at the right», «… reversed options …», «… words, icons and targets stand mirrored» |
| `SegmentGeometry.side = 1` | «… arrow keys move the segmented button in reading order» |
| the echo's `ahead = 1` | «… echo rests left of the bar», «… trails its echo on the left alone» |
| `pointsLeft = back` | «… month arrows are mirrored and point their ways» (the glyphs 20 apart) |
| the arrow's target by `place` | «… month arrows …», «… words, icons and targets stand mirrored» |
| the switch's `side = 1` | «… the switch's thumb slides in from the right» (−9.5 px) |
| the time picker's row in the layout's direction | «… the time and the pictures …», «… not mirrored» |
| the illustration in the layout's direction | «… not mirrored» (44 % apart), and the sweep (the TV's words) |
| `MutantText` without `TextDirection.Content` | «a left-to-right title … ends in its ellipsis on the right» |
| the slider's handle by `place` | «… the slider's handle glides toward the left» |

**Goldens and parity cells.** None moved: every case is right to left, which no golden or parity cell draws, and the
time picker's and the illustration's wrappers lay out left to right exactly as before (`./gradlew check` green).

**Frames looked at** (throwaway contact sheets, deleted): every catalogue component left to right, right to left and the
right-to-left frame mirrored back, in «Эфир» and «Копия» at density 1 and «Эфир» at density 2, under reduce motion, before
and after; the long title in both directions.

## Findings

- **A text field's left-to-right value stands at the field's left right to left**, while its prefix and placeholder
  mirror: the value line is a `BasicTextField` laid out in the value's own direction (`TextDirection.Content`, B-58), and
  its block caret, scroll and clip (B-186) are computed from the line's left. Aligning it to the layout's start would
  rework that arithmetic; the platforms' own fields align a value by its direction. Left as it is; it is one line for the
  designer if it should stand by the prefix.
- **Icons are not mirrored.** The answer lists layouts, not glyphs: the kit's `MutantIcons.Back` still points left in a
  right-to-left bar, and the menu's chosen-row mark, the window's play triangle, still points right (a play symbol). Not
  changed here.
- **A right-to-left word in a left-to-right layout** still takes the layout's direction, its ellipsis at its start: the
  answer names the left-to-right title in a right-to-left layout, and leaving left to right untouched keeps every golden.
- **A centred label differs by a sub-pixel between the directions** (a button's word: 4.6 % of its inked pixels apart, all
  on glyph edges, its box within 2 px): Skia sets the right-to-left line from the other side of a fractional width. Not a
  layout, not chased.

**Not verified.** No phone and no browser: Android, iOS and wasm only as `./gradlew check` and
`:mutant-core:compileKotlinIosArm64` build them. The sample was not looked at right to left in a window.
