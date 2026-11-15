---
id: B-47
title: "Carousel: 220 × 120 cards, calm side cards, a pill pointer among dots"
status: done
priority: P3
size: M
stage: stage-6-later
epic: feature-components
blocked_by: [B-21, B-43]
---

# B-47 — Carousel: 220 × 120 cards, calm side cards, a pill pointer among dots

Later. Bodies `card` (220 × 120; side cards `calm` 0.5), `dot` (10) and `pill` (the pointer, 26 × 10). State: rest; not interactive in the design.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты позже.dc.html`

## Findings (2026-11-15)

**Built.** `MutantCarousel(items, …)` with `MutantCarouselItem(title, subtitle, status)`, as the later page draws its shelf (`l-carousel`, 400 dp):
- a `HorizontalPager` of cards 220 × 120, 14 dp apart, in a 172 dp view that clips them;
- the current card stands 84 dp in, so the card before peeks 70 dp, as on the page, and the next fills the rest;
- the cards beside the current one stand calm (`data-calm=".5"`);
- under the view, 16 dp down, a 10 dp dot per card, and the pill pointer (26 × 10) at the current one, 8 dp apart.

The items are structured, a title (Rubik 16/500), a subtitle (13, secondary) and a status at the foot (Handjet 20, amber), so the kit lays the status at the card's foot and keeps the clean zone over the words.

**Designed here (D15).** The page is a still.
- **Moving.** A swipe moves the shelf. A press on a card beside the current one brings it to the middle, at once under reduce motion. A press on the current card goes to `onClick`.
- **Semantics.** The carousel says where it stands (`2 / 3`).
- **Its own stage.** The cards are a stage of their own, `<stage>-view`, as the page's view is: a body's seed, speckle and patterns read its stage, so the kit's view seeds its cards apart from the screen's, as the page's does.
- **No width given.** The carousel takes the page's 400 dp, as a pager cannot measure in an endless width; the constraint sweep caught that.
- **Larger text.** A larger font scale grows the card, `IntrinsicSize.Min` with 120 dp at least, and the view with it. Without the intrinsic height, `fillMaxHeight` under a minimum filled the window (716 dp), as in B-61.

**The reference script** now records, for an element crop with `slots`, the bodies of the stages nested in the element too, each in its own stage's frame. Before this, the cards (on `l-carousel-view`) had no slots and every outline differed: 1 242 px in «Эфир». No existing reference's slots changed.

**Parity** (`CarouselParityTest`, both themes).
- Bare: 0 px in «Эфир». In «Копия» it is 69 px: the speckle under the print slip on its 40 dp grid, single pixels over the current card and the pointer (research §1.15). That residual is recorded and never to be raised.
- The current card's words and the next card's start lie within 1 px of the page's. In «Эфир» the title's top row stands one above the page's: light on dark, on the ink threshold (B-24's cause), recorded as `TOP_RESIDUAL`. The card before shows no word in its 70 dp.

**Behaviour** (`CarouselBehaviourTest`):
- a press on the next card brings it in, and a press on the current card is the click;
- the current card is 220 × 120 at 84 dp;
- the carousel says `2 / 3`.

**QA.** The kit gained a carousel for the constraint, long-content and font-scale sweeps. The keyboard sweep and the hover test gained it too; it has no disabled state.

**Sample.** A «Carousel» section on the components page.

**Mutants.** All seven killed:
- `CarouselParityTest`:
  - side cards at full wear;
  - the pointer a dot;
  - the view seeded on the screen's stage;
  - a 12 dp gap to the dots.
- `CarouselBehaviourTest`:
  - the current card at 90 dp;
  - a press beside that moves nothing;
  - an animated move under reduce motion.

The last two first survived. The test pressed with the mouse, and in the test window a mouse press focuses the card, and the pager brings a focused card into view by itself, around the code under test. It now presses with a finger and wants the move within one frame.
