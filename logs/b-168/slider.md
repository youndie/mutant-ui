# B-168 — slider: `MutantSlider`

Group 15 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-slider` from `2afc3e9`, 2026-11-22,
on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand). The slider lives in
`mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSlider.kt`; its numbers were decided in
[B-30](../../docs/backlog/B-30-slider.md) (and B-85 for the value's raster).

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-slider/` (not in the repository).

- **The slider as the sample composes it** (`core/`): the sample's three cells (`Sliders` in `ComponentsSelection.kt`:
  rest `sl-rest`, dragging `sl-drag` with a held press, disabled `sl-off`), 280 dp wide on the page's colour, in eight
  looks: «Эфир», «Копия», reduce motion, «Чистый сигнал», `steppedMotion`, density 2, and the browser's way of drawing
  (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true,
  `LocalMutantBodyAsImage` false) in both palettes. Per look: appear (`<look>-appear`), two seconds at rest (per-frame
  diff in `log.txt`), a mouse drag taken 4 dp right of the handle's middle, 6 dp a frame to past the end and back, then
  let go (`<look>-mousedrag`, the value and the handle's middle per frame in `log.txt`), a mouse press on the channel
  far from the handle (`<look>-tap`), a finger drag of 8 dp a frame, each move in its own block (`<look>-touchdrag`).
  Then: keyboard focus, arrows, Home and End (`Efir-keys`, `Kopiya-keys`, `D2-keys`); a value set from outside while
  held, and `enabled` turned off while held (`special-outside-disable`); right to left (`special-rtl`); font scale 1.5
  and 2 while dragging across +6…+10 (`special-fontscale-*`); widths 0, 20, 34, 48, 80, 140 dp at −10, 0, +10 and the
  ends at density 2 (`special-narrow-d2`, `special-ends-d2`); a finger scrolling a column, starting on the slider
  (`log-specials.txt`); the knob's width per value and font scale (`widths.txt`).
- **The sample itself** (`sample/`): `MutantUiSample(SampleSettings(group = Selection))` scrolled to the slider in both
  palettes at density 1 and 2, the rest slider dragged by mouse (`page-*.png`, `page-*-held.png`); the demo's Settings
  and Recording screens (`DemoScreenFrame`, `demo-*.png`) and a finger scrolling the Settings screen from its slider
  (`demo-Settings-after-scroll.png`, `log-demo.txt`).

## Findings

### F-slider-01 — a finger scrolling the page from the slider's row moves the value and does not scroll — local

- **What.** The row takes every pointer at its first touch: the handle goes to the finger, and the gesture consumes
  every move, so a scrolling ancestor never sees it. A thumb that starts a page scroll on the slider's 60 dp row sets
  the value under it instead, and the page stands.
- **Numbers.** A finger down 30 dp in on the channel, then 200 dp up: the value −2 → −8 on the demo's Settings screen,
  the screen not scrolled (`sample/log-demo.txt`, `sample/demo-Settings-after-scroll.png`); in a plain scrolling column
  +3 → −7, scroll 0. The same finger on the header (above the row) scrolls the column 415 px (`core/log-specials.txt`).
  Both demo screens with a slider and the Components page scroll vertically.
- **The design.** The engine drags only its knob (`bindDrags`: `pointerdown` on the `data-mu-drag` node); B-30 chose
  that "a press anywhere on the 60 dp row takes the handle there", a press, not a scroll. The skill's rule: a control
  inside a scrolling ancestor takes a finger only past its slop (cmp-ui-component §4).
- **Fixed** (`87e5446`). A finger away from the handle takes it once it has moved across by the touch slop
  (`awaitHorizontalTouchSlopOrCancellation`), or where it lifts without moving (a tap, B-30's press); a scroll that
  claims the finger first leaves the value as it was. A mouse, and a finger on the handle itself, take it at once as
  before. The press is now let go in a `finally`, so a gesture cut off still releases it. KDoc says so.
  `SliderBehaviourTest` «a finger scrolling the page from the row scrolls it, and a finger across or a tap still takes
  the handle». Mutants, each killed by it (`<failure>` read in the JUnit XML):
  - every finger taken at once (the old path): «a scroll leaves the value ==> expected: <0> but was: <-7>»;
  - a tap not taking the handle: «a tap takes the handle there ==> expected: <6> but was: <-10>».
- **After.** The demo's Settings screen scrolls (the slider from y 593 to 288) and keeps −2
  (`after-sample/log-demo.txt`); the plain column scrolls 415 px, value kept (`after/log-specials.txt`). Mouse and
  finger drags from the handle unchanged (`after/*-mousedrag.png`, `after/*-touchdrag.png`, the same values and
  places per frame as before).
- **Left.** A finger on the 34 dp handle itself still takes it at once, so a scroll starting exactly there moves
  the value; and a finger away from the handle shows the boil only once it is taken (after the slop, or at the lift).

### F-slider-02 — right to left, the numbers read backwards and only the header mirrors — local

- **What.** Under `LayoutDirection.Rtl` the value in the handle reads `3+`, `2-`, `5-`, and the range `10+ … 10−`:
  each is laid out as a right-to-left paragraph, and the signs are neutral to the bidi algorithm. The header's row
  mirrors (the label at the right, the range at the left), but the channel, the fill, the handle, the pointer and the
  arrow keys stay left to right: a press 40 dp from the left edge moved 40 dp to the right set −5, the left-to-right value
  (`core/special-rtl.png`).
- **The design.** It draws no right-to-left slider. The kit's rule is to mirror ([B-65](../../docs/backlog/B-65-tabs-and-navigation-in-rtl.md),
  "both mirror; the rejected alternative is documenting LTR only"; the list item, snackbar and top bar mirror too), and
  a number is never damaged (brief, «Порча смысла»).
- **Fixed** (`42666da`). The slider mirrors: the handle placed with `placeRelative`, the fill drawn from the right
  end, the pointer measured from the start edge, Left and Right swapped (Up and Down not). The value and the range are
  laid out left to right; right to left, the range is written from the channel's left end, `+10 … −10`. KDoc says so.
  `SliderBehaviourTest` «right to left it mirrors…». Mutants, each killed by it:
  - the handle placed with `place`: «the handle's middle at 215.5, 83.5 wanted»;
  - the value laid out in the layout's direction: ««+5» laid out left to right ==> expected: <Ltr> but was: <Rtl>»;
  - the pointer not mirrored: «expected: <8> but was: <-8>»;
  - the right arrow stepping up: «expected: <7> but was: <9>»;
  - the fill drawn from the left: «the fill right of the handle: [3, 4, 5, …]».
- **After.** `after/special-rtl.png`, `after/Rtl-*.png`, `after/GpuKopiya-*.png` (right to left in the browser's way):
  `+3`, `−2`, `−4` whole, the fill from the right, a press 40 dp from the left then 40 dp right sets +5. Left to right
  nothing moved: `SliderParityTest` unchanged.

### F-slider-03 — a narrow row or a large font cuts the range off the header — local

- **What.** The header is a `Row` with `SpaceBetween` and no weight: when the label and the range do not both fit,
  the range is pushed out past the slider's end and cut. At font scale 2 on the sample's 280 dp, `TRACKING  −10 … +10`
  shows `−10 …` (`core/special-fontscale-2.0.png`); at density 2 and font scale 1, 80 dp shows `TR −10`, 48 dp `TR −`
  (`core/special-narrow-d2.png`).
- **The rule.** A number grows or shares, a word ends in an ellipsis (cmp-ui-component §9; B-30's AC: "a constraint
  sweep … neither throws nor clips the content"). B-30's font-scale test used `ТРЕКИНГ` and 300 dp, which fit.
- **Fixed** (`da2734b`). The range keeps its width; the label takes what is left, ends in an ellipsis, and keeps 8 dp
  before the range (a number designed here: `HEADER_ROOM`). Where both fit nothing moves (`SliderParityTest`
  unchanged). `SliderBehaviourTest` «when the label and the range do not both fit…» at font scale 2 on 280 dp and at
  scale 1 on 110 dp. Mutant: the label without its weight — killed, «scale 2.0, 280 dp: the range 153.0 wide in 41».
- **After.** `after/special-fontscale-2.0.png`: `TRACK…  −10 … +10`.
- **Left.** Under about 78 dp the range alone does not fit and is still cut (`after/special-narrow-d2.png`: `−1` at
  20 dp, `−10 …` at 48 dp). The slider is then narrower than two handles; a range that wraps or hides is the owner's.

### F-slider-04 — at a larger font the handle changes width as the value changes, and jumps while dragged — local

- **What.** B-30 widens the handle for a value that does not fit with 4 dp of room; the kit measures the value shown,
  so the handle's width follows the value. Dragging across 0 or ±10 widens and narrows the knob, the travel changes
  under the finger, the knob stands somewhere else, and the body is generated anew at the new size (D17: new flow lines
  and drips) in one frame.
- **Numbers** (`core/widths.txt`): at font scale 2, `0` 34 dp, `±1…±9` 38 dp, `±10` 53 dp; at 1.5, `±10` 42 against 34
  for the rest; at 1.3, 37 against 34. At font scale 1 every value of −10…+10 fits 34 (B-30's AC holds), but a range
  of −100…100 widens only at ±100 (38). Frames: `core/special-fontscale-2.0.png`, +9 → +10.
- **The rule.** B-150's class: a layout that changes width on a state change. B-30's number stays — 4 dp of room — and
  the value to fit is the widest the range can show.
- **Fixed** (`263b105`). The handle fits the widest of the range's two ends and the value shown, with B-30's 4 dp of
  room. At font scale 1 every value of −10…+10 still fits 34 dp, so no parity cell moves. `SliderBehaviourTest` «at
  font scale 2 the handle keeps one width whatever value it shows». Mutant: the value shown alone measured — killed,
  «the handle's amber at 0, +9, +10: [33, 37, 52]».
- **After.** `after/special-fontscale-2.0.png`: the knob 53 dp from +6 to +10 and back, its body the same.

### F-slider-05 — a value set from outside while the handle is held is shown at the pointer's place — reported (owner)

- **What.** While held, the handle stands where the pointer puts it (B-30). The app setting another value then (a
  remote, a reset, a clamp that refuses part of the range) shows the new number in the handle at the old place until
  the pointer moves or lets go; let go, the handle jumps to the value's place in one frame.
- **Numbers.** Held at +5, the value set to −8: `−8` drawn at the +5 place for four frames, then 164 dp to the left in
  one frame at release (`core/special-outside-disable.png`, rows 2–3). A consumer that clamps to ≤ +5 sees the handle
  follow the pointer to +10 with `+5` in it.
- **Why not fixed here.** B-30 decided only that the handle follows the pointer while held. Following the value
  instead (Material's way) breaks the follow for a consumer that rounds; the choice is the owner's.

### F-slider-06 — every move the slider makes by itself is a cut — reported (owner)

- **What.** The handle moves only under a pointer; every other change of the value moves it in one frame:
  - a press on the channel away from the handle: 234 dp on the sample's 280 dp slider (`core/Efir-tap.png`);
  - each arrow key one step: 12.3 dp at 280 dp, 29.4 dp on the Recording screen's 0…10 at 328 dp; Home and End the
    whole travel (`core/Efir-keys.png`);
  - the settle at release, at most half a step: 6.15 dp at 280 dp, 14.7 dp on the Recording screen;
  - a value set by the app (F-slider-05) or by a reader's `setProgress`.
- **The design.** The engine has no tap, no keys and no settle (its handle stays where it is let go, the value
  rounded). B-30 designed these (D15) without a motion; the kit's slide (`slid`, 330 ms) and the cmp-ui-component §2
  rule ("every change of a discrete input is a transition") would make them moves, but the duration and curve for the
  slider are a number to decide, not to take here.

## After the fixes

The scratch tests re-run on the fixed code into `/private/tmp/b168-slider/after/` («Эфир», «Копия», density 2, right to
left, the browser's way in «Эфир» and in «Копия» right to left; keys; the specials) and `after-sample/` (the demo's
scroll): every drag, tap and key as before in left to right, frame for frame in the logged values and places.

## Looked at and found right

- **Dragging**, by mouse and by finger, every look: the value and the handle follow every move (each finger move in
  its own block reached the gesture, 8 dp a move: `core/log.txt`); the handle is held by where it was taken, stops at
  the ends, and boils while held (the per-frame diff steps at the hot tick, 12 a second, and still so through the 300 ms
  filmed after release, as the heat cools). The fill always ends under the handle's middle and never grows from its right end, at the ends included
  (`core/special-ends-d2.png`).
- **Disabled while held**: the gesture ends with the modifier; the handle goes to the value's place, the press is let
  go, and enabled again the handle answers the value (`core/special-outside-disable.png`, rows 5–7).
- **Keyboard**: the focus brackets come with keyboard focus, the arrows step by one, Up too, Home and End
  reach the ends, nothing is drawn past them (`core/*-keys.png`).
- **«Копия», «Чистый сигнал», `steppedMotion`, the browser's way of drawing**: per-frame diffs within a few pixels of
  «Эфир»'s through every scenario; no stale body in the browser's way (B-166's class not seen). Reduce motion: the
  slider stands; the handle still follows the pointer and the value changes, the press shows nothing (F-buttons-06).
- **Density 2**: every part where it stands at density 1, the value's raster smooth (B-85 nothing new on this Mac).
- **Appear**: the first frame draws the whole slider, its body at rest.
- **The sample**: the three cells as composed, the rest slider dragged by mouse on the page; the demo's two sliders.

## Gate

`./gradlew :mutant-core:desktopTest :sample:desktopTest :mutant-core:ktlintCheck :sample:ktlintCheck --continue` on
this Mac at `87e5446`, the scratch tests deleted: `mutant-core` 436 tests and `sample` 22, no `<failure>` or `<error>`
in the JUnit XML; both ktlint checks clean. The public API did not change (`SliderBody` is internal).

## Not verified

- The phone and the browser itself: the browser's way was drawn through the test seams on the desktop JVM only, and
  the body shader (B-167's class) not at all. The touch slop that decides F-slider-01 is the desktop test window's;
  a phone's is its own.
- A scroll that starts on the handle itself (left as it was, F-slider-01) and a horizontally scrolling ancestor
  (a slider in a pager or carousel, where the finger's horizontal slop is contested) were not looked at.
- The sample's Components page was looked at by mouse only; a finger on it is the demo's case.
