# B-168 — progress: `MutantLinearProgress`, `MutantCircularProgress`

Group 12 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-progress` from `cb83869`, 2026-11-22,
on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand).

The progress has no body: a black track ringed 2 dp, acid segments 6 + 2 dp, the 96 dp `rewind` band, the 36 dp reel
turning 30° a scene tick, all drawn by one `Canvas` each from the page's literals (B-27). So most of the kit's failure
classes (a stale body from the pool, a pour, venom, drips in the clean zone) cannot reach it; what can is geometry,
direction, width and the paused frame.

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-progress/` (not in the
repository).

- **The components alone** (`core/`), a determinate bar at 328 dp with its value ramped by every frame from 0 to 1 over
  two seconds, and the page's indeterminate row (a 272 dp band beside the reel), every 16 ms frame diffed against the
  one before (`looks.txt`), a strip of every third frame (`look-<look>.png`) in these looks:
  - «Эфир» and «Копия»;
  - reduce motion, «Чистый сигнал», `steppedMotion`;
  - right to left;
  - the browser's way of drawing (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies`, `LocalMutantBodyOnGpu`
    true, `LocalMutantBodyAsImage` false);
  - density 2 in both palettes.
- **The band** at 200 and 328 dp, every frame for 4.8 s, its solid columns logged (`band.txt`, `band-w*.png`); paused
  (reduce motion) at every width from 16 to 720 dp in steps of 4 (`band.txt`, `paused-widths.png`).
- **The value**: the text's width at every value 0…32 % (`values.txt`); a value stepped as the sample steps it
  (.42 + .02) to 1, back to 0, and to .5, .2, 1, .999, .995, .004, 0, with the track's end and the fill's end read on
  each (`value-seq.png`, `values.txt`); NaN, −.5, −0, 1.5, ±∞, 1e-9, .99999 (`computed.png`).
- **Widths**: the determinate at 0, 20, 40, 60, 80, 120 and 900 dp, the band at 20 and 900, both in a row with the reel
  and no weight, the reel given 60, 20 and 120 × 36 dp (`widths.png`); a doubled font (`fontscale2.png`); both bars and
  the reel in a horizontally scrolling row (`unbounded.png`, `unbounded.txt`).
- **Reduce motion switched on and off** while the band runs, and the reel every tick for 13 ticks (`pause-toggle.png`,
  `reel-ticks.png`).
- **The sample as it composes them** (`sample/`): the Communication group, scrolled to Linear progress, 2 s of frames
  diffed (`section-<look>-d<density>.{png,txt}`) in «Эфир», «Копия», reduce motion, «Чистый сигнал», `steppedMotion`,
  and density 2 in both palettes; the Recording demo with «Listen» clicked by the mouse, 2 s (`recording-*.png`), and
  the Queue tab of the Library demo, 2.4 s (`queue-*.png`), in both palettes.

What holds and is not a finding:

- The segments are 6 on, 2 off from the track's start at every value and at density 2; a value moving cuts the last
  segment where the page's `width: 42%` would, both ends land (0: no acid; 1: segments to the track's end), a value
  going back is drawn at once (the design has no tween for a value). The track's end stands still while the value
  counts: Handjet's digits are one width, the text 38 dp from `000%` to `100%`.
- NaN and −∞ draw and read as 0, 1.5 and +∞ as 1.
- The band moves exactly 12 dp a scene tick and comes round as the engine's `(fi · 12) % (pw + w) − w`; its seam (a
  tick or four with only the trailing quarters, then the solid half back at the start) is the engine's.
- The reel turns 30° a tick and comes back whole after twelve.
- «Копия», «Чистый сигнал», `steppedMotion` and the browser's way change nothing on the progress: the per-frame diffs
  match «Эфир»'s frame for frame (`looks.txt`), as the page draws it in its literals and the bars have no body.
- In the sample and both demos the bars advance and loop as B-96 set (the copy 1 % every 400 ms, playback 0.4 % every
  250 ms, from 100 % back to 0); the Tape's band passes over them as over everything.

## Findings

### F-progress-01 — right to left, the fill grows from the end and the band runs backwards — local

- **What.** Under `LayoutDirection.Rtl` the determinate row mirrors (the value stands on the left), but its fill still
  grows from the track's left edge, beside the value, toward the start; the band still runs left to right
  (`core/rtl.png`, `core/look-rtl.png`).
- **The item that decided it.** B-65: the kit mirrors in a right-to-left layout (the tabs and the navigation bar were
  made to; the list item, snackbar and top app bar already did). The design draws no RTL.
- **Fix.** Both bars draw their picture mirrored under RTL; the reel, a spool, keeps its turn.

### F-progress-02 — the indeterminate bar is 0 dp wide where no width is given — local

- **What.** In a horizontally scrolling row the indeterminate bar measures 0 × 12 and vanishes (`core/unbounded.txt`:
  `ind 0 x 12`), the determinate one beside it 250 × 23. B-64 fixed this (F14) for the determinate track only.
- **The item that decided it.** B-64: the progress track has no width of its own, so it takes `TRACK_MIN`, 200 dp.
- **Fix.** The indeterminate track takes the same 200 dp when its width is unbounded; bounded, it fills as before.

### F-progress-03 — a reel given another size draws in its box's top-left corner — local

- **What.** Given `Modifier.size(60.dp)` the spool stands in the top-left 36 dp of the 60 dp box; given `size(20.dp)`
  it overflows only right and down; given `width(120.dp)` it stands at the left (`core/widths.png`, the last row).
- **The rule.** cmp-ui-component §9, squeezed or given room: the form keeps its size, centred.
- **Fix.** The reel keeps 36 dp, centred in whatever box it is given. At its own size nothing moves.

### F-progress-04 — a narrow row cuts the value: `042%` reads `04` — local

- **What.** At 20 dp the value is measured into what the row has left and cut to `04` (`core/widths.png`, the second
  row); `100%` would read `10`, another number.
- **The rule.** cmp-ui-component §9: a number grows or shares, never cut.
- **Fix.** The value is always laid out whole and stands past the row's end when the row is narrower than it; the track
  takes what is left, as before.

### F-progress-05 — paused, the band leaves the track empty at some widths — reported (owner: B-27's number)

- **What.** Under reduce motion the band stands at the page's still tick 40 (B-27), `480 mod (w + 96) − 96`. Wherever
  `w + 96` divides 480 or nearly does, it stands before the track:
  - at 24, 64, 144 and 384 dp the track is empty;
  - of the widths 16…720 dp in steps of 4, 29 of 177 show no solid acid, only the 15 % and 40 % trailing quarters:
    28–36, 68–80, 148–168 and 384–432 dp (`core/band.txt`, `core/paused-widths.png`).
  B-27 chose tick 40 because the clock's own paused tick 0 drew "an empty track"; at these widths tick 40 draws one too.
- **Also.** Switched on while running, the band jumps from where the clock stood to tick 40's place (16 dp at 272 dp,
  `core/pause-toggle.png`), and back when switched off; the reel beside it stands where it was. D7 says the clock
  stops; the band alone does not stand where it stopped.
- **Why reported.** The number is B-27's (D15). One way to keep its aim would be a paused position that always shows the
  solid half (say the band's end at the track's middle), or the clock's own tick when it is not 0 — the owner's call.

### F-progress-06 — the value rounds to nearest: `100%` before the bar is full — reported (owner)

- **What.** `(value · 100).roundToInt()`: at .995 and .999 the value reads `100%` with the fill 1–2 dp short of the
  track's end; at .004 it reads `000%` over a sliver of acid (`core/value-seq.png`). A task at 99.5 % reads done.
- **The design.** It prints `042%` for 42 %, nothing about rounding. Flooring (as most progress counters do, so 100 %
  means done) is a decision for B-27's numbers.

### F-progress-07 — the band and the reel step at the scene tick under `steppedMotion = false` — reported (owner: D22)

- **What.** The band moves 12 dp every 125 ms (96 dp/s in 8 jumps) whatever `steppedMotion` says: 1 frame in 8 changes
  (`core/band.txt`, `core/looks.txt` the same in `look-stepped`).
- **The decision.** D22 / B-100: by default movement runs on every frame, "movement means where a body is and how far a
  transition has gone"; B-100's list names the transitions and the venom run, not the stage's `rewind` and `reel`. The
  spec page calls them «линейно, по тикам», the brief «квантование». Whether the band is movement (every frame) or the
  stage's quantised look (its tick) is the owner's word; the kit keeps the tick until then.

## Not verified

- The phone and a real window (not run here, as the pass rules say); Android's and iOS's RTL.
- The browser itself: the browser's seams were applied on the desktop, and the progress draws no body, so nothing of
  B-167's shader reaches it.
