# B-168 — pull-to-refresh: `MutantPullToRefresh`

Group 21 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-pull-to-refresh` from `662fa7a`,
2026-11-23, on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand). The component lives in
`mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantPullToRefresh.kt`; its numbers were decided
in [B-36](../../docs/backlog/B-36-pull-to-refresh.md) (the gesture, the pill's second, font scale),
[B-95](../../docs/backlog/B-95-pull-to-refresh-moves-smoothly.md) (the 300 ms eased glide, the tongue from 16 dp) and
[B-105](../../docs/backlog/B-105-tongue-becomes-the-pill.md) (the tongue becomes the pill, the pill goes back into the
line).

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-pull-to-refresh/` (not in the
repository).

- **The component as the sample's «Try it» cell composes it** (`core/<look>/`): a 300 × 260 stage on the page's colour
  under its own screen clock, eight list rows in a `verticalScroll`, the caller refreshing for two seconds when asked.
  Nine looks: «Эфир» (`Efir`), «Копия» (`Kopiya`), reduce motion (`Reduce`), «Чистый сигнал» (`Clean`),
  `steppedMotion` (`Stepped`), density 2 (`D2`), the browser's way of drawing in both palettes (`GpuEfir`,
  `GpuKopiya`: `LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true,
  `LocalMutantBodyAsImage` false) and right to left (`Rtl`). Per look, every frame captured with the list's top, the
  caller's `refreshing` and the scroll logged in `<scenario>.txt` beside a per-frame pixel diff:
  - `appear`: two seconds at rest;
  - `touch-full`: a finger 8 dp a frame to 240 dp, held, let go past the line, refreshing two seconds, done, back
    (every frame of the morph also alone, `touch-full-f<ms>.png`, and tiled as `/private/tmp/b168-pull-to-refresh/morph-<look>.png`);
  - `touch-short`: a finger let go at a 39 dp gap;
  - `mouse-full`: the same pull by a mouse;
  - in `Efir`, `D2` and `Rtl` also: `scrolled-touch` (the list scrolled down by a finger, then one finger drag down
    that scrolls it back and goes on into a pull), `scrolled-mouse` (a mouse drag on the scrolled list), `second-pull`
    (refreshing set by the caller, then a finger and a mouse pull and a push up while it refreshes), `fling` (a fast
    swipe down from the top; a pull flung back up), `wheel` (the wheel down and up past the top; a finger down 200 dp
    and back; then a mouse pull).
- **The sample itself** (`sample/`): `Pulls` (the Components page's section, `ComponentsCommunication.kt`) in both
  palettes at density 1 and 2, its «Try it» cell pulled by a finger and a mouse (`page-*.png`); the demo's Queue
  (`DemoApp` on `DemoTab.Queue` in a 360 × 720 phone with the Tape) pulled by a finger and a mouse in both palettes
  (`queue-*.png`), the end of refreshing every frame (`queue-*-end-0.png`).

What held, in every look: the pull follows the finger and the mouse at half the distance after the slop (2 → 113 dp
over 30 moves, 4 dp a move, `touch-full.txt`); the tongue grows from nothing at 16 dp, the long one from 74; let go
short it glides back over 300 ms, every frame (41 → 35 → 30 … 3 → 2, `touch-short.txt`); let go past the line it glides
to 74 and refreshes once; the pill stands its second and rides back into the line; reduce motion moves at once and the
pill still stands 1008 ms; a mouse pull clicks nothing; a mouse drag on a list scrolled away from its top pulls
nothing; a finger drag on it scrolls it home and then pulls; a second pull while refreshing does nothing; a fling down
from the top refreshes (the drag passed the line); the wheel scrolls the list (0 → 26 → 0 px) and does not pull at the
top; density 2 and right to left move the same gaps frame for frame (`touch-full.txt` identical but for D2's halves);
`steppedMotion` does not step the glide, as B-95 decided it (its glide is not one of B-100's eleven kinds).

## Findings

### F-pull-to-refresh-01 — the tongue does not fade as it becomes the pill: 300 ms of hot slime, then the clean pill in one frame — local

- **What.** When refreshing ends, B-105 has the tongue (boiled amber, drips) fade out draw by draw over the pill's own
  clean body as both take the pill's shape. In every look the tongue stays opaque for the whole move: the body cools
  from amber to yellow with its outline rough and its drips drawn up, and on the frame the move ends the clean lime
  pill replaces it whole (`Efir/touch-full.txt` 3872 → 3888 ms, 3006 px; `morph-Efir.png`, `morph-Kopiya.png`,
  `morph-GpuKopiya.png`, last row; the demo the same, `sample/queue-Efir-end-0.png` 2304 → 2320). It is the cut B-105
  was made to remove.
- **The cause.** The tongue fades by `graphicsLayer { alpha; compositingStrategy = ModulateAlpha }`, which applies its
  alpha to each draw call of its own layer and not to a child layer. Since B-135 (2026-11-18, a day after B-105)
  `MutantSurface` draws its body in a `graphicsLayer()` of its own, so the alpha reaches nothing. A scratch test: a red
  box drawn under `ModulateAlpha` 0.2 comes out `#330000`, a green one in a child layer `#00ff00`, unchanged.
  `PullToRefreshMorphTest` reads the body's size, not its colour, so it stayed green.
- **The design.** B-105: "the tongue over it, its drips drawn back up, fades out draw by draw"; the rejected cross-fade
  of two whole bodies does not apply (one body, the pill's, stands under).
- **Local**: `MutantPullToRefresh.kt` (`Tongue`).
- **Fixed** (`3650b49`). The tongue fades through a saved layer drawn around it (`pullTongueFade`), which takes its
  child layers, 64 dp past its box (the surface's own reach) so its drips fade with it; only while it becomes the pill,
  so the page's frames draw as before. `PullToRefreshMorphTest` «the boiling tongue fades out over the pill as it
  becomes it…»: a finger pulls past the line, refreshing two seconds, then of the hanging slime the share that is the
  clean pill's lime (greener than red by 30) per frame. Boiling 0.0 (limit < 0.1); two frames in 0.004 (limit < 0.5:
  still mostly tongue); ten frames in, 160 ms, 0.9987 (limit > 0.8); the standing pill 1.0 (limit > 0.9). Mutants, each
  killed by it (`<failure>` read in the JUnit XML):
  - the old fade, `graphicsLayer { alpha; ModulateAlpha }`: «the tongue has faded over the pill by 160 ms: lime
    0.0039»;
  - the tongue gone at once (`pullTongueFade(0f)`): «the tongue does not vanish at once: lime 1.0 at 32 ms».
- **After.** `after/morph-<look>.png`: the boiled tongue thins over the clean pill as both take its shape, the pill
  whole by about 160 ms; the frame the move ends changes 64 px (3006 before), `after/Efir/touch-full.txt` 3872 → 3888.
  The same in «Копия», the browser's way, density 2 and the demo (`after-sample/queue-*-end-0.png`).

### F-pull-to-refresh-02 — refreshing started without a pull never boils — local

- **What.** The design's refreshing frame boils (`v1-ptr-b`, `data-heat`). The tongue boils only when refreshing
  starts while a pulled tongue already stands. Refreshing started any other way shows the long tongue lime and still,
  its word in it, for as long as it refreshes: the app's own refresh started by code, the reader's custom action
  (`onRefresh` from rest), and a screen composed refreshing — the sample's «Состояния» cell, which stands lime for
  good (`sample/page-Efir-d1-0.png` top cell against the boiling «Try it» cell under it; `Efir/second-pull-0.png`).
- **The cause.** The press that boils the tongue is emitted from a `LaunchedEffect(refreshing)` of the component, into
  an interaction source the tongue's `MutantSurface` collects in its own `LaunchedEffect`. When the tongue is composed
  in the same frame (a screen composed refreshing) the component's effect runs first, when it is not composed at all
  (a gap under 16 dp when refreshing starts from rest) nobody collects: the `SharedFlow` drops the press either way,
  and nothing emits it again. A pulled tongue was already collecting, so a pull boils.
- **Local**: `MutantPullToRefresh.kt`.
- **Fixed** (`1fc8023`). The tongue presses its own body, from an effect after the body's, so the body already collects
  when the press comes; whenever the tongue is composed anew it presses again. `PullToRefreshMorphTest` «refreshing
  boils the tongue on a screen composed refreshing and when started with no pull»: the share of lime in the hanging
  slime after a second, 0.0 in both cases (limit < 0.1; 1.0 before the fix, NaN, which fails, if nothing hangs).
  Mutant: the press effect moved back before the body: «composed refreshing, the tongue boils amber: lime share 1.0»,
  killed.
- **The size reader moved with it.** `PullToRefreshMorphTest` «the long tongue becomes the pill with no jump in its
  size» read the body from slime at full strength (green over 140). With the tongue now boiled amber, its fade over the
  pill (F-01) dropped out of that reading on one frame, 121 × 40 → 114 × 30, a frame that draws no jump
  (`after/morph-*.png`). It now reads slime faded to a tenth (red or green 24 over the page's); its steps are 5 dp at
  most against the 8 B-105 allows. B-105's mutant, the move taken out (`morph` always 1), still fails it: «1520 ms:
  126×39 → 94×26».
- **After.** `after/Efir/second-pull-0.png`: refreshing set by the caller from rest, the tongue heats as it grows and
  boils amber; `after-sample/page-*-0.png`: the «Состояния» cell boils, rising over the press's 320 ms from the page's
  first frame (a screen composed refreshing does not start hot: the body's heat starts with its press).

### F-pull-to-refresh-03 — reduce motion: refreshing shows no boil — reported (= F-buttons-06, new face)

- Under reduce motion the press's heat rises on the paused screen clock, so it stays 0: the refreshing tongue is the
  pulled one in lime with a word (`Reduce/touch-full-0.png`, 1584 ms on). The design's refreshing frame is a boil
  (`data-heat`), and D7's pause should keep damage, not remove a state's look. Same cause as F-buttons-06
  (`surface/BodyHeat.kt` and the clock); not fixed here.

### F-pull-to-refresh-04 — the refreshing word appears whole in one frame — owner

- When refreshing starts the word stands in the tongue at once: let go past the line, on the frame refreshing starts
  (`Efir/touch-full-0.png`, 1584 ms); started from rest, on the frame the glide lands, the long tongue standing 300 ms
  without it (`Efir/second-pull-0.png`, 864 ms), as the word is drawn only once the gap reaches 74. B-105 fades the
  words of the morph; nothing decides the word's entrance. The design draws three still frames. A look the design does
  not draw: for the owner.

## Not findings

- The rows' amber corner brackets in the frames are the list item's keyboard focus: an injected touch leaves the test
  window in keyboard input mode and the press focuses the row (the brief's harness note). On a phone the input mode is
  touch.
- The word of the pill outgrows the pill as it goes back into the line, fading over the first 40 % of the way: as B-105
  decided.
- Neither word shows at the middle of the morph (one fades out over the first half, the other in over the second): as
  B-105 decided.
- The demo's queue gains its new row in one frame as refreshing ends (`queue-*-end-0.png`, 2016 ms): the sample's,
  F-list-divider-badge-03 and F-search-05's class.
- The mouse sees the content at its top only through the scrolls passing the connection (B-36): a list scrolled back by
  exactly its scroll would not let a mouse pull until a scroll reaches past the top. A finger's slop and a wheel's notch
  always overshoot (`Efir/wheel.txt`: a finger down 200 and back, then a mouse pull to 73 dp), so it was not seen.
