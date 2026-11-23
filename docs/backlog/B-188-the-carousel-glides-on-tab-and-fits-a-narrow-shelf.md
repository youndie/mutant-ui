---
id: B-188
title: "The carousel glides on Tab and fits a narrow shelf"
status: done
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-188 — The carousel glides on Tab and fits a narrow shelf

Tab to the next carousel card moves the shelf by Compose's bring-into-view (234 dp in 4–5 frames), not the press's glide (F-carousel-07). Under 304 dp the current card is cut, and many dots run past a narrow shelf (F-carousel-05, F-carousel-06). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 10 ([ошибка]) and 33 ([решение]).

- **The decision and its reason.** A Tab scroll takes 330 ms on the `slide` curve. A card is min(220, the window's width − 84) dp wide. With more than seven cards a counter «03/12» (Handjet 18, the `osd` colour) stands instead of the dots.

- AC:
  - Tab to the next card scrolls the shelf over 330 ms on the slide curve
  - at widths from 400 down to 200 dp the current card is whole and min(220, w − 84) wide
  - eight cards show «01/08» and no dots; seven show seven dots
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCarousel.kt`

## Result, 2026-11-23

`MutantCarousel.kt` only. On `fix/b-188-the-carousel-glides-on-tab-and-fits-a-narrow-shelf`, on this Mac, desktop JVM,
`runComposeUiTest` with the clock driven by hand.

- **The glide on Tab** (answer 10). Keyboard focus on a card asks to be brought into view, and the pager answered by its
  own bring-into-view: a spring, 234 dp in four or five frames. A `BringIntoViewModifierNode` between each card and the
  pager now takes that request: the shelf glides to the card, and the card's place, cut to the carousel's width, is
  asked of the containers round the carousel, so a scrolling page still shows it. The pager's `BringIntoViewSpec` could
  not do it: its `scrollAnimationSpec` is deprecated and the pager's own spec does not forward it.
- **One glide** (`CarouselGlide`): 330 ms (`MutantMotion.SLIDE_MS`) on the slide curve, ease-out², on every frame by
  default and in the hot tick's 83 ms steps under `steppedMotion` (D22), the finished frame at once under reduce motion
  (D24). The press on a card beside and a notch of the wheel take it too. **The item said the press already glided
  this way; it did not**: `animateScrollToPage` with Compose's default spring, which settles in about the same 330 ms
  on another curve. The answer names one glide, so the press and the wheel now take the slide curve as well; this
  moves only live behaviour. A second ask for the card already being glided to leaves the glide running, so a press
  that also focuses its card (a mouse in keyboard mode) does not start it over. A new target mid-glide (Shift+Tab back)
  turns from where the shelf stands.
- **A narrow shelf** (answer 33). A card is min(220, width − 84) dp wide, read from the carousel's own width (the
  answer's «ширина окна»: the shelf's window, the view, as the carousel cannot know the screen's); the room after the
  last card is what is left past it, so the last card still stands 84 dp in (B-168). From 304 dp up nothing changes.
  Under 84 dp a card is 0 wide; the constraints sweep (widths 0, 1, 24, 48) lays it out and draws it without a throw.
- **The counter** (answer 33). With more than seven cards «03/12» stands under the view instead of the dots: the OSD
  role at 18 (Handjet, tracked 1, `textOsd`), the numbers padded to the count's digits, two at least, centred in the
  dots' row 16 dp under the view. It changes at once with the current card, as an OSD line does (answer 1's badge), and
  is hidden from a reader, who already hears the carousel's state description.

Numbers the answers do not state, decided here (D15): the counter's padding to the count's digits and its place
(centred, 16 dp under the view, as the dots); the glide's steps under `steppedMotion` (the kit's transition step, as
D24 reads answer 4).

**Tests** (`CarouselBehaviourTest`, each failing on the code before, run with `MutantCarousel.kt` at `main`):

| AC | Test | Before |
|---|---|---|
| Tab to the next card scrolls the shelf over 330 ms on the slide curve | «B-188 Tab to the next card glides the shelf over 330 ms on the slide curve»: each frame on the way names when the glide began, within 10 ms of each other (0.8 ms measured), every frame within 3 px of `84 + 234 (1 − p)²`, landing 300–350 ms in (320 measured) | only 9 frames on the way, 188 → 85 px by 210 ms |
| … under `steppedMotion` and reduce motion (D22, D24) | «B-188 Tab glides in the hot tick's steps under steppedMotion and lands at once under reduce motion»: three to five places under the steps; under the pause 318 → 84 within two frames of the key, nothing between | 11 places under the steps |
| at widths from 400 down to 200 dp the current card is whole and min(220, w − 84) wide | «B-188 at widths from 400 down to 200 dp …»: every 8 dp and 304, 303, 260, 201; the card's own size and place (its bounds in the root are cut by the view, which read a cut card as whole), the next card 14 dp after it where it is in view | 296 dp: 220 wide |
| eight cards show «01/08» and no dots; seven show seven dots | «B-188 eight cards show a counter and no dots, seven show seven dots»: «01/08», no lime under the view, the OSD amber there; «03/08» after a turn; seven cards: no counter, the lime pointer, seven runs of ink on the dots' middle line | no «01/08» |

A fifth test guards what the change could break, not an AC, so it passes on the code before as well: «B-188 a card
focused below the fold of a scrolling page is still brought into view» (the pager used to pass the request on; the new
node does it now).

**Mutants**, each after the commits (`6e05daf`, `88d287a`), one line changed, the aimed test named among the failures in the JUnit
XML, the file restored with `git checkout`, `git status` clean:

| Mutant | Killed by |
|---|---|
| no node between the card and the pager (`.then(bring)` removed) | «Tab … slide curve»: only 9 frames on the way, the pager's spring |
| the glide linear (`p` for ease-out²) | «Tab … slide curve»: the start by frame spread 59 ms |
| the glide never stepped (`stepped(…, false)`) | «… under steppedMotion …»: 18 places on the way |
| the glide under the pause (`if (false)` for the instant branch) | «… under reduce motion»: the glide's frames under the pause |
| the card always 220 (`CARD_W`) | «at widths …»: 296 dp, 220 wide against 212 |
| the counter past eight (`> CAROUSEL_MAX_DOTS + 1`) | «eight cards …»: no «01/08» |
| the counter from seven (`>= CAROUSEL_MAX_DOTS`) | «… seven show seven dots»: a counter with seven cards |
| the card's place not passed on (`outside.requester.bringIntoView` removed) | «… below the fold …»: the page scrolled 0 dp |

**Goldens and parity cells.** None moved. The cells (`MutantCarousel_Rest_*`, `MutantCarousel_RestBare_*`) draw three
cards at 400 dp, where the card stays 220 and the dots stay; `CarouselParityTest` passes unchanged, and the repository's
PNGs (the Chrome references) are untouched. No departure: nothing crosses the parity line, so research D20's registry
gains no row. The rest is live behaviour or widths and counts no cell draws.

**Frames looked at** (throwaway sheets, deleted; frames in `/private/tmp/b188/`, not in the repository): Tab from the
current card to the next, before and after, in «Эфир», «Копия» and «Эфир» at density 2; the shelf at 400, 300 and 200 dp
with three and twelve cards; seven, eight and twelve cards at the first and third card; Shift+Tab half-way through the
glide. Before: 334 → 204 px in the first frame of the snap. After: the shelf eases in over 330 ms, the pill sliding under
it as before; at 200 dp the card is 116 dp and whole, its title ending in an ellipsis; «01/08», «03/12» centred under the
view in the OSD amber, rust on «Копия»; Shift+Tab turns the shelf back from where it stands, with no jump.

**Not verified.** No phone and no browser: Android, iOS and wasm were compiled and run only as `./gradlew check` runs
them, and `compileKotlinIosArm64` compiled. A real keyboard in a real window and a screen reader skipping the counter were
not driven; the counter is hidden by `hideFromAccessibility`, read by no test.
