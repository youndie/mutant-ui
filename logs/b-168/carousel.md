# B-168 — carousel: `MutantCarousel`, `MutantCarouselItem`

Group 24 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-carousel` from `662fa7a`, 2026-11-23,
on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand). The component lives in
`mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCarousel.kt`; its numbers were decided in
[B-47](../../docs/backlog/B-47-carousel.md) (cards 220 × 120, 14 dp apart, the current one 84 dp in, a 172 dp view,
the dots 16 dp under it) and [B-104](../../docs/backlog/B-104-carousel-words-on-a-pressed-card.md) (the words follow
the pressed fill). The design draws the shelf at rest only (handoff «Компоненты»: `card` + `dot` + `pill`, «боковые
calm=.5; указатель pill 26×10 среди точек 10», states `rest`; README: «карусель не интерактивна»); every movement is
the kit's (D15).

## What was looked at

A throwaway test, deleted before committing; the frames stay in `/private/tmp/b168-carousel/core/` (not in the
repository): every frame captured, sheets of every second or third frame, and per frame (`*.txt`) the pixels changed
from the frame before, the runs of ink on the indicator's middle line (`pill=`: the 26 dp run is the pill) and the
pager's `currentPage`, offset fraction, settled and target page.

- The shelf as the sample's «Carousel» cell composes it (the design's three cassettes, `rememberPagerState(1)`), 400 dp,
  on the background with 16 dp round it, in nine looks: «Эфир», «Копия», reduce motion, «Чистый сигнал»,
  `steppedMotion`, density 2, the browser's way of drawing (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies`
  true, `LocalMutantBodyOnGpu` true, `LocalMutantBodyAsImage` false) in both palettes, and right to left:
  - appear and two seconds at rest (`rest-*`);
  - a tap on the next card, then on the one before (`tap-*`, `tapzoom-*` every frame);
  - a slow drag by touch, 8 px a frame for 20 frames, and a fling (`drag-*`, `fling-*`);
  - a mouse drag across the shelf (`mouse-Efir`), the wheel horizontally and vertically, with and without the pointer
    over the shelf (`wheelH`, `wheelC`, `wheelV`);
  - keyboard: focus on the current card, Tab, Enter, Left, Tab (`keys-*`);
  - a held press on the current card and on the next (`press-*`, B-104), a mouse click on each (`mouseclick`);
  - re-aimed mid-move: a tap on the next card while the shelf still runs (`reaim-*`);
  - the first and the last card, and a tap to the last (`first-*`, `last-*`, `tolast-*`);
  - widths 200, 260, 300, 328 (the demo phone's column), 360 and 520 (the sample's cell), each at the middle card and
    at the last (`width*`); twelve cards at 400 and 200 dp (`many*`); one card; no width at all (`unbounded`).

## Findings

### F-carousel-01 — the last card never stands where the current one stands; at the sample's width it is never reached — local

- **What.** The pager has no room after its last card, so the last card stops where the content ends, not 84 dp in. At
  400 dp it stands at 180 dp with the card before showing 160 dp (`last-Efir.txt`: `frac=-0.410`; `tolast-Efir.png`
  from 512 ms). At 328 dp (the demo's column) it stands at 108 dp. At 520 dp — the sample's own «Carousel» cell — the
  shelf's whole travel is 252 dp, less than one card's 234 dp past the middle one, so the last card can never become the
  current one: a press on it moves the shelf 18 dp and back to the middle card (`width520-last-Efir.txt`: asked for
  page 2, `page=1 frac=0.077`), its `onClick` can never fire, the pill never reaches the last dot and the carousel
  never says `3 / 3`.
- **The design / item.** B-47: «the current card stands 84 dp in»; the KDoc: «a press on a card beside the current one
  brings it to the middle». The first card already stands 84 dp in with nothing before it.
- **Cause.** `contentPadding` has a start (84) and no end.
- **Fixed** (`3a0fe64`). Room after the last card, as wide as the view leaves past a card standing 84 dp in
  (`maxWidth − 84 − 220`, none under 304 dp); the view now measures it through `BoxWithConstraints`. Nothing at rest
  moves: `CarouselParityTest` unchanged. `CarouselBehaviourTest` «B-168 the last card is reached and stands where the
  current one stands, at any width»: at 400, 328 and 520 dp a press on the last card makes it the current one at exactly
  84 dp, and a second press is its click. Mutant: no room after it (`end = 0.dp * end.value`), killed — «400 dp: the
  last card's left expected 84.0 but was 180.0».
- **After** (`/private/tmp/b168-carousel/after/`): `last-*`, `width*-last-*` and `tolast-*` all `page=2 frac=0.000`,
  the card 84 dp in (112 dp from the left in right to left, its right edge 84 dp in); the first card as before.

### F-carousel-02 — the pill jumps to the next dot, and for three frames the pointer is a lime dot beside a dark pill — local

- **What.** The pointer does not move: when the pager's `currentPage` turns (half-way through the scroll) the slot of
  the dot left turns from `pill` to `dot` and the next from `dot` to `pill` in one frame — the pointer's centre jumps
  18 dp — and each body keeps its key, so the new pill pours the lime in from the dot's dark fill and the dot left
  pours its lime out. For three frames the indicator shows a lime **dot** where the pointer was and a dark **pill**
  where it goes (`tapzoom-Efir.png` 192–224 ms, `tapzoom-Kopiya.png`; `tap-Efir.png` 320 and 1008 ms), then two
  frames of the pour. Under `steppedMotion` the same, in steps; under reduce motion a cut, as it should be.
- **The design / item.** The page: «указатель — капля среди точек»; B-47 decided no movement for it (the page is a
  still). The nav bar's pill slides to a new pick (B-24, the body's `slide`, 330 ms), and B-93/B-105 name a selection's
  slime jumping instead of moving as the class to fix.
- **The brief's worry** (a pill re-aimed mid-move): nothing moves, so nothing is re-aimed; the fix below inherits
  F-nav-bar-03 (a re-aimed slide restarts its stretch) from `surface/BodyTransitions.kt`, already reported.
- **Fixed** (`973575f`). The pointer is one `pill` body laid over the dots, keyed by the dot it stands at (`cd1` at
  rest, as the page keys it), with the body's `slide` from the dot it left: the navigation bar's `PillFrom` (read, not
  changed) keeps where a slide under way stands, so a page turning back mid-slide starts from there. The dot left
  appears at once under the leaving pill; the dot reached is gone at once under the arriving one. No number is new: the
  slide is the body's (330 ms, ease-out², the stretch), and at once under reduce motion. `CarouselParityTest` unchanged.
  `CarouselBehaviourTest` «B-168 the pointer slides to the next dot and stays lime on every frame»: a press on the next
  card, then 50 frames, the lime on the dots' row read from the pixels: no frame under half the lime at rest (least 87
  against 122, the limit 61) and the lime's centre never more than 9 px from the frame before (half a dot's step; widest
  4.2), ending 18 dp on. Mutants, both killed:
  - no slide (`0.0 *` the dots' step): «frame 3: the pointer leapt from 200.4 to 217.6»;
  - the indicator of `3a0fe64` (each dot's slot turning pill and dot in place): «frame 3: 51 lime pixels on the dots'
    row, 122 at rest».
- **After** (`after/tapzoom-*.png`, `after/tap-*.png` in every look, `*.txt`): the lime capsule stretches and runs to
  the next dot in about 330 ms, lime in every frame, both ways and right to left; the browser's way draws the same runs
  as the desktop. A tap on the moving shelf stops it (Compose's own), so a pick is not re-aimed by taps; a drag turning
  back past half-way re-aims the pill from where it stands, and inherits F-nav-bar-03's stretch restarting.

### F-carousel-03 — one notch of the wheel moves the shelf 11 dp and springs back — local

- **What.** With the pointer over the shelf, a horizontal wheel notch scrolls 0.047 of a card (11 dp) and the pager
  snaps back to where it stood; five notches at once reach the next card (`wheelC-Efir.txt`). A carousel cannot be
  stepped notch by notch with a horizontal wheel, shift and the wheel, or a short trackpad swipe. A vertical notch
  passes to the page, as it should (`wheelV-Efir.txt`).
- **The rule.** `cmp-ui-component` §4, The wheel: one notch steps one item where content snaps; a trackpad's small
  deltas are summed to whole notches, reset on a change of direction.
- **Fixed** (`2a87519`). A pointer handler before the pager's own takes a sideways scroll (or shift with the wheel
  where the platform leaves it vertical), sums it to whole notches, the sum dropped when it turns, and steps
  `targetPage` by each, through `animateScrollToPage` (at once under reduce motion); right to left the other way. A plain
  vertical turn is left alone. `CarouselBehaviourTest` «B-168 one notch of the wheel steps one card, a vertical one
  leaves the shelf»: one notch on and one back, three deltas of 0.3 not a notch and a fourth one, three vertical notches
  moving nothing (fraction within 0.001, the pager's own notch moved 0.047). Mutants, both killed: no step
  (`0 * notches.toInt()`) — «a notch on expected 2 but was 1»; the sum not dropped on turning — «a vertical turn is the
  page's expected 1 but was 2» (the 0.2 left over swallowed the notch back).
- **After** (`after/wheelC-Efir.txt`): one notch glides to the next card and stops at `frac=0.000`; five more clamp at
  the last.

### F-carousel-04 — a mouse drag moves nothing — local

- **What.** Pressing on the shelf with the mouse and dragging 150 px left changes nothing (`mouse-Efir.txt`:
  `frac=0.000` throughout): Compose's `scrollable` takes no mouse drag on the desktop or in the browser, where the
  sample runs. With no horizontal wheel, a mouse user can move the shelf only a card at a time by pressing the card
  beside the current one.
- **The item.** B-47: «A swipe moves the shelf» — by touch only, as built.
- **Fixed** (`30b8916`). For a mouse only, a pointer handler takes the drag past the touch slop in the initial pass,
  so the card's press under it is cancelled: the shelf follows the pointer from there (the slop subtracted, no jump),
  and at the release the pager's own fling behaviour settles it by the pointer's speed. A finger is left to the pager.
  `CarouselBehaviourTest` «B-168 a mouse drags the shelf with the pointer, with no jump by the slop, and clicks
  nothing»: a 180 px drag in 12 px frames moves the shelf 162 px (the limit 140), no frame more than 18 px (each is
  12; a jump by the slop made 24), it settles on the next card and no click fires. Mutants, both killed: the handler
  for an unknown pointer type instead of the mouse — «the shelf moved 0.0 px under a 180 px drag»; the slop not
  subtracted — «frame 1: the next card moved from 318.0 to 294.0».

### F-carousel-05 — under 304 dp the current card is cut at the right — reported (owner; B-47's 84 dp)

- **What.** The current card stands 84 dp in at any width, so it needs 304 dp; under that its right edge and words are
  clipped by the view: at 260 dp 44 dp of it is gone, at 200 dp 104 (`width260-Efir.png`, `width200-Efir.png`,
  `many-narrow-Efir.png`). The phone's 328 dp column fits (10 dp of the next card shows).
- **For the owner.** Whether a narrow shelf takes less of the card before (the start shrinking to keep the current
  card whole) or keeps the design's 70 dp peek. B-47 states only the page's 400 dp.

### F-carousel-06 — many dots run past a narrow shelf, the pill outside it — reported (owner)

- **What.** The dots row is as wide as its dots, 18 dp each and 16 more for the pill: twelve cards need 222 dp. At
  200 dp the row runs 11 dp past each side of the carousel, over its neighbours, and the pill on the last dot stands
  wholly outside it (`many-narrow-Efir.png`). The design draws three dots; nothing decides a limit (a smaller step, a
  window of dots, or a count).

### F-carousel-07 — Tab to the next card moves the shelf by Compose's own snap, not the kit's glide — reported (owner)

- **What.** Keyboard focus on the card beside brings it in by the pager's bring-into-view: 234 dp in four to five frames
  (`keys-Efir.txt`, 240 → 322 ms), where a press on that card glides there by `animateScrollToPage` in about 330 ms
  (`tap-Efir.txt`). A mouse click goes the glide's way (`mouseclick.txt`); a finger press in the test window, which is in
  keyboard mode, focuses the card and takes the snap (`press-Efir.txt` 880–944 ms; a harness effect, a real touch does not
  focus).
- **For the owner.** Whether focus should glide as a press does (a bring-into-view spec of the kit's own, which every
  scrolling container of the kit would want alike). Not changed here.

## Looked at and found right

- **At rest and appearing**: the three cards 220 × 120, 14 dp apart, the current one at 84 dp, the card before showing
  70 dp and calm, the next calm (`rest-*.png`); 500–650 pixels a frame on average and no frame out of line in two seconds
  (5 000–5 600 on the hot tick, 13 500 at density 2); the bodies whole from the first frame on the desktop. Reduce
  motion: 0 pixels change. The browser's way within 30 pixels a frame of the desktop's.
- **A tap on the card beside**: the shelf glides in about 330 ms and stops on the card; the side cards take calm .5 and
  the new current one its full wear in the frame the page turns (no wear jump stands out in `tapzoom-*`); at once under
  reduce motion; the semantics turn `2 / 3` → `3 / 3`.
- **The press** (B-104): on the current card amber with black words and back, words readable in every frame on both
  palettes (`press-*.png`); the side card the same while the shelf moves under it.
- **Touch**: a slow drag follows the finger, settles on the nearer card on release (`drag-*`); a fling stops on the next
  card; right to left mirrored, the pill on the right for the first card (`first-Rtl.png`).
- **Density 2** draws at the places of density 1 (`rest-D2.png`). **No width** takes the page's 400 dp
  (`unbounded.png`). **One card**: a pill alone under it.
- **Torn drops** of the current card fall inside the view and are cut at its bottom (`rest-GpuKopiya.png` 1456–1936 ms),
  never over the dots (F-buttons-07's class does not reach here).
- **Focus brackets**: shown on the focused card in the test window after a press, as every component's in keyboard mode.

## Gate

`./gradlew :mutant-core:desktopTest :sample:desktopTest :mutant-core:ktlintCheck :sample:ktlintCheck --continue` on
this Mac at `30b8916`, the scratch test deleted, read from the JUnit XML: `mutant-core` 458 tests, no `<failure>` or
`<error>`; `sample` 22 tests, one failure — `DemoTapeOnceTest` «instant 1: 6 498 px unlike the phone alone» (the
Recording phone, no carousel in it), the flake the orchestrator already holds, here on the Mac under the shared
machine's load. Run alone it passed, and `:sample:desktopTest --rerun` passed 22 of 22. Both ktlint checks clean.
`:mutant-core:compileKotlinIosSimulatorArm64` compiles (the new private top-levels are `CarouselShelf`, `CarouselDots`,
`carouselWheel`, `carouselMouseDrag`, `CAROUSEL_DOT_STEP`). The public API did not change (`MutantCarousel`'s
signature is as it was), so `updateKotlinAbi` was not run.

## Not verified

- The phone and the browser itself: the browser's way was drawn through the seams on the desktop JVM only, the body
  shader (B-167's class) not at all.
- A real mouse and wheel: the mouse drag and the wheel were driven by `performMouseInput` only; a real trackpad's
  deltas and a platform's shift-wheel conversion were not seen.
- A real window's input mode under a mouse (whether a desktop mouse press focuses a card and so takes F-carousel-07's
  snap): the test's `performMouseInput` click took the glide.
- The sample's own composition was not filmed separately: its «Carousel» cell is this shelf at 520 dp (`width520-*`),
  and the demo's library column is the 328 dp case.
