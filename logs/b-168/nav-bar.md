# B-168 — nav-bar: `MutantNavigationBar`, `MutantNavigationItem`

Group 9 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-nav-bar` from `07b2f7d`, 2026-11-22,
on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand). The component lives in
`mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationBar.kt`; its numbers were
decided in [B-24](../../docs/backlog/B-24-navigation-bar.md), [B-56](../../docs/backlog/B-56-navigation-bar-unbounded-width.md),
[B-61](../../docs/backlog/B-61-text-grows-with-the-font-scale.md), [B-65](../../docs/backlog/B-65-tabs-and-navigation-in-rtl.md),
[B-66](../../docs/backlog/B-66-navigation-pill-in-narrow-cells.md) and [B-94](../../docs/backlog/B-94-the-navigation-bar-label-on-its-pill.md).
The design draws the bar at rest only (handoff «Компоненты»: `bar` + `pill`, «активный пункт — pill 56×30», states
`rest`); the pill's slide to a new pick is the kit's (B-24, D15), the body's `slide` (330 ms, ease-out², the stretch).

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-nav-bar/` (not in the repository).

- **The bar as the sample composes it** (`core/`): `NavBars`' bar — four destinations «On air», «Queue» with the badge
  `3`, «Find», «Settings», 360 dp, no insets — on the page's colour with 16 dp round it, beside a bare twin
  (`NavigationBarBody(hidden = true)`, the same keys and clock) so that what lies under each icon and label is read
  from the twin. Nine looks: «Эфир», «Копия», reduce motion, «Чистый сигнал», `steppedMotion`, density 2, the browser's
  way of drawing (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true,
  `LocalMutantBodyAsImage` false) in both palettes, and right to left. Every frame captured, sheets 2× (1× at density 2),
  a per-frame pixel diff (`*.diff.txt`) and per frame and cell the median contrast of the icon's and the label's ink
  against the twin's pixel under it (`*.ink.txt`):
  - appear (`appear-*`); a mouse click on «Find», then on «Queue», 560 ms each (`pick-*`; right to left and the two GPU
    looks' second half timed out on the shared machine);
  - four clicks 96, 96 and 64 ms apart, Settings → Queue → Find → On air (`fast-*`);
  - the badge `3` → `4` → `12` → none → `3`, then «Find» disabled and enabled (`badge-*`);
  - no selection and back to «Find» (`none-*`); two seconds at rest (`rest-*`);
  - keyboard: focus on «Queue», Tab, Enter, Left, Space (`keys-*`);
  - twelve destinations in 360 dp (B-66) over the same bar in a horizontally scrolling row (B-56), the pick moved
    0 → 11 → 2 (`narrow-unbounded.png`).
- **The sample itself** (`sample/`): `MutantUiSample(SampleSettings(group = Navigation))` with the Tape, its bar
  clicked to «Settings» and «Queue» (`page-bar.png`); the Demo page's library phone (`DemoScreenFrame(Library)`), its
  bar clicked through Queue, Search, On air, Settings, On air in «Эфир» and «Копия» (`demo-bar-*.png`, the whole phone
  `demo-full-*.png`).

## Findings

### F-nav-bar-01 — the icons take their colour at the pick, not under the pill: black on the dark bar — local

- **What.** On a pick the chosen icon turns the pill's black `#05060c` and the one left the secondary colour at once,
  while the pill takes 330 ms to slide over. In «Эфир» the chosen icon stands black on the dark bar until the pill
  reaches it, the one left stands pale on the pill as it goes, and an icon the pill passes over stays pale on lime.
- **Numbers** (`core/pick-Efir.ink.txt`, median contrast of the icon's ink against what is under it; at rest 4.9–8.8):
  the chosen «Find» 1.45:1 for 11 frames (1056–1232 ms); «On air» left 1.59:1 for 2 frames; «Queue» passed over 1.6:1
  for 6 frames (1120–1200). Under `steppedMotion` «Find» is still 1.46:1 250 ms after the click
  (`core/pick-Stepped.ink.txt`). «Копия» stays readable (worst 3.3:1, the secondary on lime).
- **Frames.** `core/pick-Efir.png` 1056–1232 and 1648–1776 ms; `core/fast-Efir.png`; the sample's
  `sample/page-bar.png` 1584–1744 ms and the demo's `sample/demo-bar-Efir.png` 1072–1232 and 1584–1680 ms.
- **The design.** The page draws the pill's icon `#05060c` on the pill and the others in `--mu-text2` on the bar;
  B-24 chose the slide. The kit's rule from B-91/B-94 and `cmp-ui-component` §4: a word's colour follows the fill as
  drawn, never the logical state.

### F-nav-bar-02 — a pick during a slide jumps the pill to the cell last chosen — local

- **What.** A new pick while the pill is under way starts its slide from the cell picked before, not from where the
  pill stands: the pill leaps there in one frame and slides back.
- **Frames.** `core/fast-Efir.png`: at 1136 ms the pill stands near «Queue» on its way to «Settings»; at 1184 it stands
  whole on «Settings» (~170 dp in one frame), then slides back to «Queue»; again at 1312 and 1408. Under
  `steppedMotion` picks less than 83 ms apart never move the pill, it hops from cell to cell
  (`core/fast-Stepped.png`). The diff: 6 100–6 300 pixels on the jump frames against 3 000–3 700 for a slide
  (`core/fast-GpuEfir.diff.txt`). «Копия» the same (`core/fast-Kopiya.png`).
- **Cause.** `PillFrom` remembers the previous index, and the slide is asked from `(previous − chosen) · cell`.
- **The item.** B-93's class (a selection's slime jumping instead of moving).

### F-nav-bar-03 — a slide re-aimed mid-way snaps its stretch back — reported (`surface/BodyTransitions.kt`, `slid`)

- **What.** A new slide request restarts the body's stretch at zero: the pill, up to 1.4 times as wide and 0.82 as
  tall half-way, is its rest size in the next frame. Seen only once F-nav-bar-02 is fixed (the pill then starts where
  it stands); numbers in the fix's after-frames below.
- **Where.** `slid` scales by `1 + .4 · sin(π p)` with `p` from the new request; `BodyMoves.follow` keeps no trace of a
  run in flight. Every slide shares it (the switch's thumb reversed mid-way, the calendar's day).

### F-nav-bar-04 — a two-digit badge grows to the left over its icon — reported (owner; B-24's numbers)

- **What.** The badge is anchored by its right edge 6 dp in from the icon box's right (B-24), so a wider count grows to
  the left: `12` covers half of the clock icon (`core/badge-Efir.png`, 1184–1328 ms), where `3` touches its corner.
  The design draws only a one-digit count.
- **Also.** In twelve 30 dp cells (B-66) the badge, anchored to the 56 dp icon box, reaches 7 dp into the next cell
  (badge `9` on the fourth item, `core/narrow-unbounded.png`).

## Looked at and found right

- **Appear**: the bar and the pill whole from the first frame on the desktop; the browser's way draws the generic
  stand-in for frame 0 (F-buttons-03's class, nothing new here).
- **The slide**: 330 ms with its stretch, landing exactly on the cell in every look; at once under reduce motion; in
  83 ms steps under `steppedMotion`; mirrored right to left with the badge on the icon's other corner (B-65). The
  labels: the chosen one in the text colour from the pick, readable on the bar in every frame (`*.ink.txt` 10.6:1
  «Эфир», 7.1:1 «Копия»); the label never stands on the pill (B-94 found the remark was the chip's).
- **No selection** (`-1`): the pill goes in one frame and comes back in one frame on a pick, with no run, as B-24
  decided (`core/none-*.png`).
- **The badge** changes its count in one frame (a count has no designed transition); «Find» disabled greys at once and
  takes no click.
- **Density 2** (`core/pick-D2.png`): every icon, label, badge and the pill where they stand at density 1.
- **Narrow cells and no width** (B-66, B-56): the pill 30 dp and inside its cell, the unbounded bar 4 × 82 dp, both
  sliding as at the page's width.
- **Keyboard**: the amber brackets on the focused cell, Tab to the next, Enter picks and slides the pill; the arrows do
  not move between destinations (Compose's default on desktop, as for the tabs; nothing decided otherwise).
- **At rest**: 2 600–2 800 pixels a tick in two seconds, no frame out of line (`core/rest-*.diff.txt`); the browser's
  way within a few hundred pixels of the desktop's through every scenario.
- **The demo**: the bar is moved between the tabs' screens whole (`movableContentOf`), and its pill slides from the tab
  left to the tab taken across the screen change (`sample/demo-bar-*.png`).
