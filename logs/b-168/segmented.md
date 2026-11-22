# B-168 — segmented: `MutantSegmentedButton`

Group 16 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-segmented` from `2afc3e9`, 2026-11-22,
on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand, 2 ms of real time between frames so the
bodies built ahead are ready).

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-segmented/` (not in the
repository): `before/` as found, `after/` with the fixes. Each sheet is a strip of frames 48 ms apart (16 ms where the
name says so). Each `.txt` beside it has one line per 16 ms frame with:

- the pixels changed since the last frame (`diff`);
- how much of each segment's window band (the rows above and below the label) is the window's lime (`limeN`, %);
- each label's ink, read against a bare twin (`SegmentedBody(hidden = true)`, the same keys, the same clock): which
  colour it is drawn in, the tenth percentile and the minimum of its glyph pixels' contrast against what lies under
  them, and the share of glyph pixels under 3:1 (`LN=<ink>:p10=…:min=…:<3=…%`).

- **The button as the sample composes it** (in a 280 dp cell, `fillMaxWidth`, `MutantTheme`), with the bare twin:
  - a pick across (0 → 2), to the next (0 → 1), back (2 → 0), two options (0 → 1), in «Эфир» and «Копия»;
  - reduce motion, «Чистый сигнал», `steppedMotion`, density 2 in both palettes, the browser's way of drawing in both
    palettes (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true,
    `LocalMutantBodyAsImage` false, as `FillChangeOnGpuTest`);
  - a pick during a run (`repick-1-then-2-at-{96,240,400,608}`, `repick-2-then-0-at-304`), fast picks every 64 ms
    (`fast-0-1-2-1-0`), and by the mouse (`mouse-click-2-right-edge`, `mouse-fast`, picks 112 ms apart);
  - the keyboard (`keys`: focus, Right, Right 96 ms later, Left), right to left (`rtl`, `rtl-keys`);
  - disabled and back at rest (`disable-flip`, every frame) and in the middle of a run (`disable-mid-run`);
  - an index of −1 and one past the end, then a real one (`to-none`, `to-past-end`);
  - narrow widths 0, 4, 12, 40, 96 and 150 dp, long labels left to right and right to left (`long`, `long-rtl`).
- **The sample itself**: `MutantUiSample(SampleSettings(group = Actions))`, the page scrolled through its `ScrollState`
  to the Segmented section, «EP · 6 h» clicked by the mouse, the section captured in «Эфир» and «Копия»
  (`sample/actions-*.png`).

## Findings

### F-segmented-01 — the words change colour on a timer, not with the slime under them: 1.06:1 for half a second — local

- **What.** The labels take their colour from the run's clock (`VenomRun.labelOn`: the old segment's word black until
  340 ms, the new one's from then), while the lime goes where the venom takes it. In «Эфир» the pale words stand on
  the passing lime and the black one on the dark body:
  - a pick across (`before/pick-0-2-efir.txt`): the word passed over («LP · 4 h») pale on lime from 48 to 496 ms, up
    to 97 % of its glyph pixels under 3:1, its tenth percentile 1.06:1; the chosen word pale on the arriving lime
    160–352 ms (62–97 % under 3:1); the word left black on the dark body 288–352 ms (1.81:1), then pale on the lime
    still there until 432 ms;
  - 29 of the run's 45 frames have a word under 3:1 (50 word-frames); a pick to the next segment 17 frames, back 30,
    two options 14, by the mouse 36, by the keys 36; the same under `steppedMotion`, at density 2 and in the browser's
    way. «Копия» stays readable (worst tenth percentile 4.1:1: both inks are dark).
  - The sample shows it as well: `before/sample/actions-efir.png` 112–544 ms.
- **The design.** The page's `bindPicks` recolours the words in a `setTimeout(…, 340)`; the engine draws the same
  frames. The kit's rule since B-91/B-94 and `cmp-ui-component` §4: a word's colour follows the fill as drawn, never
  the logical state; the navigation bar's F-nav-bar-01 is the same case, fixed the same way.

### F-segmented-02 — the venom runs where no window shows: disabled, and to an index outside the options — local

- **What.** The ink and fill layers draw the run whatever `enabled` and the index say, though the KDoc says a disabled
  button and an index outside `options` show no window:
  - disabled 240 ms into a run, the lime goes on flowing over the grey disabled body until the run ends
    (`before/disable-mid-run.png` 256–448 ms);
  - an index of −1 starts a run to a slot one segment left of the button: tentacles leave the body's left edge for
    400 ms (`before/to-none.png` 112–400);
  - an index past the end runs to a slot off the right edge, out over the page (`before/to-past-end.png` 64–400), and
    a real index after it runs in from there, the tentacles reaching past the button's end (592–880).
- **Where.** `SegmentedBody` follows `selectedIndex` itself, and `VenomRun` (shared) answers any slot it is given.

### F-segmented-03 — right to left, every word stands off its segment by half the room beside it — local

- **What.** A label's box is placed at its segment's start, which right to left is its right; the draw then moves it
  right by `(cell − line) / 2` as if it stood at the left. At the sample's 280 dp each word stands about 20 dp right of
  its place: «SP · 2 h» straddles the window's right edge, «EP · 6 h» runs into the body's end (`before/rtl.png`,
  every frame; `before/long-rtl.png`).
- **The decided look.** B-31: "Every label lies where the page's does"; B-94's class (a label off its pill).

### F-segmented-04 — a label wider than its segment spills over its neighbours and loses its start — local

- **What.** The KDoc says such a label "ends where the segment does". It is laid out cut to the segment's width and then
  centred on its full line, which moves it left by half its overflow: «Standard play · two hours» shows as «ndard play»
  from left of its segment, and «Extended play» runs under «LP» (`before/long.png`); at 96 dp the sample's own words
  overlap, «SP • LP • EP •» (`before/narrow-96.png`).
- **The decided look.** B-31's KDoc sentence; the kit's rule for a word that does not fit (`cmp-ui-component` §9) is
  an ellipsis at its end.

### F-segmented-05 — a pick during a run jumps the window to the segment picked before — reported (`VenomRun.kt`)

- **What.** `VenomRun.follow` starts a new run from the index it was running to, not from where the slime stands: the
  window appears whole at the earlier target in one frame, then flows from there.
  - `before/repick-2-then-0-at-304.txt`: at 320 ms the lime goes from 67 % / 21 % (old / new place) to 0 % / 100 %,
    8 792 pixels changed in that frame against 300–1 600 a frame through a run;
  - `before/fast-0-1-2-1-0.txt`: picks 64 ms apart, the window teleports to 1, 2 and 1 at 80, 144 and 208 ms (8 268,
    8 115, 8 083 pixels), and the venom never shows until the last;
  - the mouse 112 ms apart (`before/mouse-fast.txt`, 128 and 240 ms) and the keys (`before/keys.png`, 160 → 208 ms)
    the same.
- **The design.** The page reads the pill's `offsetLeft`, already at the last target, so the engine jumps too. The tab
  bar (F-app-bar-tabs-01) and the navigation pill (F-nav-bar-02) fixed the same class in their own files; this run is
  shared with the menu and the drawer, so it is reported, not fixed here. A fix would start a new run from what is
  drawn (the old run's masses at its p), which the generator's `venomShapes` has no input for.

### F-segmented-06 — right to left the segments do not mirror — reported (owner)

- **What.** The first segment stays at the left and Right chooses the next (`before/rtl.png`, `before/rtl-keys.png`).
  B-65 decided that the tabs and the navigation bar mirror; nothing decided it for the segmented button. Mirroring it
  would place the segments, the venom's slots and the press point from the right and map the arrows through the
  direction. Left as it is until the owner says.

### Looked at, nothing wrong

- Reduce motion: the window and the words move in the change's frame (`before/reduce.txt`, every frame).
- «Чистый сигнал», density 2, the browser's way: the same frames as «Эфир» at density 1 (their diffs peak at the same
  frames); no stale fill or word.
- Enabled ↔ disabled at rest: one frame, the kit's known cut (F-icon-fab-04), nothing of the window left behind.
- The keyboard: one stop, the brackets drawn; the arrows choose. Narrow widths 0–40 dp neither throw nor draw outside
  the body. The press point steers the mass (`before/mouse-click-2-right-edge.png`).
