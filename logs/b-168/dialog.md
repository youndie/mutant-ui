# B-168 — dialog: `MutantDialog`, `MutantDialogAction`

Group 13 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-dialog` from `cb83869`,
2026-11-22, on this Mac (M-series, the desktop JVM, `runComposeUiTest` with the clock driven by hand).

## What was looked at

Two throwaway tests, deleted before committing. The frames stay in `/private/tmp/b168-dialog/` (not in the repository).

- **The dialog as the sample's «Try it» stage composes it** (`core/`): a 360 × 300 screen of its own
  (`MutantScreenClock`), a card, the «Open the dialog» button and the dialog with Cancel and the primary Erase. Eight
  looks: «Эфир», «Копия», reduce motion, «Чистый сигнал», `steppedMotion`, density 2, and the browser's way of drawing
  (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true, `LocalMutantBodyAsImage`
  false, as `FillChangeOnGpuTest`) in both palettes. Every frame was diffed against the one before
  (`<name>.diff.txt`); the sheets hold every frame of each transition.
  - `openclose-`: the button clicked by the mouse, the open, Cancel clicked, the close, the screen after.
  - `press-`: Erase pressed by the mouse, held 480 ms, released (it closes the dialog).
  - `reverse-`: Escape 112 ms into the open, then the dialog reopened 96 ms into the close.
  - `keys-` (`.log`): Tab to the opener, Enter, Tab ×3, Shift+Tab ×3, Escape, Tab.
  - `exitclicks.log`: clicks on the scrim during the exit over a target behind it; Enter during the exit.
  - `long-`, `wheel-`: 40 sentences of text, scrolled by the wheel and by a swipe, Tab to the actions.
  - `palette-`: «Эфир» switched to «Копия» while the dialog is open.
  - `loss-`: the dialog opened while the screen's keyframe loss runs (`KeyframeLossBody` around the stage).
  - `clickwindow-`: a mouse click on the window's text, then Enter.
- **The sample** (`sample/`): the Overlays page scrolled to the Dialog section (its «open» cell and «Try it»), opened by
  the mouse and closed by Escape, in both palettes; the demo's Settings phone (its dialog open from the start) for 2 s
  and closed by Cancel, in both palettes; the Library phone (which carries the keyframe loss) taken to Search, a row
  chosen and «Erase 1» opened as the loss ran.

## Findings

### F-dialog-01 — a dialog reversed mid-way jumps: the open to whole, the close to nothing — local

- **What.** `DialogFollow` restarts the new direction from its start, wherever the old one stood.
  - Escape 112 ms into the open: the dialog stood at about 35 % and showed at about 95 % on the next frame, then faded
    out from there.
  - Reopened 96 ms into the close (about half shown): it vanished in one frame and rose again from nothing.
- **Frames.** `core/reverse-efir-0.png` (288 → 354 ms), `core/reverse-efir-1.png` (450 → 466 ms); every look the same.
  The diff file shows the 100 k-pixel steps at 354 and 466 ms.
- **The rule.** cmp-ui-component §1: every change of a discrete input has its entry, its exit and its interruption; a
  cut is a choice to write down. B-28 decided the two durations, not a jump between them.

### F-dialog-02 — a keyframe loss running as the dialog opens cuts the window for the dialog's whole life — local

- **What.** The dialog's window and its primary button are bodies inside the screen's `MutantKeyframeLoss`, so they
  register with the screen's loss scene. A loss running when the dialog opens is held mid-run with the screen's clock,
  and its band keeps cutting every body of the scene: the window's top half is missing where the band stands, and the
  screen's frozen snapshot (the opener button, the card's drips) shows through it under the title and the text, for as
  long as the dialog is open.
- **Frames.** `core/loss-efir-1.png`, 4192–4448 ms (the loss ran from 3648, the dialog opened at 3840).
- **Reach.** The demo's Library phone carries the loss, and its Search tab opens this dialog («Erase n»): a loss runs
  for 0.9–1.4 s of every 9, so about one open in eight meets it. `sample/demo-search-loss-0.png` caught a band above the
  window, not across it.
- **The rule.** B-98: «The loss covers the screen, not what floats over it». A dialog is not a popup, so B-98's guard (a
  body in another layout root is skipped) does not reach it.
- **The same shape elsewhere (reported).** The bottom sheet runs on a clock of its own and holds the screen as the
  dialog does (`MutantBottomSheet.kt`), so it very likely shows the same cut; not looked at here. A rule in the scene
  («a body on a clock other than the scene's is not the screen's») would cover both and a consumer's overlay; it is
  `surface/` code.

### F-dialog-03 — a click on the window's text moves focus to the first action unseen; Enter then fires it — local

- **What.** The window takes its own clicks through `clickable {}` with `canFocus = false`. A press on a clickable asks
  for focus, and a focus target that cannot take it passes it to its first focusable child: the first action. A mouse
  click on the title, the text or the window's blank area therefore focuses Cancel with no brackets (the mouse puts the
  window in touch mode), and the next Enter fires Cancel. With the actions in the other order, a click on the text
  followed by Enter erases.
- **Frames.** `core/clickwindow-efir-0.png`, `core/clickwindow.log` (`after mouse click on text [true, false]`,
  `after Enter: open=false`); a swipe on the text does the same (`core/wheel.log`).
- **The rule.** B-57: what has focus shows it; cmp-ui-component §9: a scrim and a window are pointer targets only.

### F-dialog-04 — the KDoc says the open and the close step twelve times a second — local (documentation)

- **What.** Since B-100 the open and the close run on every frame and step only under `steppedMotion`
  (`core/openclose-efir-*` against `core/openclose-stepped.diff.txt`: steps at 572, 652, 732, 812 ms). The KDoc still
  says «Both step twelve times a second».

### F-dialog-05 — the window fades with the scrim, so the screen's words read through the window's — reported (owner)

- **What.** The open and the close are one alpha over scrim, band and window together (B-28). For most of the 320 ms
  the window is translucent: the opener's label and the card's drips stand inside the window, across its title and text
  («Open the dialog» over «The tape can be recorded again»), from about 30 % to 90 % shown. The close shows the same in
  reverse.
- **Frames.** `core/openclose-efir-0.png`, `-1.png` (540–716 ms), `core/openclose-kopiya-1.png` (668–764 ms),
  `sample/section-efir-0.png` (508–668 ms).
- **The design.** The brief's `pause`/`play` (320 / 200, curve `lock`) darkens the background and freezes it; the
  engine has no motion for it (the spec page says so), and B-28 decided «scrim, band and window come up together». For
  the owner: the window opaque from its first frame (only the scrim and band fading), or the window's own fade over the
  first part of the open.

### F-dialog-06 — focus goes back to the opener as the exit starts, so Enter during the exit reopens the dialog — local

- **What.** `ReturnFocus` follows `visible`: the opener takes focus on the close's first frame, while the scrim still
  covers it and takes every click. Enter pressed during the 200 ms exit reaches the opener under the scrim: the dialog
  closed by Escape opens again (`core/exitclicks.log`: «confirms by Enter during exit: 0, open=true»). A double Enter on
  Cancel does the same. Clicks during the exit are taken by the scrim (0 reached the target behind it).
- **The rule.** B-28: «its actions stop answering as it starts to close»; the screen behind it stays out of reach until
  it is gone, by the pointer and the keys alike. The focus still returns «on closing»: at its end, the frame it leaves.
- **The same shape elsewhere (reported).** `MutantBottomSheet` and `MutantNavigationDrawer` call the same
  `ReturnFocus(visible, …)`; their groups decide.

### F-dialog-07 — a window taller than the screen stands flush with its top and bottom, its foot cut — reported (owner)

- **What.** B-59 lets the text scroll inside a window as tall as the screen allows. The window keeps 24 dp from the
  sides but none from the top and bottom: in a 300 dp screen it stands from 2 to 300, its bottom outline and corners cut
  off by the dialog's clip. The text's last visible line is cut through its middle, with no sign that it scrolls (the
  wheel and a swipe do scroll it: `core/wheel.log`).
- **Frames.** `core/wheel-full.png`, `core/long-efir-0.png`, `core/long-d2-0.png`.
- **For the owner.** A vertical margin (the sides' 24?) and whether the cut line or a scroll hint is wanted: numbers
  B-59 did not decide.

### F-dialog-08 — the sample's three dialogs give no `returnFocusTo`: after Escape focus is nowhere — reported (sample)

- **What.** `ComponentsOverlays.kt` («Try it»), `DemoSettings.kt` and `DemoSearch.kt` open the dialog without naming the
  opener. After Escape no node is focused (`sample/section-efir.log`: «opener focused: false»), and a keyboard user starts
  again from the page's top. The face of F-snackbar-05 in the sample; the kit's KDoc says so.

### F-dialog-09 — a palette switch while the dialog is open: words on the old palette for the dialog's whole life — reported (shared)

- **What.** The sample's header switches the palette while a stage's dialog stays open.
  - The dialog's text keeps «Эфир»'s secondary colour on «Копия»'s cream window (F-buttons-02, B-169 merging).
  - The screen behind is held, so the card's pour to the new fill never runs. The card stands in «Эфир»'s dark fill
    under «Копия»'s dark words until the dialog closes, not 3–5 frames as in F-chip-02 and F-card-cocoon-02.
- **Frames.** `core/palette-efir-0.png` from 656 ms.

### F-dialog-10 — every dialog's window has the same body, and a consumer cannot key it — reported (owner)

- **What.** The window's key is a private constant (`"dialog"`), and the primary action's default is `"dialog-ok"`. The
  sample's «open» cell and «Try it» stage draw the same window and the same Erase button, drip for drip. This is
  F-card-cocoon-04's face, except that the window's key is not a parameter at all.
- **Frames.** `sample/section-efir-0.png` (the first cell against the others).

## Looked at and as decided

- The open (320 ms) and the close (200 ms) run on every frame, stepped under `steppedMotion` (where the close takes 250
  ms: three 83 ms steps, `stepped()`'s rounding, kit-wide), and at once under reduce motion.
- The screen behind stands still while the dialog shows, and only that screen (B-90): the Settings phone changes only by
  its button's boil (about 1 000 px per tick) and lives again 220 ms after Cancel.
- The primary action pressed: black on acid, then on amber, readable in every frame in both palettes (B-91). The plain
  action has no press look (F-buttons-05's face).
- Tab and Shift+Tab come round inside the actions, the brackets show on each, Escape dismisses, and pointer clicks on
  the scrim during the exit reach nothing.
- Density 2 and the browser's way of drawing: the same frames (the diffs within a few pixels of the desktop's).
- «Чистый сигнал»: the window is clean in every look already.
