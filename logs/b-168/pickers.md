# B-168 — pickers: `MutantDatePicker`, `MutantTimePicker`

Group 22 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-pickers` from `73dcdd9`,
2026-11-22, on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand). Both live in
`mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDatePicker.kt`; their numbers were decided
in [B-45](../../docs/backlog/B-45-date-and-time-pickers.md), [B-93](../../docs/backlog/B-93-the-calendar-slime-moves-to-the-new-day.md)
and [B-150](../../docs/backlog/B-150-the-date-picker-keeps-its-width.md), the 36 dp cell in research D19. The design
draws both still (the later page's `l-date`: the calendar `bar` 284 dp, the chosen day a `pill` r 18 with its number
`#05060c` 700; two `field` bodies 76 × 72, the hours `data-fill="green"`, «часы выбраны, минуты ждут»); every motion
is the kit's (D15): the drop's run (B-93, the navigation bar's slide), the time field's pour.

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-pickers/` (not in the repository).

- **The pickers as the sample composes them** (`core/`): the calendar of November 2026 and the time picker 21:30, each
  beside a bare twin (`DatePickerBody`/`TimePickerBody` with `hidden = true`, the same keys and clock) so that what
  lies under each number is read from the twin, in nine looks: «Эфир», «Копия», reduce motion, «Чистый сигнал»,
  `steppedMotion`, density 2 (no twin: it does not fit the window), the browser's way of drawing
  (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true,
  `LocalMutantBodyAsImage` false) in both palettes, and right to left. Every frame captured into a sheet, with a text
  file per sheet: the pixels changed from the frame before, the drop's lime (middle and box, px), and per day the
  median contrast of its ink against the twin's pixel under it, the ink on a light and on a dark ground read apart
  (a number half on the drop reads by its worse half):
  - appear and a mouse click from the 5th to the 18th, a run down two rows and across one (`date-pick-*`);
  - clicks on the 25th, then the 3rd 128 ms later, then the 19th 96 ms after that (`date-fast-*`);
  - the arrows: next, previous, previous (`date-month-*`); no selection and back, then the 31st of a 30-day month
    (`date-none-*`);
  - the keyboard: focus on the 13th, Right, Down, Enter, Left, Space (`date-keys-*`);
  - the time picker: a click on the minutes, 512 ms later on the hours (`time-switch-*`); three clicks 64 and 48 ms
    apart (`time-switchfast-*`); focus on the hours, Up three times (23 → 0 wraps), Down, focus on the minutes, Up
    (`time-step-*`), with each field's size per frame.
- **The sample itself** (`sample/`): `MutantUiSample(SampleSettings(group = Selection))` in «Эфир» and «Копия», the
  Date picker section scrolled into view and the 20th clicked (`page-date-*.png`), the Time picker section and its
  minutes clicked (`page-time-*.png`).

The fixes' frames are in `after/`, written by the same test with each fix in place; its files also carry `bad=`, per
number the share of its ink that reads under 1.5:1 against what lies under it (a stricter reading than the median).
The `core/date-pick-*` and `core/date-fast-*` files were rewritten on the code before the fixes with that column added.

## Findings

### F-pickers-01 — the days' numbers turn by a half-cell rule, not under the drop: light on lime, black on the bar — local

- **What.** B-93 turned a number dark "while the drop's middle stands within half a cell of it". The drop is drawn
  stretched along its run (up to 1.4 times, `slid`) and its middle is half a cell away for a long part of a diagonal
  run, so the rule and the drawing disagree: the 18th stands white on the arriving lime, the 12th it crosses white
  on lime, the 5th black on the dark bar once the drop has slid half off it.
- **Numbers** (`core/date-pick-Efir.txt`; at rest every number 8.5:1 or better): the 12th 1.46:1 at 236 ms, the 18th
  1.06:1 at 300 ms; in `core/date-fast-Efir.txt` the 18th 1.04:1 and the 10th 1.06:1, nine frames under 3:1 over three
  runs; under `steppedMotion` the 12th 1.88:1 for five frames (`core/date-pick-Stepped.txt`). The median-of-pixels
  reading undercounts it: the sheets show a number half pale on the lime on nearly every frame of a run.
- **Frames.** `core/date-pick-Efir.png` 172–316 ms (the 5th, the 11th and 12th, the 18th); `core/date-fast-Efir.png`;
  the sample's `sample/page-date-Efir.png` 96–160 ms (the 13th and the 20th). «Копия» reads throughout (dark words on
  paper and on lime alike).
- **The decision.** B-93's AC «the number stays readable on every frame»; the kit's rule (`cmp-ui-component` §4,
  F-nav-bar-01): a word's colour follows the fill as drawn.
- **Fixed** (`0c55678`). Each number is drawn twice over the same place: light and regular outside the drop, the
  pill's black and bold inside it, the drop read as the body slides it (`DropPlace`: its run home `1 − (1 − p)²`, its
  stretch along the run, half its outline inside its box, on every frame of the screen's clock while it runs). A number
  wholly on one side is drawn as it is, unclipped, so the resting calendar draws as before: `DatePickerParityTest`, the
  behaviour tests and the QA sweeps unchanged. The black copy is hidden from a reader and never sizes the cell.
  `CalendarNumbersOnDropTest` «every day's number reads on what is drawn under it through the drop's runs»: «Эфир», a
  bare twin, the run 5 → 18 and three picks into running runs, every frame, with and without `steppedMotion`: no
  number with more than 25 % of its ink under 1.5:1 against what lies under it. It failed on the code before («stepped
  false, frame 3: 31 % of the 12th unreadable»). Mutant: the drop's oval read without its run (`from.cols * 0.0`),
  killed — «frame 0: 100 % of the 5th unreadable».
- **After** (`after/date-pick-*.txt`, `after/date-fast-*.txt`, the share of a number's ink under 1.5:1 as `bad=`):
  before, up to 100 % of a number (the 12th, the 11th, the 19th in `core/date-fast-Efir.txt`) and 66 % of the 18th in
  `core/date-pick-Efir.txt`; after, at most 19 %, the pixels along the drop's edge where the slime's wobble leaves the
  oval (`after/date-pick-Efir.png`, `after/date-fast-Efir.png`: the black follows the drop's edge across each
  number). The same in every look with a twin; «Копия» and reduce motion none (density 2 has no twin: looked at, not counted).

### F-pickers-02 — a pick during the drop's run leaps it to the day picked before — local

- **What.** A new pick while the drop runs starts the new run from the cell picked before, not from where the drop
  stands: it jumps there in one frame and runs back. Other groups' brief named it (F-nav-bar-02 in the bar).
- **Numbers** (`core/date-fast-Efir.txt`): at 220 ms the drop's middle stands at (132, 199) on its way to the 25th; on
  the frame after the click on the 3rd it stands at (116, 246), the 25th's cell, 49 px away, then runs back; again
  from (100, 200) to (80, 138) on the third pick. 2 645–2 872 pixels change on those frames against 900–1 100 on the
  run's frames. The same in every look but reduce motion.
- **Cause.** `DayRun` keeps the previous chosen day and the slide is asked from `(previous − chosen) · cell`.
- **Fixed** (`f2f23ea`). `DropFrom` keeps where the run it asked for started, in columns and rows from the chosen
  cell, and when; a new pick asks for a run from what is left of that one at the screen clock's now
  (`(1 − p)²`, `p` stepped under `steppedMotion`), as the body draws it, and the body's slide restarts only on a new
  run (its id is the run's count, so a month change or a deselection starts none). `CalendarRunTest` «B-168 a pick
  while the drop runs starts the new run from where the drop stands»: 5 → 25, then the 3rd 128 ms later, density 2;
  the drop's lime never moves more than 30 px a frame and lands on the 3rd. Mutant: the run asked from the day picked
  before again (`cols * 0.0`), killed — «frame 8: the drop leapt 100 px».
- **After** (`after/date-fast-Efir.txt`): on the re-aimed frames the middle moves on from where it stood ((132, 199)
  → (122, 227) over the click's 76 ms, then back towards the 3rd at 4–9 px a frame).

### F-pickers-03 — right to left, the days stand under the wrong weekdays — local

- **What.** The header and the weekday row are `Row`s and mirror (ВС at the left), but the day grid is a `Layout`
  that places its cells with `place`, left to right: the 1st of November 2026 (a Sunday) stands at the right, under
  ПН, and every date under the wrong weekday. The arrow keys move the focus a day «right» to the left of the screen.
- **Frames.** `core/date-pick-Rtl.png`, `core/date-keys-Rtl.png`.
- **The rule.** B-65 mirrored tabs and the navigation bar; the calendar mirrors half of itself already (its rows). A
  grid that agrees with its own weekday row is not a look to decide.
- **Fixed** (`ffa93d5`). The grid places its cells and the drop with `placeRelative`; the drop's run and its oval are
  mirrored on x, and Right and Left step the focus by the screen's direction. `CalendarRightToLeftTest`: right to left,
  the 1st to the 7th each stand under their own weekday (within 2 px), and a pick of the 18th lands the lime on it while
  Right moves the focus to the 17th, which stands right of it. Mutant: the cells placed with `place` again, killed —
  «day 1 stands 216 px off ВС» and «the lime stands at 224, the 18th at 152».
- **After** (`after/date-pick-Rtl.png`, `after/date-keys-Rtl.png`): the 1st under ВС, the drop runs from the 5th (ЧТ)
  to the 18th (СР) mirrored, the numbers split along it.

### F-pickers-04 — the time picker's digits turn at the click, the field's acid pours for 350 ms — local

- **What.** A click on the minutes makes them the field being set: their digits turn black at once while the acid takes
  the pour's 350 ms to come down over them, and the hours' digits turn light at once while their acid drains. For
  about 20 frames black digits stand on the dark field and light ones on the acid.
- **Numbers** (`core/time-switch-Efir.txt`, median ink against the twin; at rest 12–17:1): the hours 1.06:1 and the
  minutes 1.46:1 on the click's frame, under 3:1 until 280–296 ms; on the way back 1.46 and 1.06:1 again. Under
  `steppedMotion` the same, stepped (`core/time-switch-Stepped.txt`). «Копия» reads (dark digits on paper and acid).
  Reduce motion: both at once, readable.
- **Frames.** `core/time-switch-Efir.png` 168–488 ms and 800–1040 ms; `core/time-switchfast-Efir.png`;
  the sample's `sample/page-time-Efir.png`.
- **The decision.** B-94's rule as the chip holds it: a label turns when the pour's front passes under it
  (`inkOnBody`, the ground the surface provides).
- **Fixed** (`2d0afd9`). The digits take `inkOnBody`: the fill under the body's middle while it pours, whichever of the
  two colours reads better on it, as the chip's label (B-94). `TimePickerDigitsFollowPourTest`: «Эфир», a bare twin,
  the minutes picked then the hours, every frame for 768 ms each, with and without `steppedMotion`, the mouse taken off
  both: no field with more than 40 % of its digits' ink under 1.5:1. Mutant: the colour from the press again, killed —
  «stepped false, the minutes picked, frame 0: 60 % of field 0 unreadable».
- **After** (`after/time-switch-*.png`, `after/time-switchfast-Efir.txt`): each field keeps its colour until the front
  passes its middle. **What remains**: while the front crosses the 52 sp digits a part of them stands on the other fill,
  up to a third of the ink on the flip frame (`bad=[0.33, 0.25]` at 312 ms, under 0.2 within 50 ms,
  `after/time-switch-Efir.txt`). Splitting the digits along the front, as the calendar's numbers along the drop, needs
  the body's pour front from `MutantSurface` (the content gets only the ground under the middle,
  `LocalMutantBodyGround`): reported as F-pickers-11.

### F-pickers-05 — a pick re-aimed mid-run snaps the drop's stretch — reported (`surface/BodyTransitions.kt`, F-nav-bar-03)

- The calendar's face of F-nav-bar-03, seen once F-pickers-02 is fixed: a drop stretched tall along a run down the
  grid is 46 px tall on the frame before a new pick and 33 px (its rest) on the next, its middle where it was
  (`after/date-fast-Efir.txt` 220 → 296 ms: the lime's box 175..220 → 210..242; 2 402 pixels change on that frame
  against ~1 000). Every re-aimed pick shows it.

### F-pickers-06 — the calendar's height follows the month: 4, 5 or 6 rows — reported (owner; B-45, B-150's class)

- **What.** The grid is as tall as the month's weeks: November 2026 six rows, December five, February 2027 four. Each
  arrow press can change the body's height by 36 or 72 dp; the body is generated anew at the new size (D17), its
  outline and flow lines jump, and whatever stands under the calendar moves (the demo's timer sheet, the sample's row).
- **Frames.** `core/date-month-Efir.png` (296 → 330 px tall and back; the diff reads -1 for a size change).
- **Question.** Six rows always (the design's November has six), or the month's own count as now. B-150 kept the
  width; nothing decided the height.

### F-pickers-07 — the acid does not follow the keyboard: Up sets the minutes while the hours stay acid — reported (owner; B-45)

- **What.** B-45: «a press on a time field makes it the one being set. Up and Down step the field focused». With the
  keyboard on the minutes (brackets drawn) Up turns 30 into 31 while the hours keep the acid that says they are the
  field being set.
- **Frames.** `core/time-step-Efir.png` 682–890 ms.
- **Question.** Should keyboard focus make a field the one being set? It is behaviour B-45 decided, so it is not
  changed here.

### F-pickers-08 — no today, no disabled days, no typed digits — reported (owner; the design draws none)

- The brief asked to look at today and disabled days in the calendar and at typing a time: the kit has none of them.
  `MutantDatePicker` takes no today, no range and no disabled days; every day of the month is pickable; the time fields
  take Up/Down and `setProgress`, not digits. The design draws a still November with one chosen day and two fields; a
  look for today or a disabled day would be a new design (D15), and an API.

### F-pickers-09 — the sample's chosen day follows every month — reported (sample: `ComponentsSelection.kt`, `DemoSettings.kt`)

- **What.** Both keep only `day` and pass it with whatever month is shown, so the 13th picked in November stands
  chosen in December and October as well (`core/date-month-Efir.png` shows the component doing what it is told:
  `selectedDay = 13` in each month). A consumer's example should pass the chosen day only in its own month.

### F-pickers-10 — right to left, the month arrows keep their left-to-right look 30 dp apart — reported (owner; B-65's class)

- **What.** The arrows' glyphs stand ◄ ► with the back one at the left in both directions, but right to left about
  30 dp apart instead of 6 (centres 42 dp against 18), because each glyph is aligned to the end of a target that the
  layout places unmirrored (`Arrow`'s `layout` places with `place`; its `contentAlignment` mirrors). By the targets'
  arithmetic a press still lands on the arrow drawn under it (back on ◄, forward on ►); not clicked.
- **Frames.** `core/date-pick-Rtl.png` (any frame, the header's left end).
- **Question.** Which way do the month arrows point and stand in a right-to-left calendar (B-65 settled tabs and the
  bar only; segmented's F-segmented-06 asks the same)? The 30 dp gap is fixed with whatever answer that gets.

### F-pickers-11 — a time field's digits cannot be split along the pour's front — reported (`surface/MutantSurface.kt`)

- **What.** After F-pickers-04's fix the digits turn as the front passes the body's middle; the front takes about
  150 ms to cross the 52 sp digits, and meanwhile up to a third of their ink stands on the other fill (`bad=` in
  `after/time-switch-Efir.txt`, 296–440 ms; `after/time-switch-Efir.png`). The surface gives its content only the fill
  under its middle (`LocalMutantBodyGround`); a content drawn twice, clipped along the front (as the calendar does along
  the drop, the navigation bar along its pill), needs the front's path. Every pouring body with large words shares it
  (the chip's label is small enough to hide it).

### Known faces, not new

- A switch back to the other time field within the pour's first half turns both fields whole in one frame (4 200
  pixels against ~600 a frame; `core/time-switchfast-Efir.png` 352 ms): F-checks-switch-04 / F-chip-03
  (`surface/BodyMoves.kt`).
- The browser's way of drawing: the same numbers as «Эфир» on every scenario (`core/*-GpuEfir.txt`); no stale fill on a
  time field's pour.
- A press on a time field draws a moment of edge mosaic on its right side (`core/time-switch-Efir.png` 800 ms): the
  press's own look, as every pressed body.

## Looked at and found right

- **Appear**: both whole from the first frame on the desktop.
- **The drop's run** (apart from F-pickers-01/02): 330 ms, eased, landing exactly on the day, at once under reduce
  motion, in 83 ms steps under `steppedMotion`, the same at density 2.
- **A month change** moves the drop to the same number at once (B-93's decision), the 31st of a 30-day month shows no
  drop (`core/date-none-*`), no selection removes it in one frame and a pick brings it back without a run (as the
  navigation bar, B-24).
- **The width** (B-150): 284 dp in the sample's cell, which offers the whole row (`sample/page-date-*`).
- **The 36 dp cells** (D19) tile the grid; the keyboard's focus brackets sit inside the cell; Enter and Space pick.
- **The time fields** stay 76 × 72 through every value and switch (`sizes=` in `core/time-*.txt`); Up wraps 23 → 00.
- **«Копия»** reads on every frame of every scenario.

## Not verified

- The demo's timer sheet (`DemoSettings.kt`, the bottom sheet holding both pickers): its row stands below the phone's
  fold, and neither a wheel turn nor `performScrollTo` reached it under the held clock (the latter took the test JVM
  down, as earlier agents found on the sample page). The pickers there are the same components with keys `demo-cal`
  and `demo-tp`.
- The phone's GPU (B-167): nothing here touches the shader.
