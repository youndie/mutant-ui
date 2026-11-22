# B-168 — snackbar: `MutantSnackbar`

Group 11 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-snackbar` from `b185aa1`,
2026-11-22, on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand, 4 ms of real time between
frames so the bodies built ahead are ready).

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-snackbar/` (not in the
repository). A sheet is a strip of frames 48 ms apart (16 ms apart for an interruption). Each `.txt` beside it has one
line per 16 ms frame with the bar's top as drawn (the first row mostly in the palette's `noSignal`, dp) and the pixels
changed since the last frame.

- **The sample as it composes the snackbar** (`MutantUiSample(SampleSettings(group = Communication))`, the page
  scrolled through its `ScrollState` to the section's «Try it»), each tap a mouse press held 32 ms:
  - «Show the snackbar», then UNDO (`try-<look>-show`, `try-<look>-undo`);
  - UNDO pressed 64 ms into the ride in (`try-<look>-undo-rising`);
  - «Show the snackbar» pressed 32 ms into the ride out (`try-<look>-show-leaving`);
  - in «Эфир», «Копия», density 2, reduce motion, `steppedMotion` and «Чистый сигнал».
- **The Demo's Recording screen** (`DemoScreenFrame(DemoScreen.Recording, …)`, as `DemoScreensTest` composes it), its
  bottom 160 dp: the snackbar riding in as the screen opens, UNDO, then «Queue» (`demo-<palette>-appear`, `-undo`,
  `-queue`), in both palettes.
- **The snackbar alone** (`core-`), 328 dp wide in a 360 × 200 host:
  - in, out and in again (`core-<look>-in`, `-out`, `-again`) in «Эфир», «Копия», both at density 2, reduce motion,
    `steppedMotion`, «Чистый сигнал», and the browser's way of drawing in both palettes (`LocalMutantFirstFrameWaits`
    false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true, as `FillChangeOnGpuTest`; the second entrance
    takes its body back from the pool);
  - hidden 48, 96, 144 and 192 ms into its ride in, and shown again at the same points of its ride out
    (`core-hide-at-<ms>`, `core-show-at-<ms>`, `core-interrupted.txt`);
  - its message changed while it stands (`core-swap`), and two snackbars at once (`core-two`);
  - a long message, a long action and no action at density 2 and in «Копия», with a mouse press held on the action
    (`core-long-*`);
  - the keyboard: Tab to the action, Enter, then Enter, Tab and a click while it leaves, and Tab and Enter once it is
    gone (`core-keyboard.txt`, `core-focus.png`).

## Findings

### F-snackbar-01 — a snackbar turned back mid-ride jumps to the far end of the other ride — local

- **What.** `SnackbarRide.follow` restarts the ride from its start whenever `visible` changes, wherever the bar stands:
  - hidden while it rides in, the bar jumps up to its place in one frame, then rides down. In the sample, UNDO pressed
    64 ms in: 28 dp below its place, then at its place, then down (`try-efir.txt`, `try-efir-undo-rising` 64 → 96 ms:
    top 119 → 91 → 103). On the bar alone, hidden 48 ms in: 37 dp in one frame (`core-interrupted.txt`, `hide-at-48`
    54 → 17);
  - shown again while it leaves, it jumps down to the start of its entrance, its height and 8 dp below. In the sample,
    32 ms into the ride out: 12 dp below its place, then 57 dp (`try-efir-show-leaving` 32 → 64 ms: 103 → 148). On the
    bar alone, shown 48 ms in: 23 → 56 dp (`show-at-48` 40 → 73);
  - under `steppedMotion` the jump is the whole ride: UNDO pressed before the first step (the bar still all the way
    down) stands it at its place for a step, then it steps down again (`try-stepped-undo-rising` 64 → 96 → 192 ms:
    148 → 91 → 136).
  Density 2, «Копия» and «Чистый сигнал» are the same (`try-efir-d2.txt`, `try-kopiya.txt`, `try-clean.txt`).
- **The design.** The brief's `osd-in` / `osd-out` say nothing of an interruption. B-26 designed the two rides
  (D15); a ride that starts from where the other left the bar keeps both its numbers and removes the jump. The
  brief's class "geometry growing from the wrong side" / "a frame of the wrong state".

### F-snackbar-02 — the sample's «Try it» does not hide what has ridden off — reported (the sample)

- **What.** `MutantSnackbar`'s KDoc leaves hiding the ride to its holder ("Whatever holds it at the screen's bottom
  hides what has ridden off"). The section's «Try it» holds it in a 56 dp `Box` that does not clip. So the bar rides
  in from 56 dp below over the section's bottom edge and the band between sections, and rides out there and vanishes
  whole in one frame, 56 dp below its place (`try-efir-undo.png`, 208 → 256 ms; `try-efir.txt`, `try-efir-undo` 208
  → 224: top 148 → none, 20 034 pixels changed in the frame).
- **Where.** `sample/…/ComponentsCommunication.kt` (`Snackbars`): a clip on the holder, or a holder at the bottom of a
  clipped stage.

### F-snackbar-03 — on the Recording screen 10 dp of the bar stays on screen at the ride's far end — reported (the sample, B-26's number)

- **What.** B-26 rides the bar from its own height and 8 dp below its place. The Recording screen stands it 18 dp
  above the phone's bottom (`DemoRecording.kt`, `padding(bottom = 18.dp)`), so at the far end 10 dp of the bar and its
  drips stay inside the screen:
  - as the screen opens, the first frame shows that 10 dp strip at the bottom edge (`demo-efir-appear.png`, 16 ms);
  - after UNDO it rides down to the strip, holds there from about 160 ms, and the strip vanishes in one frame at
    224 ms (`demo-efir-undo.png`, 160–208 ms against 256 ms; «Копия» the same).
- **The item that decided it.** B-26 (its 8 dp, D15). Either the demo's 18 dp or the ride's 8 dp; not re-decided
  here.

### F-snackbar-04 — the action has no pressed look — reported (a look the design does not draw)

- **What.** The action is a `clickable` with `indication = null`, and nothing else reads its press: a mouse press held
  48 ms on «ОТМЕНИТЬ» changes no pixel of the action against the frame before it (`core-long-efir-d2.png`,
  `core-long-kopiya.png`, frames 656–704 against 640). Keyboard focus draws the brackets (`core-focus.png`).
- **The design.** The page draws the snackbar at rest only (README: "rest"); B-26 decided no press look by leaving
  it out. The same question as F-buttons-05 for the text button, here for a word on the blue bar — owner.

### F-snackbar-05 — the action taken from the keyboard leaves focus nowhere — reported (owner)

- **What.** Tab reaches the action and draws the brackets; Enter fires it once. The host hides the snackbar, the
  action stops being focusable as its leaving starts, and focus is cleared: no node is focused while it leaves or
  after it is gone (`core-keyboard.txt`). In a window the next Tab starts again from the first focusable.
- **The design.** Nothing; cmp-ui-component §9 asks an overlay to return focus to its opener or to say in its KDoc that
  focus is left to the window. A snackbar has no opener: whether it takes a `returnFocusTo` or the KDoc says so is
  the owner's call.

### F-snackbar-06 — a new message in a standing snackbar is a cut; two snackbars share one body — reported (owner)

- **What.**
  - a message changed while the bar stands swaps its words in one frame, the body unchanged (`core-swap.png`, 48 →
    64 ms). B-26 left a queue to the host; the KDoc does not say a new message is a new snackbar (hide, then show,
    or a new `key`);
  - two snackbars shown at once on the default `key = "snack"` draw the same body: the same outline, drips and
    shadow, boiling in step (`core-two.png`). A face of F-card-cocoon-04 (constant default keys), nothing new.

### F-snackbar-07 — B-26 says the brief gives nothing of the ride's path; its curve column says `lock` — reported (owner)

- **What.** B-26: "the brief gives the durations and nothing of the path", and it chose the cube, eased out. The
  brief's event table names the curve for `osd-in` / `osd-out`: `lock`, "exponential decay with one overshoot", and
  the ride "со срезом" and "оставив след" (B-26 lists the trace as not drawn; the slice is not drawn either). The
  numbers stand (B-26); the item's sentence about the brief is not quite right.

## Looked at and found right

- **The ride.** In 240 ms, out 200, eased by the cube: on every look the bar rises from 56 dp below its place to it
  and leaves the composition at 224 ms after a 200 ms ride out (`core-efir.txt`). Under `steppedMotion` its steps show
  at 112, 192 and 272 ms (the frames after 83, 167 and 250). Under reduce motion it stands at its place in the change's
  frame and goes in the next, its body still (`core-reduce.txt`, `try-reduce.txt`).
- **The browser's way.** The second entrance takes its body from the pool and draws it from the first frame
  (`core-efir-web-again.png`). The very first frame draws the stand-in, a flat rectangle without the slime's edge
  (`core-efir-web-in.png`, 16 ms): F-buttons-03's face, one frame, nothing new.
- **Density 2** draws the same strips at twice the pixels (`core-efir-d2-*`, `try-efir-d2-*`).
- **Long words** (B-70). At density 2 a long message ends in an ellipsis before the action; a 39-character action ends
  in an ellipsis within half the bar (`core-long-efir-d2.png`). No mosaic, drip or flow line enters the words.
- **What a hidden snackbar answers: nothing.** Composed hidden, nothing is composed. Leaving, the action is disabled:
  Enter, a click on it and Tab do nothing, no node is focused (`core-keyboard.txt`); its message stays a polite live
  region for the 200 ms of the ride. Gone, no node with its words is left, `onHidden` ran once, and Tab and Enter do
  nothing.
- **Keyboard focus** draws the field's brackets around the action (`core-focus.png`).

## Fixed

### F-snackbar-01 — `f5154ea`

- **The cause.** `SnackbarRide.follow` set only the ride's start time on a change; `out` was a function of the new
  direction and its progress alone, so a new ride began at the far end of the way.
- **The fix.** The ride remembers how far below its place the bar stood when it began (`from`, the old ride's
  position at the moment of the change) and runs B-26's curve over what is left: in, `from · (1 − e)`; out,
  `from + (1 − from) · e`, with `e = 1 − (1 − p)³` over the same 240 and 200 ms. A ride from rest or from nothing has
  `from` 1 or 0 and draws as before, so no golden or parity cell moves. Under `steppedMotion` the new ride holds where
  the bar stood until its first step, 83 ms after the change; under reduce motion both still land at once.
- **The kept test.** `SnackbarBehaviourTest.turned back half-way, it rides back from where it stands, never jumping
  to the far end` reads the bar's top as drawn on every frame through an entrance turned back after three frames and
  a leaving turned back after three more: no frame moves it more than 13 dp (a whole ride's first frame moves it 11
  or 12), it rides down after the hide and up after the show, and it ends at its place with `onHidden` never called.
- **After.** On the bar alone, hidden 48 ms in: 54, 46, 52, 57 … 73 dp below its top line, then gone; shown 48 ms into
  its leaving: 40, 49, 43, 38 … 17 (`core-interrupted.txt`). In the sample, UNDO 64 ms in: 119, 107, 116, 124 … 148,
  gone at 304 ms; shown again 32 ms into the leaving: 103, 122, 116, 112 … 91 (`try-efir.txt`; density 2 the same,
  `try-efir-d2.txt`). Under `steppedMotion` a hide before the first step leaves the bar where it stood, below, until
  the ride ends (`try-stepped-undo-rising`). The frame of each change stands where the ride it leaves had come to,
  so the turn shows one frame late, as every frame of the ride is drawn from the composition of its frame
  (`try-efir-undo-rising.png`). F-snackbar-02's vanish at the far end stays.
- **Mutants**, each run against `SnackbarBehaviourTest` and read from its JUnit XML:
  - the reversed ride from the far end again (`from = if (visible) 1.0 else 0.0`): killed, the kept test's "no jump"
    (`[57, 47, 38, 1, 13, 24, 57, …]`);
  - the leaving ride ignoring where it began (`else eased`): killed, the same assertion (`[…, 38, 1, 13, …]`).

## Gate

`./gradlew :mutant-core:desktopTest :sample:desktopTest :mutant-core:ktlintCheck :sample:ktlintCheck --continue` on
this Mac at `f5154ea` with the scratch tests deleted: exit 0; the JUnit XML holds 427 tests in `mutant-core` and 22 in
`sample`, no failure, error or skip. No public API changed (`SnackbarRide` is internal), so no `updateKotlinAbi`.

## Not verified

- The phone and the browser itself: the browser's way was drawn through the desktop seams only, and the body shader
  on a phone's GPU (B-167) not at all.
- A real window: the mouse and keyboard came from the test harness. Whether a screen reader announces the polite
  region again when a snackbar turned back mid-leave rides in again was not checked.
