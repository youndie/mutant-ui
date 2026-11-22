# B-168 — card-cocoon: `MutantCard`, `MutantCardTitle`, `MutantCardSubtitle`, `MutantCocoon`

Group 7 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-card-cocoon` from `43bb3e2`,
2026-11-22, on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand).
The components' own items: [B-21](../../docs/backlog/B-21-card.md) (80 dp tall, 200 wide given no width, the content
16 dp in and 14 down, lines 4 apart, no press look designed), [B-22](../../docs/backlog/B-22-skeleton-cocoon.md) (the
engine's `buildStage`/`stageStep` 1:1, 16 and 8 dp blocks, 6 % a tick, stands under reduce motion and «Чистый
сигнал»), [B-91](../../docs/backlog/B-91-pressed-labels-on-amber.md) and [B-104](../../docs/backlog/B-104-carousel-words-on-a-pressed-card.md)
(the lines follow the fill through `inkOnBody`), [B-63](../../docs/backlog/B-63-hover-and-hand-cursor.md) (the clickable
card's hand and simmer). The design: the «Card · Кокон» stage of `mutant-ui Компоненты.dc.html` (`card` rest and
disabled at 80 px, the card's cocoon and the list row's), the handoff's rows Card (`card`, dead pixels in the corner,
rest and disabled) and Skeleton (no body, loading), the profile `card` (`r` 10, `over` 24, `dead` 1), the generator's
`deadPx` (blocks from 2 dp right of the clean zone, densest within 34 dp of one right corner), and the brief's event
table (Card `appear` → `tune-in`, which the animation-spec page lists among the events the engine does not have).

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-card-cocoon/` (not in the
repository). Sheets are one tile a frame (16 ms) unless named otherwise; `*-diff.txt` and `*-every.txt` count the
pixels more than 16 levels apart from the frame before.

- **The sample's sections** (`sample/`): `MutantUiSample(SampleSettings(group = …))`, the page scrolled through its
  `ScrollState` to the section, the section's cells cropped from the root:
  - Card (Containment): the group opened from Foundations and the first 40 frames (`card-appear-<look>`); two seconds at
    rest, every frame diffed (`card-rest-<look>`); a finger down on the rest card for 576 ms and up, 800 ms
    (`card-press-<look>`); in «Эфир» and «Копия» the mouse over it, pressed, released and gone (`card-mouse-<look>`);
  - Cocoon (Communication): opened from Foundations, two seconds every frame (`cocoon-<look>`);
  - looks: «Эфир», «Копия», reduce motion, «Чистый сигнал», `steppedMotion`, density 2;
  - the Demo's Library screen (`DemoScreenFrame(Library)`, both palettes): a finger put on the first card and moved up
    the list 240 dp, then lifted (`demo-finger-<palette>`).
- **The components as the sample and the demo compose them** (`core/`), on the page colour, by paths and in the
  browser's way of drawing (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu`
  true, `LocalMutantBodyAsImage` false, as `FillChangeOnGpuTest`; `-gpu` in the name):
  - the demo's hatching: a 80 dp cocoon handed over to the card it stands for, back, and over again
    (`handover-<palette>[-gpu]`);
  - a card appearing, leaving and coming back (`appear[-gpu]-shown`); the palette switched under it and back
    (`appear[-gpu]-palette`); pressed by the mouse, removed mid-press and composed again (`appear[-gpu]-pressgone`);
  - eight cards and a cocoon in a `verticalScroll`: one card captured by its own node on every frame of a 3 px a frame
    scroll (`scroll-card2`), and a finger scrolling the list from a card (`scroll[-gpu]-finger`);
  - sizes: 120 dp, no width, a long title in 240 dp, 600 dp, 362 × 160, an empty card, at densities 1 and 2 in both
    palettes (`sizes-<palette>-d<n>`); three cards with no `key` (`clones`);
  - contrast read frame by frame as `PressedLabelContrastTest` reads it (the label's pixel furthest from the ground 8 dp
    left of it): the title and the subtitle over a touch press, a mouse hover and press, a hover alone
    (`press-contrast-<palette>-<way>.txt`), and over a palette switch (`palette-contrast-to-<palette>.txt`).

Held as the design draws it, and looked at: the card's body, its dead-pixel corner right of the clean zone (on a long
title only in the last 14 dp, as `deadPx` puts it), its drips and flow lines kept off the lines, at every size and at
density 2 in proportion; the disabled card's still «no signal» body with grey lines; a touch or mouse press boiling the
clickable card to amber with its title turning to the ink as the fill passes its middle and back on release (the title
at 4.45:1 or better, the subtitle 3.22:1 or better on every frame of a press, a release and a hover, in both palettes);
reduce motion standing everything (0 pixels changed over two seconds and a press); «Чистый сигнал» removing the Tape
only; `steppedMotion` moving nothing of this group's (the per-frame diffs equal «Эфир»'s); a scrolled card changing only
on its own ticks (`scroll-card2-diff.txt`: 398–404 pixels at 1152, 1280, 1392 ms, 0 between: D16 holds); the cocoon
appearing from its first frame, repainting on the scene ticks (125 ms, 2–5 thousand pixels) and standing under reduce
motion and «Чистый сигнал» (0 pixels changed), its blocks 16 and 8 dp at density 2; in the browser's way a card back
from the pool drawing its body from its first frame (`handover-*-gpu` at 1728, `appear-gpu-shown` at 1424).

A touch press in the test window focuses the card and shows its brackets and its calm focus body (`card-press-*`,
`scroll-finger`): the test window's input mode is Keyboard. The mouse sheets show no brackets. Not a finding. In the
browser's way a card appearing for the first time draws B-149's stand-in, a plain rounded plate with no drips or dead
pixels, until its build is done (9 frames in the test, `handover-efir-gpu-p0.png` 1088–1216): B-149's design, its
stand-in look already reported as F-buttons-03.

## Findings

### F-card-cocoon-01 — a finger scrolling a list of cards boils the card it started on — reported (kit-wide; a look the design does not draw)

- **What.** A finger put on a card and moved to scroll its list presses the card first: its body warms towards amber
  as the list starts to move, then cools for the next 480 ms × heat. The user scrolls, and the card under the thumb
  lights as if chosen.
- **Frames.** `sample/demo-finger-efir-p0.png`: the Demo's Library, the first card brown from 1120 to 1248 ms while the
  list moves, cooling to 1280 (the per-frame diffs carry the scroll, so the sheet is the evidence);
  `core/scroll-finger-p0.png` (and `scroll-gpu-finger`), the same on a plain list: «Tape 4» brown at 2016–2080, cooling
  to 2176.
- **Cause.** Compose's `clickable` holds a press inside a scrollable only on Android (`TapIndicationDelay` is 0 on
  desktop, iOS and the browser, CMP 1.12); elsewhere it emits `PressInteraction.Press` on the finger's down and
  `Cancel` only when the scroll takes the pointer past the slop. The card hands its interaction source to its body
  (B-63, B-91), so the body boils from the down. cmp-ui-component §4 names the class (field-ui B-93: a finger scrolling a
  list squashed and lit the card under it).
- **Why not fixed here.** Every body that shares its source with a `clickable` does the same (the list row, the chip,
  the buttons, the carousel's cards); a fix for the card alone would leave its neighbours in the same list boiling.
  The hold (Compose's 100 ms tap timeout, or until the lift, dropped past the slop) is a number no item decided, and
  knowing a scrollable ancestor needs Compose's internal `isInScrollableContainer`. One relay in `surface/` that holds a
  finger's press inside a scrolling ancestor would serve every body. Phones in the browser are where the owner looks at
  the demo.

### F-card-cocoon-02 — a palette switch pours every card's old fill under its new words: 1.46:1 for 5 frames — reported (`surface/BodyMoves.kt`, F-chip-02's face)

- **What.** Switching «Эфир» → «Копия» makes the card's body pour the cream fill down from the top over 620 ms (its
  pour for an 80 dp body: `260 + 5 · h`, at most 620) while its lines are already the new palette's dark ink. Neither
  candidate `inkOnBody` picks from reads on the old navy: the title reads **1.46:1** for 5 frames (656–720 ms after the
  switch), the subtitle **1.87:1** for 9 frames (to 784). Back to «Эфир» within the pour, the title flicks to 1.46:1
  for one frame (848). After a switch back the body keeps pouring navy over navy, invisibly, and a press made then
  boils only the part the front has passed: the lower half stays the unboiled old fill for 130 ms.
- **Frames.** `core/appear-palette-p0.png` 2224–2416 (the cream body with dark words on the navy band),
  `core/appear-pressgone-p0.png` 2688–2800 (a press boiling from the top down), `core/palette-contrast-to-kopiya.txt`,
  `core/palette-contrast-to-efir.txt`.
- **Why it is a new face.** F-chip-02 measured a chosen chip at 1.81:1 for 3 frames. The card is chosen by no one: every
  body of the screen pours on the switch, and a card's pour is the longest of the kit's small bodies. The engine pours
  only on a toggle. The cause is the surface's `follow()` comparing resolved colours.

### F-card-cocoon-03 — the card appears whole in one frame and the cocoon hands over in a cut — reported (a look the design does not draw; F-search-05's face)

- **What.** A card composed (a section opened, a tape's copy done) is drawn whole from its first frame and leaves in one
  frame. The Demo's cocoon «still being copied» hands over to its card in one frame and back (17 917 pixels at
  1088 ms in «Эфир», 27 525 in «Копия», 0 after). The brief's event table has a row for the card itself: `appear` →
  `tune-in`, «Мозаика 16 → 8 → 4 → чисто, затем контур; каскад 40 мс, весь экран ≤ 500 мс», 320 ms, `stepped(3)`;
  the animation-spec page lists «tune-in у Кокона» among the events the engine does not have, so no frame of it exists.
- **Frames.** `core/handover-efir-p0.png` 1072 → 1088 and 1520 → 1536, `sample/card-appear-efir-p0.png` (whole at
  1568 ms, the group's first frame).
- **For the owner.** F-search-05 reported the search results' face; the card is the event's own row in the brief, and
  the demo shows it on its first screen. It is D15's to design (the mosaic in 16 → 8 → 4 dp is the cocoon's own block
  set), or to leave as a cut.

### F-card-cocoon-04 — cards with no `key` are one body repeated — reported (a default the owner settles)

- **What.** `MutantCard`'s `key` defaults to the constant `"card"`, so every card a consumer writes without one is the
  same generated body: the same drips, the same top mosaic, boiling in step. Only what follows the clean zone differs
  (the flow lines, where the dead pixels start). A list of cards reads as a pattern stamped three times.
- **Frames.** `core/clones-p0.png`: three cards with three titles, one body.
- **Context.** Every labelled component defaults its key to its label (F-icon-fab-03), so two chips differ by their
  words; the card has no label parameter. The cocoon's KDoc says «Several on one screen each take their own key»; the
  card's says nothing. The sample and the demo give every card a key. The options: a KDoc line; a default from the
  composition (`currentCompositeKeyHash`), which differs per call site and item; or leave it. It moves no golden.

### F-card-cocoon-05 — under the mouse the subtitle reads 3.22:1, against 6.9 at rest — reported (a look the design does not draw; B-63's hover)

- **What.** The hover simmer (heat 0.3, B-63) warms the card's navy to a brown on which the secondary line reads
  3.22:1 for as long as the mouse rests on the card (6.9:1 at rest); the title reads 5.81:1. It clears the kit's 3:1
  (B-91's AC), not WCAG's 4.5:1 for 13 sp text. Under a press the subtitle never stays near the crossover: its worst
  frame is 3.30:1.
- **Frames.** `core/press-contrast-efir-hover.txt`, `core/press-contrast-efir-mouse.txt`; «Копия» 5.19:1 at worst.
- **For the owner.** Listed beside B-63's hover look, which its item leaves to the owner's review round.

## Fixes

None: every finding's cause is in shared code or a look the design leaves open. Nothing in `MutantCard.kt` or
`MutantCocoon.kt` was changed.
