---
id: B-196
title: "The pickers: today, disabled days, six rows, the clock's accent"
status: done
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-196 — The pickers: today, disabled days, six rows, the clock's accent

The pickers have no today mark and no disabled days (F-pickers-08); the calendar's height changes with the month, 4–6 rows, and its body regenerates (F-pickers-06); the time picker's acid stays on the hours while Up and Down set the focused minutes (F-pickers-07). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 21 and 32 ([решение]) and 40 ([ошибка], the clock).

- **The decision and its reason.** Today wears four corner brackets in the `osd` colour around its number, as a focused field does. A disabled day is drawn in `dis`, without a body, and cannot be pressed. The calendar always has six rows, so its body never changes height with the month. The clock's accent follows the field being edited.
- Typed digits stay out: the designer deferred them.

- AC:
  - today shows four brackets in `osd`; a disabled day draws no body, ignores a click and Enter, and its semantics say it is disabled
  - every month of 2026 lays the calendar out at the same height
  - editing the minutes by Up and Down moves the accent to the minutes
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDatePicker.kt`

## Result

### 2026-11-23

- **Looked at first**: a throwaway contact sheet (`/private/tmp/b196/before-*.png`, `after-*.png`, not in the repository) of
  November 2026, December 2026 and February 2027 side by side and the time picker after Up on the focused minutes, in
  «Эфир» and «Копия» at densities 1 and 2, under reduce motion. Before: the three calendars stood 307, 271 and 271 dp tall
  and the hours kept the acid while the minutes had the brackets (F-pickers-06, -07). After: all three 307 dp, the weeks a
  month does not reach blank; today bracketed, the refused days grey; the minutes acid.
- **Today** (answer 21): `today: MutantDate?`, in the month shown, wears the focused field's four corner strokes
  (`drawCornerBrackets`, 10 dp arms, 2 dp wide) in `textOsd`, not in `focus`, which «Копия» tells apart. Designed here
  (D15), as the design draws no today: the strokes stand **4 dp inside the 36 dp cell**, a 28 dp square around the number,
  so the keyboard's focus brackets (1 dp in) still read around them on the same day. Over the drop (today chosen) they are
  drawn on it, amber on acid in «Эфир», rust on acid in «Копия»; looked at, both read.
- **Disabled days** (answer 21): `selectableDates: MutantSelectableDates` (a `fun interface`, Material's
  `SelectableDates.isSelectableDate` over year, month and day, the kit having no date type; `MutantSelectableDates.All` by
  default). A refused day is drawn in `textDisabled`, never wears the drop even when it is `selectedDay` (the drop is the
  only body a day has), has no hand cursor, and its `selectable` is disabled: no click, no focus, so no Enter, and its
  semantics say disabled. Designed here: the arrow keys step over refused days to the next open one in their direction,
  as a refused day takes no focus; a whole refused row or column ends the step where the month ends.
- **Six rows** (answer 32): the grid always lays out six weeks (`DAY_ROWS`), the most a month reaches; the calendar is
  307 dp tall in every month, so its bar body is generated once and stays. B-178's `CalendarKeepsItsSize` is untouched
  and `CalendarShortOfRoomTest` still holds (its November already had six weeks).
- **The clock's accent** (answer 40, its clock part): Up and Down on a time field make it the one being set, the acid
  pouring to it as a press would, then step its value. The snackbar's and the drawer's parts of answer 40 are other items.
- **API**: `MutantDatePicker(…, modifier, today = null, selectableDates = MutantSelectableDates.All, monthNames, …)`, new
  public `MutantDate` (data class) and `MutantSelectableDates`. Source-compatible for calls that name the arguments after
  `modifier` (the sample's); binary-incompatible for compiled callers, as every added parameter is. ABI dumps updated with
  `./gradlew updateKotlinAbi` (desktop, android, klib) and reviewed: the two new types and the new signature.
- **Tests**, `PickersTodayDisabledSixRowsTest`, pixels counted over areas, every threshold well clear of what is drawn:
  - today: the amber in each of the 23rd's four 10 dp corner squares (28 px at density 1, 112 at 2; asked ≥ 12 · d²),
    none when today is in another month, none of the focus colour in «Копия», none at the 24th; both palettes, d 1 and 2;
  - disabled: a refused selected 13th shows no acid (0 px; < 1/50 of the cell asked), its number grey (≥ 42 px at d 1,
    ≥ 204 at 2; asked ≥ 6 · d²) with none of the text colour (the 14th's 33–252 px the control), `assertIsNotEnabled`,
    a click picks nothing, it offers no `RequestFocus`, Right from the 12th lands on the 14th and Enter there picks the
    14th; the open control shows the drop (> 1/3 of the cell), takes the click, and Right lands on the 13th;
  - six rows: every month of 2026 lays the calendar out at 307 dp (before: 271 in nine months, 307 in March, August and
    November);
  - the clock: Up on the focused minutes turns 30 into 31 and moves `Selected` and the acid (3 859–3 969 of 5 472 px) to
    them, none left on the hours; Down on the focused hours takes it back.
  - Before the change, the six-row and clock tests fail (`271.0` months; `(true, false)` after Up on the minutes); the
    today and disabled tests do not compile there, the API being new, and are held by their mutants below.
- **Mutants**, after `bd6260a`, each against `PickersTodayDisabledSixRowsTest`, read from its JUnit XML, restored with
  `git checkout`, `git status` clean after each:
  - `textOsd` → `focus` for today's brackets: killed by the today test («Копия», `[0, 0, 0, 0]` amber in the corners);
  - `enabled = enabled` → `true` on a day's `selectable`: killed by the disabled test (`is not enabled`);
  - `&& open[selectedDay - 1]` dropped from `chosen`: killed by the disabled test (736 px of acid in the refused cell);
  - `DAY_ROWS` → the month's own `(lead + days + 6) / 7`: killed by the six-row test (271 dp months);
  - `onActivate()` dropped from the time field's key handler: killed by the clock test (`(true, false)` after Up).
- **Goldens and parity cells**: none moved. The parity cells and the design goldens draw November 2026, which has six
  weeks, no today and no refused day; `:mutant-core:desktopTest` re-ran them all green.
- **Gate**, this Mac: `./gradlew check --continue` BUILD SUCCESSFUL (`:mutant-core:desktopTest` 546 tests and
  `:sample:desktopTest` 39, 0 failed, read from fresh JUnit XML; `:mutant-core:iosSimulatorArm64Test` 26; ktlint, lint,
  `checkKotlinAbi` after the update); `./gradlew :mutant-core:compileKotlinIosArm64` green; `make check`,
  `make docs-against BASE=origin/main` clean.
- **Not verified**: a phone and a real browser (none in this pass). The sample shows neither today nor a refused day: the
  item's scope is the component, and a sample showing them is the owner's call.
