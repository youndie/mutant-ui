# B-168 — bottom-sheet: `MutantBottomSheet`

Group 20 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-bottom-sheet` from `662fa7a`,
2026-11-22, on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand).

## What was looked at

Two throwaway tests, deleted before committing. The frames stay in `/private/tmp/b168-bottom-sheet/` (not in the
repository).

- **The sheet as the demo's Library composes it** (`core/`): a 360 × 600 screen of its own (`MutantScreenClock`), an
  opener button, a card and four list rows; the sheet (`demo-new`, `WindowInsets(0)`, `returnFocusTo` the opener) with a
  Handjet title, the three-speed segmented button and a Record button. Nine looks: «Эфир», «Копия», reduce motion,
  «Чистый сигнал», `steppedMotion`, density 2, the browser's way of drawing (`LocalMutantFirstFrameWaits` false,
  `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true, `LocalMutantBodyAsImage` false) in both palettes, and
  right to left. Every frame was diffed against the one before (`<name>.diff.txt`).
  - `openclose-<look>-*.png`: opened by a mouse click on the opener, risen, closed by Escape, the screen after.
  - `reverse-efir-*.png`: Escape 112 ms into the rise, reopened 96 ms into the sink (every frame).
  - `keys.log`, `exit.log`, `clicksheet.log`: Tab ×6 and Shift+Tab ×6 inside; Enter on Record and a second Enter
    during the sink; Escape then Enter; mouse clicks during the sink on Record and on the scrim; a click on the
    sheet's title then Enter.
  - `drag-efir-*.png`, `drag.log`: a touch drag of 150 dp down from the grabber, a fling down, a mouse drag.
  - `tall-0.png`, `tall.log`: the Settings timer's content (calendar, time picker, Done) in a 300 dp screen, plain and
    inside a `verticalScroll`.
  - `loss-efir-0.png`, `loss-last-0.png`: the sheet (a title and six rows) opened while the screen's keyframe loss runs
    (`KeyframeLossBody` around the screen).
  - `rtl-open-0.png`: open, right to left.
- **The sample** (`sample/`): the Overlays page's Sheets section (its «open» cell and «Try it», opened by the mouse);
  the Library phone's FAB sheet; the Settings phone's timer sheet (the list scrolled to «Timer recording», opened by the
  mouse); both palettes, every second frame of the rise and the last frame full size.

## Findings

### F-bottom-sheet-01 — a sheet reversed mid-way jumps: the rise to whole, the sink to nothing — local

- **What.** The sheet calls `DialogFollow(visible, start)` without the two durations, so the new direction restarts
  from its beginning wherever the old one stood (F-dialog-01's face).
  - Escape about 112 ms into the rise: the sheet stood about a third up (its title 70 dp under the screen's foot) and
    stood whole on the next frame, then sank from there.
  - Reopened 96 ms into the sink, the sheet about half down: it vanished in one frame and rose again from below.
- **Frames.** `core/reverse-efir-0.png`, 1120 → 1186 ms (risen whole in one frame) and 1266 → 1282 ms (gone in one).
- **The rule.** cmp-ui-component §1: every change of a discrete input has its entry, its exit and its interruption.
  B-35 decided the two durations (330 ms in, 200 ms out), not a jump between them.

### F-bottom-sheet-02 — focus goes back to the opener as the sink starts, so Enter during it reopens the sheet — local

- **What.** `ReturnFocus(visible, …)` gives focus to `returnFocusTo` on the sink's first frame, while the scrim still
  covers the opener. A second Enter during the 200 ms sink — a double Enter on Record — reaches the opener under the
  scrim and opens the sheet again; Escape then Enter does the same (F-dialog-06's face).
- **Frames.** `core/exit.log` («two Enters on record: {record=1, opener=1} open=true»), `core/keys.log` («Enter during
  exit after Escape: open=true, opener=2»); `core/reverse-efir-0.png` from 1186 ms: the opener's brackets under the scrim
  while the sheet still stands.
- **The rule.** B-35: «focus moves into it and goes back to `returnFocusTo` on close» — at its end, as the dialog's
  B-168 fix reads it; the screen behind stays out of reach until the sheet is gone.

### F-bottom-sheet-03 — a sinking sheet's content still answers a click and a key — local

- **What.** Through the 200 ms sink the window's content keeps its input. A mouse click on Record 32 ms into the sink
  (Record standing 30 dp lower) fired it (`core/exit.log`: «record=1» after Escape). With focus kept inside the sheet
  through the sink (F-bottom-sheet-02's fix), a second Enter on Record fires it twice. The scrim already stops answering
  as the sink starts (`OverlayAndMouseTest`); the sheet's own content does not.
- **The rule.** cmp-ui-component §4, Overlays: a closing overlay's actions stop answering as the exit starts (field-ui
  found «a leaving sheet's row clicked 48 ms into the exit»). The dialog's actions stop by `LocalDialogOpen`.

### F-bottom-sheet-04 — a keyframe loss running as the sheet opens cuts the window for the sheet's whole life — local

- **What.** The sheet's body is a `MutantSurface` inside the screen's `MutantKeyframeLoss`, so it joins the screen's loss
  scene. A loss running when the sheet opens is held mid-run with the screen's clock (the sheet holds it), and its band
  keeps cutting the window: from the tear line down about 60 dp the screen's frozen snapshot (the card) shows through
  the sheet, under its title, the grabber gone, for as long as the sheet is open (F-dialog-02's face).
- **Frames.** `core/loss-last-0.png` (4 720 ms, the sheet opened at 3 808 ms with the loss running),
  `core/loss-efir-0.png`.
- **Reach.** The demo's Library phone carries the loss, and its FAB opens this sheet («New recording»).
- **The rule.** B-98: the loss covers the screen, not what floats over it.

### F-bottom-sheet-05 — content taller than the screen: the window is centred and cut, and a scrolling content throws — local

- **What.** `SheetWindow` measures the body with no height limit, so the sheet's content never learns how much room
  there is.
  - A plain content taller than the screen: the window reports more than the screen gives, Compose centres it, and it
    is cut at both ends. In a 300 dp screen the Settings timer's sheet has its tear line, grabber and title above the
    screen (title at y −29) and its Done button at its foot (y 297 of 300); nothing scrolls.
  - A content that scrolls (`verticalScroll`, and a `LazyColumn` the same way) throws on the first frame: «Vertically
    scrollable component was measured with an infinity maximum height constraints». A consumer's long list in a sheet
    crashes the application (B-98's class: a crash under an ordinary overlay).
- **Frames.** `core/tall-0.png`, `core/tall.log`.
- **The rule.** cmp-ui-component §9 (no limit read as `Int.MAX_VALUE`, B-58); D5: the slot never depends on the body.
  The demo's sheets fit their 720 dp phones (`sample/timer-*-last.png`), so the sample never shows it.

### F-bottom-sheet-06 — the sheet's body and the sheet itself are Tab stops that show nothing — local

- **What.**
  - The window's body takes its clicks through `clickable {}`, which is focusable: Tab from the opened sheet stops on
    the body first (no brackets; `core/keys.log`, «Tab: Новая запись #27», the body's merged node), and a mouse click on
    the sheet's words moves focus to it unseen (`core/clicksheet.log`, «click title: Новая
    запись #125»).
  - Tab past the last control stays on it; Shift+Tab past the first goes to the sheet's own focusable container
    (`#24`), which shows nothing, and stays there.
- **The rule.** B-57 (F22): the scrim and the window are not focus stops, and Tab past the last control comes round to
  the first, Shift+Tab past the first to the last; F-dialog-03: a window's clicks are taken without moving focus.

### F-bottom-sheet-07 — the sheet cannot be dragged: no drag, no fling, no mouse drag — reported (owner)

- **What.** The sheet has no gesture at all. A touch drag of 150 dp down from the grabber, a fling down and a mouse drag
  leave it where it stands (`core/drag.log`: the title at 449 dp before and after each; `core/drag-efir-0.png`).
- **The design.** D15 names «sheet and drawer sliding and dragging» as designed here; B-35 left dragging to B-43, which
  set no numbers, and B-35's findings list the rise and the sink only. The grabber is drawn and promises a drag.
- **For the owner.** Whether the sheet drags, and its numbers: what follows the finger (the window, the scrim's
  darkness), the distance or share and the fling speed that close it, the settle back (and its curve), whether it
  drags up past its height; the mouse path (cmp-ui-component §4: a mouse drag scrolls nothing in Compose); how a drag
  that starts on content which scrolls hands over. The drawer (B-34) has none either.

### F-bottom-sheet-08 — a content taller than the room: does the sheet scroll it, and how far up may it rise — reported (owner)

- **What.** After F-bottom-sheet-05's fix the window is at most the screen's height and a consumer's scroll works; a
  plain content taller than that still runs off the screen's foot, its top (grabber, title) kept. The dialog scrolls its
  own text (B-59); the sheet would have to scroll its whole content, which then cannot hold a `LazyColumn` or a
  `verticalScroll` of its own (nested vertical scrolls throw).
- **For the owner.** Whether the sheet scrolls its content by itself, and a gap kept above a sheet risen to the
  screen's top (the tear line now can reach the screen's top edge): numbers B-35 did not decide.

## Looked at and as decided

- The rise (330 ms, ease-out²) and the sink (200 ms) run on every frame, step in 83 ms under `steppedMotion`
  (`core/openclose-stepped.diff.txt`: changes at 1180, 1260, 1340, 1420 ms), and are at once under reduce motion
  (one frame of change at 1084 and 1694 ms). The scrim fades with the rise.
- The body is whole on every frame of the rise and at rest in every look (B-97): no wedge, density 2 and the browser's
  way of drawing included (`core/openclose-d2-*.png`, `core/openclose-gpu-*.png`). The tear line stands over the body,
  the mosaic rising past the top edge under it (B-35).
- A mouse click on the scrim dismisses, a click on the sheet does not, and a click on the scrim while the sheet sinks
  reaches nothing (`OverlayAndMouseTest`).
- The screen behind stands while the sheet shows and runs again once it is gone.
- Right to left: the window, the grabber and the tear line are symmetric and stay so (`core/rtl-open-0.png`). The
  segmented button inside is not mirrored (F-segmented-06, reported).
- «Чистый сигнал»: the body is clean, the Tape off; the motion as in «Эфир».
- The sample: the Sheets section, the Library sheet and the timer sheet rise whole over their screens in both palettes;
  the timer's content fits the 720 dp phone (`sample/timer-*-last.png`).

## Fixes

Each in its own commit with a kept test in `SheetBehaviourTest`; each mutant was run after committing against the
aimed test, its failure read from the JUnit XML, and the file checked out again. After-frames:
`/private/tmp/b168-bottom-sheet/after/`.
