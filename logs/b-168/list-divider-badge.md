# B-168 — list-divider-badge: `MutantListItem`, `MutantDivider`, `MutantBadge`

Group 10 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-list-divider-badge` from `07b2f7d`,
2026-11-22, on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand). The three live in
`mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantListItem.kt`, `MutantDivider.kt` and
`MutantBadge.kt` (the divider's strokes in `surface/MutantDivider.kt`); their numbers were decided in
[B-25](../../docs/backlog/B-25-list-item-divider-badge.md) and, for the divider's density,
[B-53](../../docs/backlog/B-53-text-button-divider-at-density.md). The design draws all three at rest only (handoff README,
«Компоненты»: ListItem, Divider and Badge, `rest`).

## What was looked at

Throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-list-divider-badge/` (not in the
repository).

- **The specimens as the sample composes them** (`<look>/`): the List item section's three rows (`tile1` with
  `12:04`, `tile2` with `НОВОЕ`, `tile3` with no icon), the Divider section's `d0`, the Badge section's two plates and a
  compact count, plus a row whose badge comes and goes and a 240 dp row with long words and a badge, in eight looks:
  «Эфир», «Копия», reduce motion, «Чистый сигнал», `steppedMotion`, density 2, and the browser's way of drawing
  (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true,
  `LocalMutantBodyAsImage` false) in both palettes. Per look, every frame captured with a per-frame pixel diff
  (`*.txt`) and sheets:
  - appear (`appear.png`) and two seconds at rest (`rest.png`);
  - a mouse press held 480 ms on `tile1`'s row, its release and 640 ms after (`press-mouse.png`, the tile and words
    zoomed 2×); a finger's press and lift on `tile2`'s row (`press-touch.png`);
  - the row disabled and enabled in place (`disable.png`, `enable.png`);
  - a badge appearing (`3`), changing its count (`12`), a large count (`1 288`, `99 999 999`) and leaving
    (`badge-*.png`).
- **The tile's icon against the fill under it** in every frame of a real mouse press and release, by a twin row of the
  same key and interaction source with the icon drawn transparent (`contrast-*.txt`, B-91's rule), in both palettes and
  both ways of drawing.
- **A scrolling list of many** (`list-<look>/`): a `LazyColumn` of 60 rows laid out as the demo's queue lays them (an
  icon tile, two lines, a time, a divider under each, 16 dp in), inside `MutantKeyframeLoss`, in «Эфир», «Копия»,
  density 2 and the browser's way: a finger's fling (`fling.png`), a slow scroll back and forth through the keyframe
  loss's window (`loss.png`), and 60 frames scrolled 23 dp each checking every visible row's tile for a frame drawn
  without its body (`empty-tiles.txt`).
- **The divider at densities 1, 2 and 3** (`divider-d*.png`, the strokes' starts in dp in `divider-runs.txt`).
- **Words' colours in place** (`live-colours.txt`, `before/live-*.png` and `live-*.png`), and hover under reduce motion
  (`hover.txt`).
- **The sample itself** (`sample/`): `MutantUiSample(SampleSettings(group = Containment | Communication))` in both
  palettes, each section captured (`page-*.png`); the demo app on its Queue tab inside a phone with the Tape and the
  keyframe loss, a pull to refresh by a finger that adds «Только что из эфира» at the top
  (`queue-*-before.png`, `queue-*-refresh.png` and `.txt`, `queue-*-after.png`), and the loss over the list
  (`queue-*-loss*.png`).

Brackets in the corners of a row after a finger's touch are the test window's keyboard input mode, not the kit
(the harness note of this pass); they were not counted.

## Findings

### F-list-divider-badge-01 — a row disabled or enabled in place keeps its words' colours — local

- **What.** `enabled` flipped on a live row greys its tile at once, but the headline, the supporting line and the
  trailing time keep their enabled colours for good: in «Эфир» the headline stays `#e9f2e4`, the supporting line
  `#aab4cf` and `12:04` the amber `#ffb000`, against `textDisabled` `#8a90a6` that a row composed disabled shows. Flipped
  back, the words would equally keep a stale grey. B-25 decided that a disabled row's words grey.
- **Frames.** `Efir/disable.png` (4368–4560 ms: grey tile, white words, amber time), the same in every look;
  `before/live-disabled-live.png`; `live-colours.txt` before the fix: `disabled-live … headline=e9f2e4 supporting=aab4cf
  time=ffb000`.
- **Cause.** The kit's text keeps the layout it measured when only the colour changes, and its draw is not invalidated
  (F-buttons-02, B-164's memo in `MutantText.kt`, reported). This is its face on the list item; a palette switch
  happened to redraw the words here only because the row's ground was repainted in the same layer.
- **Fixed** in `MutantListItem.kt` the way the tabs worked around it: each word is also tinted at drawing time
  through `MutantText`'s colour producer, so a change of colour redraws it. The cause stays reported. A headline's own
  spans keep their colours under the tint (a red span drew 112 red pixels, checked by a scratch test), so the search
  result's match (B-44) is untouched. After: `live-colours.txt` `disabled-live … headline=8a90a6 supporting=8a90a6
  time=8a90a6`, `enabled-again` the enabled colours, `live-disabled-live.png`.
- **Held** (`d468b05`) by `ListItemBehaviourTest` «a row disabled in place greys its words, and enabled gives them
  back»: a live row flipped to disabled shows each word in `textDisabled` in its first frame and none of its own colour,
  and flipped back every word's pixels in its colour count as before. Mutants, each read in the JUnit XML:
  - the three tints removed: killed («Настроечная таблица greys in the first frame disabled»);
  - the time's tint ignoring `enabled` (`osdInk = colors.textOsd`): killed («12:04 greys in the first frame disabled»);
  - one word's tint removed alone (headline, supporting line, time): survived, and equivalent here — the three words
    lie in one layer, so either of the other two tints redraws it with them. All three are kept, so no word depends on
    sharing a layer with another.

### F-list-divider-badge-02 — the keyframe loss leaves the dividers out of its picture — reported (shared: `surface/MutantKeyframeLoss.kt`)

- **What.** Over a list, the loss's band drags blocks of the tiles' bodies, but the tracking dividers inside the band
  stand whole and unmoved over the torn band, and so do the strokes it should break.
- **Frames.** `sample/queue-Efir-loss-12896.png` and `-13216.png`: the band runs from about y 280 to 530; the dividers
  under «Погода в три часа ночи» (y ≈ 336) and «Ночной повтор» (y ≈ 395) are drawn whole across it while blocks around
  them are dragged. `list-Efir/loss.png`, 3958–4982 ms, the same in a `LazyColumn`.
- **The design.** The engine's `moshStart` rasterises the whole stage SVG (`XMLSerializer().serializeToString(st.svg)`),
  whose art holds the dividers' rects (`buildStage`, `data-mu-divider`) and the cocoons beside the bodies, so its blocks
  drag the strokes too. B-48 made the kit's snapshot "the bodies alone" and does not list the dividers as a departure.
- **Why reported.** A divider would have to give its strokes to the loss's scene, an interface in `surface/`.

### F-list-divider-badge-03 — the demo queue keys its rows and dividers by position — reported (sample: `DemoQueue.kt`)

- **What.** `Queue` lays its rows out with `queue.forEachIndexed` and seeds each row `key = "queue-$i"` and each divider
  `key = "q$i"`, with no `key()` around them. A pull to refresh puts «Только что из эфира» at index 0, so in the frame
  it arrives every row already there takes the tile and the divider of the row above it: «Дождь над передатчиком»
  takes the body `queue-1` and the divider `q1` that «Погода в три часа ночи» had, and so on down. The rows' remembered
  state (press, focus, a body's transition) is handed down the same way.
- **Frames.** `sample/queue-Efir-before.png` against `sample/queue-Efir-after.png` (the dividers keep their places
  while the rows move down a row); `sample/queue-Efir-refresh.png`, 2720 → 2736 ms (18 080 pixels change in that frame
  and 25 000–30 000 in the next five as the list moves).
- **The rule.** Repeated items are keyed by identity, never by position (cmp-ui-component §3), and the kit's KDoc asks
  that each divider on a screen take its own key; the cassette's title is the identity here.

### F-list-divider-badge-04 — the list's scroll shear is not built, and the tiles boil on during a scroll — reported (owner)

- **What.** The brief's event table has «Список · scroll · shear: срезы и эхо отстают пропорционально скорости,
  ≤ 4 dp; во время скролла тики покоя стоят», and its inventory gives the list item «срезы отстают при скролле». The
  engine has no shear (the motion spec page lists «shear при скролле» among the events it lacks), and no item decided
  it under D15 (B-43 does not mention it). The kit's rows scroll rigidly and their tiles keep boiling at the rest tick
  through a fling.
- **Frames.** `list-Efir/fling.png`, 246–1398 ms (every tile at its row, no lag, the boil changing tick by tick).
- **Why reported.** A behaviour the design names but does not draw, touching the clock (ticks standing during a
  scroll) and the body; the owner's to decide whether and with what numbers.

## Seen again (already reported, no new face)

- The row's tile boils under a finger that starts a scroll (F-card-cocoon-01): the fling's first frames.
- `enabled` on a row is a one-frame cut of the tile (F-icon-fab-04's class): `Efir/disable.png`, `enable.png`.
- Hover under reduce motion redraws the tile (F-checks-switch-05): 312 pixels change on a mouse entering the row,
  none after it leaves (`hover.txt`).
- `MutantListItem`'s key defaults to `"tile"` and `MutantDivider`'s to `"0"`, so unkeyed rows and dividers are
  clones (F-card-cocoon-04's face on two more components; the sample keys all of its own).

## Looked at, nothing wrong

- **The tile's icon by a real press**: no frame under 4.37:1 against the fill under it in «Эфир» (5.07 on the way up,
  4.37 on the way down, 272 ms after the release, where the icon turns back light) and none under 11.04:1 in «Копия», the same in the
  browser's way (`contrast-*.txt`).
- **A finger's press** boils the tile and leaves the words where they are; release cools it over ~400 ms
  (`press-touch.png`).
- **The divider at densities 1, 2 and 3** (B-53): 2, 4 and 6 pixels tall, its strokes starting at the same dp to the
  pixel's rounding (81.0, 81.5, 81.3 dp; `divider-runs.txt`), across the whole width.
- **The badge**: appearing, changing its count and leaving are one-frame cuts, as the brief gives the badge no
  technique («—»); the row re-lays its words in the same frame. `99 999 999` stays whole in a 240 dp row and the words
  give way with an ellipsis (`badge-huge.png`); the plate's amber and black are the same in both palettes.
- **A list scrolled**: in 60 frames of 23 dp in four looks, the browser's way included, no visible row's tile drew a
  frame without its body (`empty-tiles.txt`).
- **Reduce motion**: 0 pixels change over two seconds and across every badge change (`reduce/*.txt`). «Чистый сигнал»
  and `steppedMotion` draw the rows as «Эфир» does (these specimens carry no Tape and no transition).

## Could not verify

- A list under reduce motion inside the keyframe loss: that run hit the test's one-minute limit and was not repeated.
- The phone's GPU (B-167), the real window and wasm: not run, by the pass's rules.
