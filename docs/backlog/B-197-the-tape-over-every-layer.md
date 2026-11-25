---
id: B-197
title: "The Tape over menus, tooltips and dialogs"
status: wip
priority: P1
size: L
stage: stage-6-later
blocked_by: []
---

# B-197 — The Tape over menus, tooltips and dialogs

The Tape does not cover popups: an open menu or a tooltip floats above it (F-menu-10); over the Settings phone it stands still while its dialog is open (F-screens-10). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 22 and 23 ([ошибка]).

- **The decision and its reason.** The Tape is the window's topmost layer, over menus, tooltips and dialogs, and it runs while a dialog is open.
- A popup is its own root, a separate window on Android, so a Tape over it is either drawn in every popup's root in register with the window's, or by one overlay above them all. The Tape's cost was measured on the phone (B-140, B-141), and this changes what it draws over; it is not closed until the phone measures it again, which waits for the owner's word to use the phone.

- AC:
  - with a menu, a tooltip and a dialog open, the Tape's band and noise are drawn over their pixels, in register with the Tape under them
  - the Tape runs while a dialog is open
  - the phone's frame cost of a page with a menu open is measured against B-141's, on the owner's word
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantTape.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantMenu.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTooltip.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDialog.kt`

## Result, 2026-11-25

Done but the phone: the first, second and fourth AC hold on desktop; the third, the phone's frame cost, waits for the
owner's word (Iteration 1 below). The status stays `wip`.

- **Which of the two designs: the Tape in every popup's root, in register with the window's.** `mutantTape()` now
  registers what it draws with (its clock, palette, stage, seeds, rasterised fills, steps) and where it stands on the
  screen (`positionOnScreen`, its size) with the window's `LocalMutantTapes`, which `MutantTheme` provides (an inner
  theme keeps the outer one's). The kit's two popups, the menu and the tooltip, take `mutantTapeOver(reach)`: every Tape
  of the window that the popup stands over is drawn over the popup, at the Tape's own size and origin moved by where the
  popup stands, clipped to the Tape; the popup and what it draws up to `reach` past its bounds (28 dp: drips of 18 and
  20, the 4 dp shadow, the outline) are drawn in a layer, and the Tape composited `SrcAtop` over it, so the popup's
  pixels get the Tape and what the popup leaves clear shows the window's Tape once. Its draw is in a layer around the
  popup's own, as B-141 put the window's, so a Tape frame replays the popup rather than drawing it again. Private
  helpers carry the Tape's name (`drawTapeLook`, `TapeOverOrigin`), for Kotlin/Native's one namespace per package.
- **Why not one overlay above all of them.** Compose has no public way to keep a popup above popups opened after it:
  on desktop and in the browser the newest layer is on top, on Android the newest panel window. A window-sized popup
  takes every press in its bounds (plakat-ui B-72, B-200 here), and a 0 × 0 one drawing past its bounds may be clipped
  on Android, where a popup is a window of its own (B-200's open note). And on Android the overlay would be a second full-screen,
  translucent window, redrawn on every frame the Tape moves and blended by the compositor over the app's window for as
  long as the app runs, not only while a popup shows.
- **What each costs on Android**, where a popup is a separate window: the chosen design redraws the popup's own window
  on every frame of the Tape while the popup shows (before, it drew only on its own changes: the menu's boil and runs, at
  ticks), at the popup's area, with one offscreen layer of that area plus the reach: a second window's traversal and
  render each frame (its own `ViewRootImpl` and render-thread sync), and the compositor's layer for it, which it had
  already. The window's own Tape is unchanged. The overlay would cost a full-screen layer drawn and blended on every
  frame all the time. Neither was measured; the phone run below is what measures the chosen one.
- **The dialog.** The dialog, the sheet and the drawer are not popups: they stand inside the screen's box, so the
  window's Tape was already drawn over them (as the dialog's KDoc now says: place it inside the screen's `mutantTape`).
  What stood still was the Tape: the dialog holds the screen's clock (B-90), and the Tape read that clock. The clock now
  keeps a Tape time (`MutantClock.tapeMs`, `tapeFrame`, `tapeSceneTick`) that moves through a hold, while the bodies'
  time stands as before; a Tape registers as a `tapeReader`, and a held clock with one keeps asking for frames, its own
  time and frame counter standing as when it asked for none. `setTime` sets both. Under reduce motion (the pause) and
  frozen, the Tape stands as before. The same holds for a bottom sheet and a navigation drawer, which hold the clock
  the same way.
- Documents: research D10 (the decision and the rejected overlay), `docs/services/mutant-core.md` (the Tape's time, the
  popups, the new test), the dialog's and `MutantScreenClock`'s KDoc. A consumer's own `Popup` is not covered: the
  modifier is internal; say so if one asks.

**Frames looked at** (throwaway sheets in `/private/tmp/b197/sheets/`, the scratch test deleted): a 320 × 240 dp Tape
with the menu open at 20, 20 dp, and with a hovered tooltip, 15 frames 96 ms apart through the band's pass, at density 1
and 2, in «Эфир» and «Копия»; a 360 × 400 dp Tape over an open dialog, six frames 400 ms apart, in both palettes and
under reduce motion. Before: the band stops at the menu's and the plate's edges and runs on past them; over the dialog
every frame is the same, the band standing at the screen's foot. After: the band, its noise and the scanlines cross the
menu, its drips, the lime window and the plate in line with the band beside them; the band passes over the dialog's
window; under reduce motion the dialog's frames are still alike.

**Tests** (`mutant-core/src/desktopTest/…/surface/TapeOverEveryLayerTest.kt`; each failed on the code before the change,
run with `main`'s five source files written back):

| AC | Test |
|---|---|
| the band and noise over a menu, in register | «the Tape's band is drawn over an open menu …» and «the Tape's noise …»: one part of the Tape alone, on a frozen clock at a moment the band crosses the menu, read as the pixels it changes (≥ 3 levels) against the same scene with no part drawn, over the menu's rows (an area, not an edge): with the menu open, and with it gone, where the part lands on the ground there. Held if over the menu it changes at least half of what it changes on the ground, at most 5 % of its changes fall where the ground's do not (out of register, or past the Tape's foot: the menu stands across it), and beside the menu, inside the 28 dp its layer takes in, at most 1 % of pixels differ from the ground's Tape (no Tape twice). «Эфир» at density 1, «Копия» at 2. Measured: band 4 800 of 4 800 and 19 094 of 19 200, noise 763 of 763 and 2 984 of 2 988, 0 out of register. Before: 0 over the menu |
| the band and noise over a tooltip, in register | «… over a tooltip's plate …», the same reading over the plate, which stands across the foot of a 100 dp Tape; «Эфир» at density 2, «Копия» at 1. Measured: band 5 793 of 6 464 and 1 598 of 1 616, noise 1 662 of 1 662 and 423 of 424, 0 out of register. Before: 0 |
| over a dialog; the Tape runs while a dialog is open | «the Tape runs while a dialog is open, its band over the dialog's window in register with the band beside it»: the band alone over a 360 × 400 dp screen with a dialog open on a running clock, eight shots 240 ms apart: the band moves between shots (7 of 7), its rows in the window's frame (clear of its words) are its rows beside the window within 2 rows in each pair, and it crosses the window (94 rows changed there). Before: it moved in 0 of 7 |
| the phone | not run: Iteration 1 |

Every case of the menu and the tooltip also reads the strip beside the popup, inside the 28 dp its layer takes in: at
most 1 % of its pixels may differ from the ground's Tape there (the Tape drawn once, not twice).

The existing suites stay green on this branch, among them B-179's and B-185's tune-in and dialog tests
(`BodiesTuneInTest`, `DialogOpaqueAndTallTest`, `DialogBehaviourTest`), B-200's `TooltipEdgesTest`, B-206's place keys,
B-141's `MutantTapeTest` (the Tape's frames draw neither its holder nor its siblings again) and B-90's
`ScreenClockTest` (the screen under a dialog stands: its own time does not move).

**Mutants**, after the commit (`d61a394`), each one line, read from the aimed test's `<failure>` in the JUnit XML, the
file copied back and `git status` clean after each:

| Mutant | Aimed test | Killed by |
|---|---|---|
| the menu without `mutantTapeOver` | band over a menu | over the popup 0 of the ground's 4 800 px |
| the tooltip's plate without `mutantTapeOver` | band over a tooltip | over the plate 0 of 6 464 |
| the popup's Tape at the Tape's screen origin, not moved by the popup's | noise over a menu | over the popup 0 of 763 |
| the popup's Tape 3 px out of register | band over a menu | 568 px over the popup where the ground has none (of 4 739) |
| the popup's Tape composited `SrcOver`, not `SrcAtop` | band over a menu | beside the popup 288 of 1 680 px differ (the Tape twice) |
| no Tape found for any popup | noise over a tooltip | over the plate 0 of 1 662 |
| the Tape's time standing through a hold | the dialog | the band moved between 0 of 7 shots |
| a held clock asking no frames for its Tape | the dialog | the band moved between 0 of 7 shots |

**Goldens and parity cells**: none moved. The window's Tape draws as before (its draw went into `drawTapeLook`, the same
calls under a translation by zero); no kit golden shows an open menu or tooltip over a Tape, or a dialog over a running
Tape. The whole desktop suite, goldens and parity included, is green.

**Not verified**: no phone, no browser, no iOS run (iosArm64 compiles). On Android the popup is a window of its own:
that `positionOnScreen` of the popup's content and of the Tape's node agree there, so the Tape is in register, is
assumed, not seen; so is the menu's popup clipping its drips and the Tape over them to its window. Chrome and iOS draw
popups on the window's canvas, as the desktop does, assumed alike. A consumer's own `Popup` gets no Tape. The cost is
not measured: Iteration 1.

## Iteration 1, 2026-11-25

Everything but the phone is done (Result above); the item stays `wip`. **The phone measurement of the frame cost against
B-141 waits for the owner's word to use the phone**: no adb and no device were used in this iteration, so the third AC
stays open. What the run would have to measure, so it is ready when the word comes:

- **What changed in the cost.** With a menu or a tooltip open, its popup now draws the Tape too, on every frame the
  Tape moves (the band and the head's noise, B-136), in a layer of its own around the popup's content (B-141's
  structure), composited over an offscreen layer of the popup plus 28 dp. With no popup open nothing changed but one
  `onGloballyPositioned` on the Tape's node. While a dialog, a sheet or a drawer is open the held clock now asks for
  frames, which it did not before, and the Tape draws on each: the screen behind a dialog is no longer idle.
- **Where.** In Chrome on the phone (wasm) a popup draws on the window's canvas, as on desktop. On Android a popup is a
  window of its own: its own `ViewRootImpl`, its own render-thread frame, and that cost is not in Chrome's numbers. So two
  runs, on the same Pixel 6a, on mains, the temperature and the big cores' current and maximum frequency logged, as
  B-140 and B-141 did.
- **Run 1, Chrome, B-73's bench** (`scripts/frame-bench-browser.mjs`): a new scene `ComponentsMenu` in
  `sample/src/wasmJsMain/…/BenchScene.kt`, the Components page with the dropdown's menu opened at load and left open,
  and `ComponentsDialog`, a page with a dialog open (the Demo page's Settings phone with its dialog open is the nearest
  existing one, `DemoSettings`), each against the same scene on `main`'s kit (built in a worktree, as B-141 did), interleaved as `~b`, 6 × 10 s, round 1
  discarded, with `ComponentsRest` and the `Control` scene over `ComponentsReduce` as B-141 ran them. Proposed for the
  pre-registration, to be written into this item before the first number: held if `ComponentsMenu`'s work p50 on this
  branch is within 2 ms of `main`'s in every counted round and `ComponentsRest`'s within 1 ms (nothing changed with no
  popup), and the control reads 10 ± 3 ms in every counted round (a run outside it is void); `ComponentsDialog` is
  reported, not held, as it is now expected to cost a Tape's frame where `main` drew nothing.
- **Run 2, Android, B-49's host** (`bench-android`, `BenchActivity`): a new scene `TenCards8Menu`, `TenCards8` with a
  `MutantMenu` open over the Tape, against `TenCards8` on the same build and against the same scene on `main`'s kit
  (the scenes added in a worktree of `main` too, as B-141 built `main`'s arm); per-frame metrics of both windows, and
  missed vsyncs. `FrameMetrics` listens on the activity's `Window`; Compose's popup is a view added to the
  `WindowManager` with no `Window` of its own, so its frames are to be read from `dumpsys gfxinfo <package>
  framestats`, which lists each view root (to be checked on the device first: nothing here ran it). Proposed: held if
  the main thread's work p50 with the menu open is within 2 ms of `main`'s and no more vsyncs are missed than `main`
  misses, in every counted round.
- **Not to forget** (from the skill and B-140): close any leftover bench tab before choosing the tab; a test needing the
  device is never chained with the mutants after it; the body shader is not touched here, so B-167's precision check is
  not needed.

### Pre-registered before the run, 2026-11-25

The owner gave the word to use the phone for this item's measurement in chat on 2026-11-25. What follows is fixed
before the first number; after it, nothing here moves, and a number on the wrong side of a line is a result.

**Stand.** The owner's Pixel 6a (`bluejay`, serial `29131JEGR05148`), on USB and mains, screen on; the driver is this
Mac. Arm `a` is this branch's head, arm `b` is `main` (`dc97cf8`), built in a worktree of `origin/main` with the same
bench scenes added there on a local scratch branch, never pushed. Every log records the phone's battery temperature
and the big cores' (cpu6) current and maximum frequency before and after the run, and after every scene. A phone above
38 °C at the start of a run gets a cool-down first, logged. Any leftover bench tab in the phone's Chrome is closed
before a run (the script logs it).

**Run 1, Chrome on the phone, B-73's bench** (`scripts/frame-bench-browser.mjs`, `DEVICE=1`, `DIST_B=` `main`'s
`wasmJsBrowserDistribution`): the scenes `ComponentsMenu`, `ComponentsDialog`, `ComponentsRest`, `ComponentsReduce`
and `Control` (10 ms busy-waited in every frame over `ComponentsReduce`), each in both arms, interleaved as `~b`, 6 rounds
× 10 s, round 1 not counted, each scene's first second not counted. Log: `logs/b-197/2026-11-25-pixel-ab.log`.

- `ComponentsMenu`: the resting Components page (`ComponentsRest`'s settings) with a `MutantMenu` of the sample's
  menu items opened at load, standing over the page's Tape 24 dp from the page's start and 200 dp from its top, and
  left open. The menu sits in an outer `MutantTheme` around the sample, so it finds the page's Tape (an inner theme keeps
  the outer one's Tapes); the outer theme's clock drives only the menu, in both arms.
- `ComponentsDialog`: the Demo page with the Settings phone shown, its erase dialog open at load (the sample's own,
  B-174). The Components page's dialogs stand on stages whose clock the page's Tape does not read, so only a phone of
  the Demo page has a Tape on the clock a dialog holds.
- **Held**, per counted round (2–6), work p50 of arm `a` minus arm `b`'s (the script's `~b minus a` lines, sign
  reversed):
  - `ComponentsMenu`: within 2.0 ms either way (|a − b| ≤ 2.0);
  - `ComponentsRest`: within 1.0 ms either way (|a − b| ≤ 1.0): nothing changed with no popup but one
    `onGloballyPositioned`.
- **Void** (and then rerun, the item saying so): the control's work p50 over `ComponentsReduce`'s outside 10 ± 3 ms in
  any counted round of either arm; or the run cut short, so that a counted round lacks one of the held scenes in either
  arm.
- **Reported, not held**: `ComponentsDialog` in both arms (on `main` the phone under the dialog drew nothing on its own,
  so `a` is expected to cost a Tape's frame there); frames a second and the busy share of every scene.

**Run 2, Android, B-49's host** (`bench-android`, release with R8, compiled `speed-profile`). Arm `b`'s APK is the same
harness built at `main` with the application id suffixed `.main`, so both stand installed at once; the driver
launches one scene per launch (force-stop, start, one scene for 10 s, the first second not counted, its
`metrics.csv` pulled), so arms of two builds can alternate within a round. Rounds 6, round 1 not counted; within each
round the launches rotate by one through `a:TenCards8`, `a:TenCards8Menu`, `b:TenCards8Menu`, `b:TenCards8`.

- `TenCards8Menu`: B-49's `TenCards8` (ten cards under the Tape at 8 ticks a second) with a `MutantMenu` open over the
  Tape at load, 40 dp from the screen's start and 200 dp from its top, on the screen's clock, left open.
- **The popup's window.** `FrameMetrics` reads the activity's window only; the menu's popup is a view root of its own.
  During every launch of every arm, the driver reads `adb shell dumpsys gfxinfo <package> framestats` once a second
  (the same perturbation for every arm) and keeps each view root's frames, deduplicated by their intended vsync. A view
  root's main-thread work for a frame is `SyncQueued − AnimationStart`, which is what `FrameMetrics`'
  animation + layout/measure + draw spans.
- **The reader's control**: the activity window's frames read from `gfxinfo` must give the same main-thread work as
  `FrameMetrics` gives for the same intended vsync, within 0.1 ms, for at least 99 % of the frames matched in every
  launch; otherwise the popup's numbers from `gfxinfo` are not reported as a number.
- **The main thread's work for a frame** is the activity window's work (`FrameMetrics`) plus the popup window's work at
  the same intended vsync (0 where the popup drew nothing at that vsync): both traversals run on the one UI thread in
  one Choreographer frame.
- **Missed vsyncs** are read on the activity's window, which draws every vsync in every scene (a layer reads the frame
  time in its draw): gaps between consecutive `VSYNC_TIMESTAMP`s over 25 ms, counted per launch after its first second.
- **Held**, per counted round: `TenCards8Menu`'s main-thread work p50 on arm `a` within 2.0 ms of arm `b`'s
  (|a − b| ≤ 2.0), and arm `a`'s missed vsyncs in `TenCards8Menu` no more than arm `b`'s in the same round
  (a ≤ b, a count).
- **If `gfxinfo` lists no separate view root for the popup**, or the reader's control fails, the held work condition is
  read on the activity window's `FrameMetrics` alone, and the item says that the popup's own frames were not measured.
- **The control, before the arms**: B-49's `Spin5` (5 ms busy-waited in a layer nested in another, over `TenCards12`)
  against `TenCards12`, arm `a` only, the same driver, 6 × 10 s, round 1 not counted, with HWUI's performance hints off
  (`setprop debug.hwui.use_hint_manager false`; ADPF moves a UI thread with long frames to the big cores and the control
  reads low, field-ui B-57), the property's value before and after logged and restored. The control reads `Spin5`'s
  activity-window work p50 over `TenCards12`'s within 5 ± 1 ms in every counted round; outside it, Run 2 is void. The
  arms run with the hints as the phone ships them, as a user gets them.
- **Void** (rerun, the item saying so): the control outside its band; or a run cut short, so that a counted round lacks
  `TenCards8Menu` in either arm.
- **Reported, not held**: `TenCards8Menu` against `TenCards8` on the same build, in both arms (what an open menu costs);
  the popup window's frames a second and its work p50 per arm; arm `b`'s `TenCards8` against arm `a`'s; `TOTAL` is not
  read for any verdict (an empty screen reads 28 ms of it, B-49).
- Logs: `logs/b-197/2026-11-25-android-control/`, `logs/b-197/2026-11-25-android-ab/` (every launch's `metrics.csv`
  and `gfxinfo` dumps), and a `.log` of each run's driver output beside them.

**Not held anywhere**: TOTAL, the frames a second, and anything of the tooltip (it opens on hover or a long press, and
the menu stands for the popup's cost).

**Amended 2026-11-25, before either run, from looking at the new scenes** (`device-bench.sh show TenCards8Menu` on
both builds, and the two wasm scenes opened once in the phone's Chrome; no held scene was timed):
- `ComponentsDialog`'s page is scrolled 420 dp at load: at the top, the Settings phone's dialog stood below the phone's
  viewport (411 × 784), and only the phone's head showed. Scrolled, the dialog stands in view with the Tape's band over
  it.
- **`gfxinfo` lists the popup as a view root of its own** («Всплывающее окно», the phone's word for a popup window,
  beside the activity's `ViewRootImpl`), with its own 120 rows of framestats. But the two roots of one Choreographer
  frame **share** `AnimationStart` and `PerformTraversalsStart`, and the popup's `SyncQueued` comes after the
  activity's (eight frames read: the activity's sync queued 0.8–8.2 ms after its vsync, the popup's 0.2–2.8 ms later).
  So the popup's `SyncQueued − AnimationStart` already holds the activity's work, and the sum written above counts the
  activity twice. **The main thread's work for a frame is therefore read as the latest `SyncQueued` among the roots
  that drew at that vsync minus their shared `AnimationStart`**; on a frame the popup did not draw it is the activity's
  `FrameMetrics` work, exactly. The thresholds, the rounds and the arms do not change. As this correction could move a
  verdict towards green, the literal sum (the activity's work plus the popup's `SyncQueued − AnimationStart`) is
  published beside it. The popup's own draw, reported, is its `SyncQueued − DrawStart`.
- The same look showed what the change does there: in about two seconds with the menu open, the popup's root rendered
  117 frames on this branch and 18 on `main` (the menu's own ticks), while the activity rendered 117 and 134.
