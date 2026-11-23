# B-168 — foundations: the surface, the Tape, the keyframe loss, icons, the sample's header

The foundations group of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-foundations` from
`662fa7a`, 2026-11-23, on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand, 3 ms of real
time between frames so the bodies built ahead are ready). **Report only**: everything this group covers is shared code
(`surface/`, `gen/`, the sample), so nothing is fixed here; the inventory table is untouched.

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-foundations/` (not in the
repository; `core/` for the surface's own scenes).

- **The sample as it composes Foundations** (`MutantUiSample(SampleSettings(…))`, the page scrolled through its
  `ScrollState`), in «Эфир» and «Копия», density 1 and 2, reduce motion, «Чистый сигнал», `steppedMotion` and the
  mosaic past the contour:
  - the whole page in parts (`page-<look>-<n>.png`);
  - each section — Theme, Icons, Tape, Keyframe loss — every frame for 2 s, diffed frame to frame (`motion-<look>-*.txt`),
    a sheet every 48 ms (`motion-<look>-*.png`);
  - the header's switches flipped on a running page — reduce motion on and off, the palette both ways, clean signal —
    every frame for 1.4 s (`switch-*.png`, `.txt`);
  - the page's Tape for 10 s, every frame diffed whole (`tape10s.txt`);
  - the Keyframe loss section's stage through a whole loss and the next one's start, every frame (`loss-*.txt`, sheets
    every 32 ms, `loss-<look>-<n>.png`).
- **The surface alone** (`core/`), at density 2:
  - every state of a card in both palettes, with and without the mosaic past the contour (`states-*.png`), and its clean
    zone read over 48 ticks (`states-*-zone.txt`); the same keys at rest and pressed (`pm-*.png`,
    `pressed-mosaic-zone.txt`);
  - each change of state on a button, every frame (`change-<palette>-<change>.png`, `.txt`);
  - **interrupted transitions**: a pour reversed 48–400 ms in on a chip and a card (`pour-*.png`, `pour-reversed.txt`),
    a slide re-aimed 64–224 ms in (`slide-*.png`, `slide-reaimed.txt`), a melt reversed both ways 48–240 ms in
    (`melt-*.png`, `melt-reversed.txt`);
  - reduce motion switched on mid-pour, mid-slide and mid-press, then off (`reduce-midway.png`, `.txt`);
  - **the browser's way against the desktop's**, the same script frame for frame (a chip toggled and toggled back
    within its pour, a press, focus in and out, disabled and back, a body appearing, loading → success → rest, the
    palette both ways) through `LocalMutantFirstFrameWaits false`, `LocalMutantPoolBodies true`,
    `LocalMutantBodyOnGpu true`, `LocalMutantBodyAsImage` left at its default as the browser has it
    (`browser-<n>.png`, desktop left, browser right; `browser.txt`);
  - the keyframe loss while a body inside its band changes fill, turns disabled and a new one appears, both ways of
    drawing (`loss-change-<way>-<n>.png`, `.txt`).
- **The body shader's source** (`BodyShader.kt`), read for half-precision expressions (B-167).

## Findings

Every finding is **reported**: the causes are in `surface/`, `gen/` or the sample, none in a component's own files.

### F-foundations-01 — a pour reversed past its first half fills what was not yet poured in one frame — reported (`surface/BodyMoves.kt:36-43`)

- **What.** The other half of F-chip-03. A fill changed back while the pour runs past half its time starts the new
  pour from the fill it was pouring in (`pourFrom = old`), so the part of the body still showing the first colour takes
  the second in the reversal's frame, then the new pour comes down over it. A body jumps to a colour it was not
  showing there, which B-94's rule meant to prevent.
- **Frames.** `core/pour-Card-320.png` (a 300 × 88 card, «Эфир», density 2; lime pixels per frame in
  `core/pour-reversed.txt`): reversed at 320 ms, the lime goes 71 489 → 86 644 px in one frame — 15 155 px, 17 % of the
  body, the unpoured bottom turned lime — and only then drains. On the chip (112 × 32) at 256–400 ms it is the drips
  alone (9 666 → 9 781). The first half, F-chip-03, measured here too: a chip reversed at 192 ms loses 9 326 lime px
  (95 % of its full 9 781) in one frame (`core/pour-Chip-192.png`), the card at 256 ms 54 563 px (63 %,
  `core/pour-Card-256.png`). In the browser's way the same (`core/browser.txt`, frames 196–197).
- **The item.** B-94 (the half rule), B-12 (the pour). The engine draws a reversal the same way, so a fix is a departure
  (D20). Class 1 below.

### F-foundations-02 — a melt reversed restarts from the other end — reported (`surface/BodyTransitions.kt:132-137`, `:156-170`)

- **What.** `MeltFollower` restarts its clock at every change, and the progress counts from 0 in the new direction. A
  melt-in reversed before it showed anything draws the whole mark at once and melts it out (F-data-table-03); a
  melt-out reversed mid-way drops what is left of the mark and melts in from nothing.
- **Frames.** `core/melt-in-out-48.png`, `core/melt-out-in-160.png`, counts in `core/melt-reversed.txt` (a 24 dp mark,
  2 304 px at density 2): in reversed at 48 ms, 0 → 2 304 px in one frame; out reversed at 160 ms, 816 → 0; at 240 ms
  192 → 0.
- **The item.** B-12 (`mutantMelt`), B-54. Class 3 below.

### F-foundations-03 — switching reduce motion off replays the transitions the pause had landed — reported (`surface/MutantSurface.kt:780-787`, `:211-215`; `surface/BodyTransitions.kt:132`)

- **What.** Under reduce motion a pour, a slide and a melt land at once (D7), but only because their progress reads
  `instant` as 1; the move itself is not ended, and the clock stood at the moment it was paused. When reduce motion is
  switched off, the progress is read again from the stopped time: a landed pour un-pours to where it stood and runs
  again, a landed slide jumps back mid-run and slides home again. A press released under the pause also keeps its heat
  (the amber) until motion comes back — the heat is a function of the standing clock (`BodyHeat.kt:33-40`).
- **Frames.** `core/reduce-midway.png`, `core/reduce-midway.txt` (a card pouring, a chip sliding and a button pressed,
  reduce motion switched on 144 ms in, the press released under it, then reduce motion off): on — everything lands in
  one frame (lime 62 105 px) and nothing moves after; off — lime 62 105 → 16 543 px in one frame, the pour runs again
  for 270 ms; the chip jumps back over the card and slides home again (`resume+16` … `resume+144`).
- **The item.** D7 («toggles are instant»), B-12. The fix: ending every running move when the pause starts
  (`BodyMoves` forgets `pourFrom` and the slide, `MeltFollower` sets `since` to −∞), so the end of the pause has nothing
  to replay. No golden draws a pause toggled mid-move.

### F-foundations-04 — a pressed body's running mosaic shifts tiles into the clean zone — reported (`surface/BodyStates.kt:83-117`)

- **What.** While a body is hot, `runningMosaic` shifts each edge-mosaic tile along its side by one or two tile sizes
  (`sh`, `d < .5 · heat`) with no regard to the clean zone, so a tile next to the zone is drawn inside it, over where
  the label stands (D5: «mosaic … never enter the clean zone»). The engine does the same
  (`mutant-engine.js:380-391`, the `data-px` tiles), so it is the design's own fault, as D21's was.
- **Frames.** `core/pm-efir-card-3-hot.png` (the zone's frame in cyan, every pixel in it unlike the zone's fill in
  magenta): a magenta-tinted tile inside the zone's right end. Read over 24 ticks (`core/pressed-mosaic-zone.txt`), the
  same seven keys at rest and at heat 1: at rest 0 px in the zone in «Эфир» (but the speckle, which the design spreads
  over the whole body); hot, two of seven keys put a tile in it on every tick (176–186 px past the speckle's 64).
  One key at rest shows 12 px at the zone's bottom-left corner on 8 of 24 ticks (`core/pm-efir-card-6-rest.png`), not
  identified further. The mosaic past the contour (`states-*-past-zone.txt`) changes none of it.
- **The item.** D5, B-11 (the pressed state). The fix: a shifted tile that would meet the clean zone does not shift
  (a departure from the engine, D20; `BodyParityTest`'s pressed cell is read before and after).

### F-foundations-05 — a body that appears or changes inside a running loss's band shows the snapshot for the rest of the loss; a new one only its drips — reported (`surface/MutantKeyframeLoss.kt:146-169`, `:302-341`)

- **What.** The snapshot is taken once, at the loss's start, and every body under the scene is cut out of the band for
  the whole run (900–1 400 ms) and the recovery (375 ms). A body inside the band that changes — a new fill, disabled, a
  press — shows its old picture there until its rows recover; a body composed during the loss (an overlay, a row
  inserted) has nothing in the snapshot, so inside the band it is the screen's ground, and its drips hang under the band
  with no body above them. After the recovery nothing is left torn.
- **Frames.** `core/loss-change-desk-0.png` … `-3.png` (desktop) and `core/loss-change-web-*.png` (the browser's way,
  the same): at 3 904 ms the card's fill turns lime, the left button turns disabled and a third button appears; until
  4 960 ms the card stays navy and the button lime in the band, and the new button is a row of lime drips under the
  band (from `t3936`); at 5 024 ms the rows recover and everything appears at once (25 257 px). The sample's own loss
  cell through a whole cycle (`loss-efir-*.png`, `loss-kopiya-d2-*.png`) cuts only its bodies, keeps the words and
  leaves nothing torn.
- **The item.** B-48 (the snapshot is the engine's way; the engine's canvas would hide a new body too). A choice for the
  owner: a body not in the snapshot (or whose look changed since) is left out of the cut, so it stands over the band,
  undamaged, until the loss ends. The dialog's agent fixed its own face of this locally (an overlay opened mid-loss).

### F-foundations-06 — the sample's Theme specimens ignore the header's switches — reported (sample: `ComponentsFoundations.kt:39`)

- **What.** Each palette's specimen is wrapped in `MutantTheme(colors = colors)`, which takes reduce motion from the
  platform (`null`) and sets clean signal, stepped motion and the mosaic past the contour to off. With the header's
  reduce motion on, the whole page stands but these two cells boil on; stepped motion and the mosaic past the contour do
  not reach them either. The Theme section is the one that says «reduce motion, the clean signal».
- **Frames.** `motion-reduce-section-Theme.txt`: under reduce motion, the Theme section changes by ~9 900 px every
  125 ms while Icons, Tape and Keyframe loss change by 0; `switch-reduce-on.txt`: after the switch, `all` equals
  `section` on every tick (10 135, 9 928, 9 656 …) — only the Theme cells move.
- **The item.** B-09 (the theme), B-99 (the catalogue). The fix: pass the header's `reduceMotion`, `cleanSignal`,
  `steppedMotion` and `mosaicPastContour` into the specimens' `MutantTheme` (read from `MutantTheme`'s locals; the kit
  exposes `reduceMotion` and `cleanSignal` on `MutantTheme` already, the other two would need it).

### F-foundations-07 — the sample draws «Копия» under «Эфир»'s Tape, and the Tape specimen under two Tapes — reported (sample: `MutantUiSample.kt:84`, `ComponentsFoundations.kt:39`, `:85`)

- **What.** The page's Tape (`mutantTape()` on the sample's root) covers every section in the page's palette. The
  «Копия» specimen therefore shows «Эфир»'s black scanlines and white grain over its paper; the design draws «Копия»
  with no scanlines and ink grain (`MutantTape.kt:74-81`). And the Tape section's cell carries a Tape of its own under
  the page's: two sets of scanlines (two dark rows in every three instead of one), two head noises and two bands.
- **Frames.** `crops/kopiya-cell-big.png` (from `page-efir-0.png`): horizontal lines across the paper and white grain.
  `crops/tape-cell-big.png`; the rows inside the card read 140 152 140 140 152 140 … (two dark rows of three), the page
  beside it 73 78 78 73 78 78 (one).
- **The item.** B-101 (the Demo page took the page's Tape off its phones for the same doubling), B-13. The fix: the
  page's Tape does not cover the Theme and Tape specimens (each a screen), as the Demo page does for its phones.

### F-foundations-08 — no body shader expression found that builds a number past 2048 from halves — nothing to fix (`surface/BodyShader.kt`)

- **What was read.** Every `half` in `BODY_SKSL`: the texture reads (`node`, `flowBins.eval`, `flow.eval`,
  `flowSegs.eval`, `still.eval`), the colour uniforms and `col`. A channel becomes a number only through `c5`, `c6`
  and `c8` (`float(v) · 31`, `· 63`, `· 255`, rounded); the words over 2048 (`val`'s `r · 512`, `word`'s `r · 2048`,
  `u16`'s `h · 256`) are summed from those floats. Everything else of half type is a colour or a coverage under 1
  (`over`, `half(cov(…))`, `fillC * clip`). The worst rounding inside the helpers is `c8` at a channel near 1: a half
  keeps 2⁻¹¹ there, × 255 is 0.12, under the 0.5 that would change the rounded value.
- **Not verified.** The phone (none used). One thing outside the question asked: the field and flow textures are
  images in `ColorSpace.sRGB` (`BodyShader.wasmJs.kt:45`, `:117`), read through `RuntimeShaderBuilder.child`; if a
  canvas ever had another colour space, Skia would convert the data on read. Chrome on the Pixel 6a draws in sRGB
  today, and B-167's screenshot shows the words intact.

### Looked at, nothing found

- **The Tape over the page for 10 s** (`tape10s.txt`, 625 frames of the whole 1024 × 768 window): no frame flips the
  picture whole. Between grain ticks the frame changes by 5 300–5 600 px (p10–p50: the head's noise at the bottom),
  while the band passes ~21 900 (p90), and the largest, a grain tick with the band passing, 37 823 px (4.8 %). Under
  `steppedMotion` the Tape section changes only on the 125 ms tick (`motion-stepped-section-Tape.txt`); under reduce
  motion it stands; under «Чистый сигнал» it goes in one frame (`switch-clean-on.txt`).
- **The keyframe loss through a whole cycle**, in «Эфир» at density 1 and «Копия» at density 2 (`loss-*.png`): the band
  cuts the bodies only, the words stay, the cleared blocks show the bodies, the recovery comes from the top in three
  steps, nothing torn after it; the next loss starts 9 s later.
- **Every state** (`core/states-*.png`) and **every change of state** (`core/change-*.png`, `.txt`) in both palettes:
  the cuts between rest, loading, success, error, disabled and focus are the known ones (F-icon-fab-04,
  F-text-field-08); a press shows nothing for up to a hot tick (`core/change-efir-press.png`: frames +16 … +64
  identical, the boil from +80 ms) — F-icon-fab-06's class, class 5 below.
- **The clean zone** over 48 ticks (`core/states-*-zone.txt`): flow lines, dead pixels and the mosaic stay out of it at
  rest, in error and focused, with or without the mosaic past the contour. What does enter is the design's: the
  speckle (over the whole body), the loading bands (across the whole width), the disabled body's snow; and the pressed
  mosaic (F-foundations-04).
- **The browser's way against the desktop's**, frame for frame (`core/browser.txt`, `browser-*.png`): within 104–636 px
  at a tolerance of 40 levels (the shader's residual against paths, B-132) on every frame, but two known ones: the
  stand-in of B-149 for the one frame a body appears in (frame 160: 4 749 px, a plain plate; F-buttons-03), and the old
  body for one frame after focus in and out (frames 81 and 101: the browser changes by ~2 050 px where the desktop does
  not; F-text-field-06). A chip back to its old fill, a press, disabled and back, loading → success → rest and the
  palette both ways draw the same.
- **Density 2** (`page-*-d2-*.png`, `motion-*-d2-*`): the sections scale as a whole; the icons, the Tape and the loss
  hold their sizes in dp.
- **The header's palette switch** (`switch-print-on.png`): the index chips pour from «Эфир»'s navy under «Копия»'s black
  words for ~140 ms — F-chip-02's class, class 4 below.
- **The two orchestrator notes about wall-clock tests** (`DemoTapeOnceTest`, `TallBodyBandTest`) were not run here. A
  likely mechanism for the first, from the code and not proved: on the desktop a resting body draws its tick's image
  rasterised ahead when it is ready and its vectors when it is not (`MutantSurface.kt:842-861`); the two pictures differ
  at the edges, so on a slow runner the page and the phone alone differ wherever one run had the image and the other
  did not.

## Not verified

- The phone and the browser itself: the browser's way was drawn on the desktop through the seams; the shader's
  precision was read, not run on a GPU.
- `DemoTapeOnceTest`'s and `TallBodyBandTest`'s CI flakiness: not reproduced (see above).
- Hover by a real mouse and touch on the sample's header: the switches were flipped through `SampleSettings`, as their
  `onCheckedChange` does.
- The costs named in the classes below: none was measured; each is a reading of the code.

## The reported classes whose cause is in `surface/` or `gen/`

One paragraph per class: the cause, what a fix in shared code would look like, what it would close, and its risk to
the goldens and to B-151's phone budget. Nothing here is built; each is input for the shared-code items after the pass.

**1. A pour reversed — B-94's half rule (F-checks-switch-04, F-chip-03; F-foundations-01 is its other half).** Cause:
`surface/BodyMoves.kt:36-43`. A change of fill while a pour runs restarts the pour from one plain colour: in the first
half (by time, `stepped(…) < .5`) from the pour's own old fill, so what was poured vanishes in one frame; past it from
the fill it was pouring in, so what was not yet poured fills in one frame. Either way the body jumps to a picture it
was not showing. The fix: a reversal pours over the picture the body shows — `BodyMoves` keeps the interrupted pour
(its old fill, its front at the progress it stopped at) and `MutantSurface.kt:790` / `BodyDrawing.kt:168` draw the
new pour's "under" as that two-coloured picture (old fill, the old front's path filled with the interrupted pour's new
fill) instead of one colour; the half-time threshold goes. It would close F-checks-switch-04, F-chip-03,
F-foundations-01, and the faces in the checkbox, radio, switch, chip, segmented and every other pour. Risk: no golden or
parity cell (they draw no reversal); `SurfaceTransitionsTest`/B-94's tests that hold the half rule are rewritten. Cost:
one more path fill in the frames of a reversed pour only, which the shader path already leaves to paths (B-110): none
for B-151's budget at rest.

**2. A slide re-aimed restarts its stretch (F-nav-bar-03, F-pickers-05).** Cause: `surface/BodyMoves.kt:45-52` sets
`slideSince = now` for every new request, and `surface/BodyTransitions.kt:98-117` (`slid`) derives both the offset
(`1 − (1 − p)²`) and the stretch (`sin(πp)`) from that one progress, so the stretch drops to 1 in the re-aim's frame.
The fix: at a re-aim `BodyMoves` keeps where the body is drawn (the old run's remaining offset, `dx · (1 − e(p))`,
added to the new request's `dx`) and the stretch it shows, and `slid` eases the stretch from that value (for example
`max(q(p), q0 · (1 − p))`) rather than from 1. Closes F-nav-bar-03 and F-pickers-05 and any slide a consumer re-aims
(switch thumb flicked twice). Measured here at the surface: a 64 dp chip re-aimed 160 ms into its slide goes from
194 to 137 px wide (density 2) in one frame (`core/slide-160.png`, `core/slide-reaimed.txt`). Risk: no golden (frozen clocks, no re-aim); `MotionEveryFrameTest` unchanged. Cost: none.

**3. A melt reversed starts from the far end (F-data-table-03; F-foundations-02).** Cause:
`surface/BodyTransitions.kt:156-170` (`MeltFollower.follow` sets `since = now` on every change) with `:132-137` (the
progress restarts at 0 in the new direction): a melt-out begun before the melt-in showed anything draws the whole mark,
and a melt-in begun mid-melt-out drops what was left. The fix: on a reversal mid-melt set `since` so the new direction
starts at the complement of the old progress (`since = now − (1 − p) · MELT_IN_MS`), so `q` continues. Closes
F-data-table-03 and the checkbox/radio faces. Risk: none to goldens; the melt tests that start from rest are unchanged.

**4. A palette switch pours every body (F-chip-02, F-card-cocoon-02, F-dialog-09, F-empty-state-04).** Cause:
`surface/BodyMoves.kt:36` compares resolved colours, and `MutantSurface.kt:183` feeds it `fill ?: colors.fillOf(p.fill)`,
so «Эфир» → «Копия» is a fill change for every body on the screen; under a held clock (a dialog) that pour never runs
(F-dialog-09). The fix: `follow` also takes the palette (`colors`, or `colors.print`) and starts no pour when the
palette changed in the same composition — the new fill shows at once, as the engine does on a theme switch. Closes
the four. Risk: none to goldens. Cost: removes a page-wide pour (every body by paths for 300–620 ms on the phone) on
each switch: a gain for B-151.

**5. A press shows nothing for up to a hot tick, and under reduce motion a hover or a press redraws at the hot tick's
time (F-icon-fab-06, F-checks-switch-05, F-buttons-06/F-icon-fab-05/F-pull-to-refresh-03).** Cause:
`surface/MutantSurface.kt:738-745`. The heat is read at `clock.nowMs`, which is not state; the draw is invalidated by
`boil.on` once and then only by the hot tick (`hotTimeMs()` reads `hotTick`), so after the press's frame (heat ≈ 0.05)
nothing changes until the next hot tick, up to 83 ms (frames +16…+64 identical, `change-efir-press.png`). The engine
redraws every frame while the heat changes (its key holds `heat.toFixed(2)`, research §1.10) at the hot tick's time.
Under reduce motion the clock stands, `HeatMachine.at` (`BodyHeat.kt:33-40`) stays at 0, but `hot` is true, so the body
is redrawn at `hotTimeMs()` — another time than `restTimeMs()` — a different boil phase for a frame on hover/press
(F-checks-switch-05). The fix: (a) while `0 < k < 1` read `clock.frame` and recolour only (`boilFill`) per frame, the
shape staying on the hot tick (the frame is memoised by tick in `FrameMemo`; only the look is rebuilt); (b) under a
paused clock use `restTimeMs()` whatever `hot` says; (c) the owner's decision for F-buttons-06: if a press should show
under the pause, `HeatMachine.at` returns its target when the clock is paused (a step, no ramp). Closes F-icon-fab-06,
F-checks-switch-05, and with (c) F-buttons-06's class. Risk: (a) no golden (fixtures pass `heat`); `SurfaceStatesTest`'s
pause test changes only with (c). Cost of (a): a re-recorded picture per frame for 320 ms after a press (paths, no field
build) — a pressed body is already drawn by paths, so the added work is the draw itself; measure on the phone.

**6. A press released into a toggle drops its heat from the old fill (F-checks-switch-03).** Cause:
`surface/BodyDrawing.kt:168` fills the pour's lower part with `from` as it is, while the new fill above is
`boilFill(look.fill, …, kk, tt)` (`MutantSurface.kt:369-371`); `MutantSurface.kt:790` passes `pourFrom` unboiled. The
fix: boil `from` with the same `k` and `t` (`boilFill(pourFrom, colors, k, t)`), as the ground already does
(`MutantSurface.kt:228`). Closes F-checks-switch-03 and the chip's face. Risk: none to goldens (no fixture pours hot);
cost nil.

**7. The browser's stand-in ignores the state and the stroke (F-buttons-03, F-tooltip-05).** Cause:
`surface/StandIn.kt:16-34` draws `look.fill` and `look.ink` whatever `state`, and `MutantSurface.kt:681` calls it before
the Disabled/Success branch. A Disabled or Success body needs no generated instance at all for most of its look (both
are drawn on the slot's rounded rectangle, `BodyStates.kt:268-418`). The fix: pass `state` (and the outline ink) to
`drawStandIn`; for Success draw `drawCleanBody`'s rectangle (it needs only the slot), for Disabled the desaturated fill
with its scanlines and outline (all but the instance's mosaic tiles); for a live body keep the plate. Closes
F-buttons-03, F-tooltip-05 and every disabled body coming back from the pool in the browser. Risk: none on the desktop
(it never draws a stand-in); `FirstFrameOffTheFrameTest`'s stand-in case is extended. Cost: nil.

**8. In the browser, a change of signature keeps the old body for frames (F-text-field-06; F-icon-fab-04 and
F-text-field-08 are its desktop cut).** Cause: `surface/MutantSurface.kt:686-702` (B-162): a new signature never held
is built between frames while the old instance goes on drawing; focus changes the signature through the wear
(`MutantSurface.kt:164`, `slot.calm`, `slot.over`), disabled/success through `still` (`:286`). On the desktop the same
change is a one-frame cut (engine same). The fix, in two parts: (a) build the focus and still variants ahead once the
body is shown (they are the next likely signatures: `keep.hasRecent` would hold them from the first time); (b) for the
cut itself, an owner's decision: a short crossfade or melt between the two instances (draw both for ~160 ms). (a)
closes F-text-field-06 after the first visit; (b) closes F-icon-fab-04 and F-text-field-08. Risk: (a) none to goldens;
it doubles a focusable body's first builds in the browser (between frames, B-161's steps). (b) moves no golden (static
states), costs a second body draw for the crossfade's frames.

**9. A finger starting a scroll boils the body (F-card-cocoon-01).** Cause: `surface/MutantSurface.kt:148-163` heats on
`PressInteraction.Press` at once, and `clickable` off Android emits the press on the pointer's down. The fix: the relay
delays the heat by the tap timeout (100 ms) and drops it on `Cancel` before then, as Material's ripple does for
scrollable containers. Closes F-card-cocoon-01 for every body with an interaction source. Risk: none to goldens; a real
press shows ≤ 100 ms later (on top of class 5's hot-tick delay unless 5 is fixed). Cost nil.

**10. The keyframe loss leaves dividers out of its snapshot (F-list-divider-badge-02).** Cause:
`components/MutantDivider.kt:28` draws through `surface/MutantDivider.kt:16` (`drawTrackingDivider`) in content, with no
`MoshBody` (`surface/MutantKeyframeLoss.kt:115-139`), so the band never takes it and its words-layer stands over the
band. The fix: the divider registers a `MoshBody` (`rememberMoshBody` + `moshBehind`) as a surface does. Closes
F-list-divider-badge-02. Risk: `KeyframeLossParityTest` has no divider; the list's loss frames move (B-48's departure
list shrinks by one). Cost: a divider's strip in the snapshot — negligible.

**11. The Tape does not cover popups (F-menu-10).** Cause: `surface/MutantTape.kt:349-374` draws over its own node's
content; a popup is another root. The fix: the Tape publishes its moment and its root offset through a local, and the
kit's popups (menu, tooltip) draw `drawTape` over themselves clipped to their bounds at their offset in the Tape's
coordinates. Closes F-menu-10. Risk: none to goldens (popups have none with a Tape). Cost: a second Tape draw per frame
while a popup is open (B-141 put the Tape in its own layer; a popup's Tape needs the same).

**12. The pour's front is not exposed to the content (F-pickers-11).** Cause: `surface/MutantSurface.kt:202-230` gives
the content one colour (`LocalMutantBodyGround`, `BodyInk.kt:17`), the fill under the label's middle. The fix: a second
local with the front's path in the surface's coordinates (`gen.pourFront(g, p).toPath()`, already built at `:790`) for
content that is taller than a label to clip its ink into two colours. Closes F-pickers-11. Risk: none to goldens; cost
of a clip on large digits during a pour only.

**13. A parent's `ModulateAlpha` fade does not reach a body (F-pull-to-refresh-01).** Cause:
`surface/MutantSurface.kt:330` — since B-135 every body draws in its own `graphicsLayer()`, and a parent layer with
`CompositingStrategy.ModulateAlpha` applies its alpha to its own draw operations only, not to a child layer. The fix
is at the callers (the one in `components/MutantPullToRefresh.kt:348` on main): fade with the default strategy (or
`Offscreen`); a guard test in `surface/` that a surface under a parent alpha of 0.2 draws at 0.2. Closes
F-pull-to-refresh-01's class. Risk: an offscreen layer per fading overlay for the fade's frames only.
