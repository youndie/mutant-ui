---
id: B-183
title: "The drawer and the sheet follow a finger and a mouse"
status: done
priority: P2
size: L
stage: stage-6-later
blocked_by: []
---

# B-183 — The drawer and the sheet follow a finger and a mouse

The navigation drawer and the bottom sheet cannot be dragged by touch, fling or mouse, though D15 names their dragging (F-drawer-09, F-bottom-sheet-07). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 5 ([решение]).

- **The decision and its reason.** The panel and the sheet follow the finger 1:1, unquantised. They close when dragged past 40 % of their size or flung faster than 800 dp/s. The settle takes 330 ms on the `slide` curve. The scrim is proportional to the position. Past the open position they drag with a resistance of 0.3, at most 24 dp. While dragged, the body boils.
- A mouse drag scrolls nothing in Compose, so the gesture reads the pointer itself (cmp-ui-component §4).
- Not covered: the sheet's own scrolling and its hand-over to the drag (B-184).

- AC:
  - by touch and by mouse: a drag moves the panel and the sheet by the pointer's distance on every frame; released past 40 % or above 800 dp/s they close, otherwise they settle back in 330 ms
  - the scrim's alpha is proportional to the position at every frame
  - dragged past open, they move 0.3 of the pointer, never past 24 dp
  - the body is hot while dragged
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationDrawer.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantBottomSheet.kt`

## Result

### 2026-11-24

- **Looked at first**: throwaway contact sheets (`/private/tmp/b183/sheets/`, not in the repository) of the drawer and the
  sheet open over a screen, a finger dragging them 72 dp towards closed, back 108 dp past open, then 162 dp towards closed
  and let go, 12 frames after: in «Эфир» at density 1, «Копия» by a mouse at density 2, and «Копия» under reduce motion.
  Before: neither moved under a finger or a mouse. After: both follow the pointer, stop 24 dp past open, close from where
  they are let go and leave; the scrim lightens with the position; the body turns amber within 320 ms of the drag.
- **The drag** (`PanelDrag.kt`, used by both): one gesture, `panelDrag`, reads the pointer for a finger and a mouse alike
  (`awaitEachGesture` and `awaitFirstDown(requireUnconsumed = false)`, cmp-ui-component §4), from any press on the
  drawer's window or scrim, or on the sheet's window (its scrim above still dismisses on a click). The panel follows the
  pointer 1:1 from its first move, unquantised under `steppedMotion` too, and a press catches it where it is drawn
  (mid-slide in, mid-settle, past open). Past the touch slop the gesture consumes the moves, so a press on a row, a
  button or the scrim under it is cancelled. Not `draggable` for a finger: it holds the panel through the slop and then
  jumps by all of it, and hears no mouse. The orchestrator's brief named the pointer path for the mouse only; the same
  path serves the finger, for that reason.
- **Let go** past 40 % of its size (the drawer's 202 dp window, the sheet's window height) or faster than 800 dp/s towards
  closed (the pointer's velocity tracker), the panel calls `onDismissRequest` and closes from where it stands; otherwise
  it settles back. Both glides are the slider's (B-182): `MutantMotion.SLIDE_MS`, `(1 − p)²` of the way left, from the
  kit's `stepped`, so on every frame and in the hot tick's steps under `steppedMotion`; under reduce motion, and on a
  frozen clock, they land at once (D24). A consumer that keeps the panel visible after the dismissal sees it settle back;
  one that closes it while it is held or settling sees it close from there. The frame stays composed until a close glide
  is over (330 ms), past the 200 ms slide out it replaces.
- **Past open** the panel moves 0.3 of the pointer, at most 24 dp (`PANEL_DRAG_RESIST`, `PANEL_DRAG_OVER_DP`, beside
  `PANEL_DRAG_CLOSE_SHARE` and `PANEL_DRAG_CLOSE_SPEED`, each naming answer 5).
- **The scrim** is `0.5 × (1 − closed share)` on every frame: dragged, settling, closing, and in the scripted slide in and
  out too, where it followed the time linearly while the window followed ease-out², so a press mid-slide does not jump
  the scrim. Answer 5 names it, and it wins over D24's «no smooth opacity», which governs how a body appears and changes;
  recorded under D24.
- **The boil**: each panel's window gets its own `HeatMachine` through `MutantSurface(heatMachine = …)`, pressed from the
  slop on and released at the lift, so a tap with a finger's jitter does not warm the window. At heat 1 the window is
  amber, and the drawer's words would stand white or amber on it (the title vanished, the rows read about 1.9:1 in the
  contact sheet): they now take `inkOnBody` (B-91), dark on the amber and their own colour at rest. At rest nothing
  changes.
- **Tests**, `PanelDragTest` (density 1, the drawer and the sheet each, read from the drawer's title and a word on the
  sheet; every case by a finger and by a mouse unless named):
  - a drag of 10 × 6 dp: the window stands within 1 dp of the pointer's travel on every frame, inside the slop and past
    it (AC 1);
  - 30 % at under 400 dp/s and still: every frame of the settle within 1.5 dp of `x₀ · (1 − p)²`, back at 0, not
    dismissed (AC 1);
  - 50 % at under 400 dp/s, and 48 dp at 1 500 dp/s: dismissed once, every frame of the close on
    `size − (size − x₀) · (1 − p)²`, then gone; 48 dp at 375 dp/s is not dismissed (AC 1);
  - 12 × 10 dp towards open: `−min(0.3 · travel, 24)` within 1 dp on every frame, then back through open with the pointer
    (AC 3);
  - over a white screen, a finger: the scrim's alpha, from the mean red of a 50 × 40 or 320 × 40 dp patch, within 0.02
    of `0.502 · (1 − closed share)` on every frame of the slide in, a drag past half way and the close (AC 2; measured
    within 0.0054);
  - held 40 dp along for half a second: the mean red-minus-blue of a body patch rises from −0.15 to 1.0, and is back at
    −0.15 1.5 s after the lift (AC 4; threshold +0.3);
  - under reduce motion a short let go is back and a long one gone in the next frame (D24);
  - right to left, the drawer follows a finger to the right, 1:1 (cmp-ui-component §4).
- **Before the change** (`origin/main`'s `MutantNavigationDrawer.kt` and `MutantBottomSheet.kt`, `PanelDrag.kt` removed),
  all eight fail: `at 0.0 after 6.0`; `held at 0.0, 60.6 wanted`; `let go at 0.0 of 202.0`; `at 0.0 after 10.0 towards
  open, −3.0 wanted`; the scrim `0.024, 0.047 for 0.905 closed` (the slide in's linear scrim); the amber `−0.153 dragged,
  −0.153 at rest`; reduce motion `it follows the finger`; right to left `at 0.0 after 6.0`.
- **Mutants**, after `281bbeca` and `579647ef`, each against its aimed test, read from its JUnit XML, the file copied back
  and the sources clean after each:
  - the panel moving only past the slop: killed, 1:1 (`at 0.0 after 6.0`);
  - a settle of `(1 − p)`: killed, the settle (`57.66, 54.87 on the curve` at 16 ms);
  - 2 000 dp/s to close: killed, the close (`flung: dismissed expected 1 but was 0`);
  - a resistance of 0.5: killed, past open (`at −5.0 after 10.0, −3.0 wanted`);
  - the drawer's scrim back on the time (`alpha = shown`): killed, the scrim (`0.024, 0.047`);
  - the sheet's scrim on the slide alone: killed, the scrim (`Sheet, sliding in: 0.0, 0.047`);
  - the drawer's and the sheet's window never heated: each killed, the boil (`−0.153 dragged, −0.153 at rest`);
  - reduce motion not standing the glide (`still = clock.frozen`): killed, reduce motion (`back at once, at 60.6`);
  - the drawer's direction not mirrored: killed, right to left (`at −1.8 after 6.0`).
- **Goldens and parity cells**: none moved; they draw open on a frozen clock. `DrawerMotionTest`, `DrawerBehaviourTest`,
  `SheetBehaviourTest` and the parity tests pass unchanged.
- **ABI**: the dumps move by one compiler-named lambda of `ComposableSingletons$MutantNavigationDrawerKt`; no public
  declaration changed.
- **Gates on this Mac**: `make check` green; `./gradlew check` green (622 tests in `mutant-core`'s desktop suite and 50 in
  the sample's, none failed, read from the JUnit XML; `checkKotlinAbi` after `updateKotlinAbi`, above);
  `./gradlew :mutant-core:compileKotlinIosArm64` green; `make docs-against BASE=origin/main` clean.

### Findings

- **F-1, the consumer's own words on a hot sheet** — reported (owner). The sheet's body turns amber while dragged, and a
  word the consumer sets on it in the theme's text colour (in the sample, the new recording sheet's `BasicText` title and its `Osd` line)
  keeps that colour: light on amber in «Эфир», about 1.9:1, for as long as the drag lasts. The kit's own components on it
  (list items, cards, buttons) turn through `inkOnBody`, which is internal. Exposing the ground's ink, or a lesser heat
  for a panel's body, is a choice for the owner.
- **F-2, past open the drawer shows its start edge.** Its body runs off the screen's start by 10 dp; pulled the full
  24 dp past open, 14 dp of the screen show at the start, under the body's edge and its misregistered shadow. Looked at
  in the contact sheets; kept, as the answer names the 24 dp and a wider run off would regenerate the body (D17).
- **F-3, a closed drawer is not pulled out from the edge**: it is not composed while closed, and no AC asks for it.
- **F-4, a tap's jitter moves the panel** by the jitter and settles it back in 330 ms, as it follows the pointer from its
  first move (AC 1); it neither closes it nor heats it, as both need the slop.
- **Not verified**: a phone, a real window and the browser (no device, adb or real browser was used); a drag over a
  scrolling content in the sheet, whose hand-over is B-184 (here a move the content consumes first is the content's and
  the sheet settles back).
