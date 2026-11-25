---
id: B-197
title: "The Tape over menus, tooltips and dialogs"
status: done
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

Done. The first, second and fourth AC hold on desktop; the third, the phone's frame cost, was measured on 2026-11-25
(The run, below): held on Android, red in the phone's Chrome by a two-level mode that falls on either build. The owner
closed it as measured, the reds recorded (Decision, at the end), and the mode became
[B-211](B-211-two-levels-of-frame-work-in-the-phones-chrome.md).

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
| the phone | measured 2026-11-25 (The run, below): Android held, Chrome red; closed as measured by the owner (Decision) |

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

### The run, 2026-11-25

The Pixel 6a on mains, charging from 27 % through both runs, so it ran warm: 34.7 °C at Run 1's start, 39.0 at its
end. Before Run 2's control it was given the driver's longest cool-down, 900 s, and still read 39.4 °C, and the control
ran at that; before the arms it cooled to 37.7 °C in 780 s. Both are logged (it was charging at up to 3 A; at one point the phone's Chrome stood in front after Run 1 with a page drawing (not a bench tab), its renderer
at 72 % of a core, and the Home key was pressed, no tab touched). Before Run 1 the script closed four leftover bench tabs
of an earlier session (`127.0.0.1:8768/?scene=DemoLibrary&…`), logged. Neither run was cut short, none is void, and
nothing was rerun.

**Run 1, Chrome 158 on the phone** ([log](../../logs/b-197/2026-11-25-pixel-ab.log), `frames.csv` beside it), arm `a`
at `592b80c`, arm `b` `main` at `dc97cf8` with the same scenes, interleaved as `~b`, viewport 411 × 728 at 2.625×; same
host, same run. Work p50 in ms per counted round:

| round | 2 | 3 | 4 | 5 | 6 |
|---|---|---|---|---|---|
| `ComponentsMenu`, `a` | 5.7 | 5.8 | 5.5 | 6.1 | 9.9 |
| `ComponentsMenu`, `b` | 5.4 | 5.3 | 5.5 | 6.2 | 6.1 |
| a − b (held: within 2.0) | +0.3 | +0.5 | 0.0 | −0.1 | **+3.8** |
| `ComponentsRest`, `a` | 7.4 | 5.5 | 5.6 | 6.0 | 5.9 |
| `ComponentsRest`, `b` | 5.4 | 9.2 | 8.9 | 6.3 | 9.9 |
| a − b (held: within 1.0) | **+2.0** | **−3.7** | **−3.3** | −0.3 | **−4.0** |
| `Control` over `ComponentsReduce`, `a` | 8.4 | 8.3 | 8.3 | 8.2 | 8.2 |
| the same, `b` | 8.6 | 8.4 | 8.4 | 8.4 | 8.4 |
| `ComponentsDialog`, `a` (reported) | 9.2 | 8.9 | 9.0 | 9.1 | 9.0 |
| `ComponentsDialog`, `b` (reported) | 8.8 | 8.9 | 8.7 | 8.9 | 8.8 |

- **`ComponentsMenu`: RED.** Within 2.0 ms in rounds 2–5 (−0.1 to +0.5), outside it in round 6 (+3.8).
- **`ComponentsRest`: RED.** Outside 1.0 ms in four of five counted rounds, three of them with `main` the slower
  (−3.3 to −4.0) and one with this branch the slower (+2.0).
- **The control** reads 8.2–8.6 ms in every counted round of both arms, inside 10 ± 3: the run is not void.
- Reported: `ComponentsDialog` costs the same in both arms (a − b from 0.0 to +0.4 ms, busy 58–61 % in both): on this
  page the dialog's phone runs beside a header whose bodies move anyway. Every scene of both arms held 61.2–61.9 frames a
  second; no long animation frame in a held scene's counted round.
- **What the reds are made of** (`frames.csv`, the median work of each second): every held scene of both arms sits at
  one of two levels for whole seconds, about 5–6 ms or about 9–10 ms. `a`'s `ComponentsMenu` sat at the upper level for
  all of round 6 (9.5–10.5 ms each second) and at the lower in rounds 2–5; `b`'s `ComponentsRest` sat at the upper level
  for all of rounds 3, 4 and 6; `a`'s `ComponentsRest` moved from the lower to the upper six seconds into round 2. In
  the rounds where both arms of `ComponentsMenu` stayed at the lower level the difference was −0.1 to +0.5 ms. The upper
  level fell on `main` where nothing changed (`ComponentsRest`) as often as on this branch, so it is not the arm; what
  puts a scene there was not measured (which core ran Chrome's renderer is the first thing to read; not done). That is a
  hypothesis, and the reds stand as measured.

**Run 2, Android: the control** ([log](../../logs/b-197/2026-11-25-android-control.log), every launch's files in
[the directory](../../logs/b-197/2026-11-25-android-control/)), arm `a`, HWUI's hints off for the run (the property was
`true` before and is `true` again, logged). The big cores were capped as low as 984 MHz by its end (38.5 °C).
`Spin5`'s work p50 over `TenCards12`'s per counted round: 4.43, 4.90, 4.97, 5.09, 5.12 ms, **inside 5 ± 1**: Run 2 is
not void.

**Run 2, Android: the arms** ([log](../../logs/b-197/2026-11-25-android-ab.log), every launch's `metrics.csv`,
`gfxinfo` dumps and logcat in [the directory](../../logs/b-197/2026-11-25-android-ab/); read by
`scripts/device-bench-ab.py read`), arm `a` at `592b80c`, arm `b` `main` at `dc97cf8`, HWUI's hints as shipped,
36.8–37.6 °C; same phone, same run, launches interleaved. In ms per counted round:

| round | 2 | 3 | 4 | 5 | 6 |
|---|---|---|---|---|---|
| `TenCards8Menu` main-thread work p50, `a` | 1.17 | 1.21 | 1.20 | 1.22 | 1.28 |
| the same, `b` | 0.72 | 0.75 | 0.77 | 0.80 | 0.83 |
| a − b (held: within 2.0) | +0.45 | +0.46 | +0.43 | +0.42 | +0.45 |
| the literal sum's a − b (published beside it) | +1.14 | +1.18 | +1.13 | +1.15 | +1.21 |
| missed vsyncs, `a` / `b` (held: a ≤ b) | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| the popup's frames a second, `a` / `b` | 52.6 / 7.0 | 60.1 / 8.0 | 60.0 / 7.9 | 59.0 / 8.0 | 52.6 / 7.0 |
| the popup's draw p50, `a` / `b` | 0.18 / 0.63 | 0.19 / 0.63 | 0.16 / 0.61 | 0.18 / 0.65 | 0.18 / 0.67 |
| the activity window alone, `a` / `b` | 0.73 / 0.72 | 0.72 / 0.75 | 0.73 / 0.77 | 0.73 / 0.80 | 0.80 / 0.83 |
| `TenCards8`, `a` / `b` (reported) | 0.77 / 0.74 | 0.81 / 0.77 | 0.79 / 0.78 | 0.81 / 0.83 | 0.93 / 0.88 |

- **`gfxinfo` and the popup's window**: every launch with the menu open listed the popup («Всплывающее окно») as a
  view root of its own beside the activity's. **The reader's control holds**: of the activity's counted frames, the
  `gfxinfo` dumps held 531–542 of 531–542 per launch (at least 98.2 %; a frame between two dumps is not seen, and a popup
  frame there counts as none), and every one matched gave `FrameMetrics`' work within 0.1 ms.
- **Main-thread work: held.** With the menu open this branch costs the main thread 0.42–0.46 ms a frame more than
  `main` at p50, in every counted round, against the 2.0 written; by the literal sum, 1.13–1.21 ms, inside it too.
- **Missed vsyncs: held.** None in either arm in any counted round (the activity drew 59.8–60.2 frames a second).
- What it costs, reported: on `main` the popup's window drew at the menu's own ticks, 7–8 times a second; on this branch
  on every frame, 52.6–60.1 a second, each draw cheaper (0.16–0.19 ms against 0.61–0.67: a Tape frame replays the
  popup's layer). The main thread's p95 with the menu open is alike in both arms (8.96–9.30 against 8.87–9.69 ms); the
  ticks set it. `TenCards8` with no popup reads alike in both arms (a − b from −0.02 to +0.05 ms).

**Against B-141.** B-141 measured `ComponentsRest` in Chrome 157 on 2026-11-19 at a viewport of 411 × 784; this run
is Chrome 158 at 411 × 728, so its absolute numbers are not set beside B-141's: each comparison here is this branch
against `main` in the same run, as B-141's was.

**Verdict.** Android holds every held condition. Chrome does not: `ComponentsMenu` is red in one counted round of five
and `ComponentsRest`, where this change adds one `onGloballyPositioned` and no draw, is red in four, both by the
two levels above, which fell on `main` as often as on this branch. By the pre-registration that is red, and it stays
red: the third AC is not ticked and the status stays `wip`.

## Question

**Measured, 2026-11-25:** on the phone's own windows (`bench-android`), an open menu over the Tape now costs the main
thread 0.42–0.46 ms a frame more than on `main` at p50, with no vsync missed in either, and the popup's window draws on
every frame (52–60 a second against 7–8); the reading of that window is checked against `FrameMetrics` and holds. In the
phone's Chrome both held conditions are red, by a two-level mode (about 5.5 against 9.5 ms of work a frame, for whole
seconds) that put `main`'s unchanged resting page at the upper level in three rounds and this branch's menu page in
one. The control held, so the run is not void, and by its own rule it is not rerun.

The choices:
1. **Close the AC as measured, with the reds recorded**: Android held, and Chrome's reds are a mode that falls on
   either arm (the resting page, unchanged here, was slower on `main` in three rounds). The two-level mode becomes an
   item of its own, as B-142 did for the nine-second stall.
2. **A new run for Chrome, pre-registered anew by the owner's amendment** before its first number, with a design that
   can resolve 1–2 ms through the two levels: more rounds and a held reading per level, or the renderer's core pinned
   or read per scene.
3. **Find the two levels first** (which core runs Chrome's renderer per scene, whether ADPF or the phone's heat moves
   it, B-140's "levels and steps"), then run Run 1 again as pre-registered.

## Decision, 2026-11-25

The owner chose the first option in chat: **the AC is closed as measured, the reds recorded.** On Android, where an
open popup is a window of its own and where the new cost lands, both held conditions held; in the phone's Chrome both
are red, and stay red in this item: no threshold moved and nothing was rerun. The two-level mode (about 5.5 against
9.5 ms of work a frame, for whole seconds, on either arm, `main`'s unchanged resting page included) is
[B-211](B-211-two-levels-of-frame-work-in-the-phones-chrome.md), as B-142 was for the nine-second stall. Until it is
understood, a Chrome A/B on the phone resolves no difference under about 4 ms at five counted rounds, so Run 1 says
that the menu does not cost 4 ms or more there, and nothing finer.
