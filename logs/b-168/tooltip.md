# B-168 — tooltip: MutantTooltipBox

Group 17 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), 2026-11-22, on the desktop test runtime of this
Mac. The component's own items: [B-32](../../docs/backlog/B-32-tooltip.md) (the two plates, where they stand, when they
show: at once while a mouse hovers, while the anchor holds keyboard focus, after a finger's long press until 1.5 s after
it lifts; a popup of no size that takes no press), [B-43](../../docs/backlog/B-43-undesigned-behaviour.md) and research
D15 (appearance designed here), [B-63](../../docs/backlog/B-63-hover-and-hand-cursor.md) (hover by mouse only),
[B-98](../../docs/backlog/B-98-the-loss-crashes-on-a-popup.md) (a popup's bodies draw uncut over a keyframe loss),
[B-106](../../docs/backlog/B-106-stepped-motion-named-plainly.md) (the header switch's hint). The design: the «Tooltip»
stage of `mutant-ui Компоненты v1.dc.html` (`tip`: blue, 30 dp, `ПОДЕЛИТЬСЯ` over an icon button; `tip-rich`: amber,
44 dp, two lines over a FAB; both `r 6`, `over 20`, `cf 0`, `white-space: nowrap`), the handoff's row `Tooltip` (`blob`,
shown only), the brief's table (`Tooltip`, no event).

## What was looked at

Frames in `/private/tmp/b168-tooltip/` (scratch tests, deleted). Sheets are one frame per cell (16 ms unless the label
says otherwise); `*.txt` beside a sheet holds its numbers, the per-frame diff counting pixels more than 16 levels apart.

| Sheet | What it composes | States and transitions |
|---|---|---|
| `appear-{plain,rich}-{efir,kopiya}.png`, `appear-{plain,rich}-d2.png` | the plain plate over an icon button, the rich one over a FAB, as B-32's test and the page stand them | rest; a mouse onto the anchor, 12 frames; two seconds shown, every frame diffed; the mouse off, 6 frames; both palettes; density 2 |
| `reduce-rich-efir.png`, `clean-plain-efir.png`, `stepped-plain-efir.png` | the same | under reduce motion, «Чистый сигнал», `steppedMotion` |
| `seams-plain-efir.png`, `seams-rich-kopiya.png` | the same, in the browser's way of drawing (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies`, `LocalMutantBodyOnGpu` true, `LocalMutantBodyAsImage` false) | the same |
| `edges.png`, `edges.txt` | six FABs in a window-filling stage: at the left edge, the right edge, the top, the top-left corner, 16 dp in from the bottom-right corner, and a long text in the middle | each hovered in turn |
| `neighbour-{none,keyboard,touch-linger,hover-neighbour}.png`, `neighbour.txt` | a button where the plate stands, as B-32's test | a mouse click on the button under the plate's words with the plate hidden, held by keyboard focus, lingering after a long press, and with the mouse resting on the button first; a finger's tap on it while the plate lingers |
| `focus.txt` | the anchor and a button below | Tab after a mouse click on the anchor, after a finger's tap on it, after a click elsewhere, from nothing |
| `scroll-longpress-after.png`, `scroll-longpress-after.txt` | the anchor in a scrolling column | a finger landing on the anchor and scrolling the column 142 px over 640 ms, then lifting |
| `hover-scroll.png`, `hover-scroll.txt` | the same | the mouse resting on the anchor while the column scrolls 200 px |
| `text-change.png`, `text-change.txt` | the rich plate over a FAB, reduce motion on | the first line a running time, `REC · 0:42:17` → `:18` → `:19` → `:20` |
| `components-section.png` | `MutantUiSample(group = Communication)`, the Tooltip section scrolled into view | a mouse over the share button, then over the FAB; a finger's long press on the FAB, lift, 1 s and 1.6 s after |
| `demo-lib-fab-loss.png`, `demo-lib-loss-full.png` | `DemoScreenFrame(Library)`: the FAB in its tooltip box at the screen's bottom end | the mouse on the FAB from 2.6 s (and 3.0 s) into the screen's clock to 6.0 s, across its first keyframe loss |
| `demo-lib-fab-sheet.png`, `demo-rec-share.png` | the Library FAB and the Recording screen's share button | hovered, then clicked: the new-recording sheet and the share sheet rise over them, the mouse resting |
| `header-steps.png`, `header.txt` | `MutantUiSample()`: the header's stepped-motion switch (B-106) | hovered |

Held as designed, and looked at:

- **Appearance and leaving** at once, as B-32 decided: the plate, its body and its words all stand in the first frame
  after the mouse arrives (`firstShownFrame=0` in every `appear-*.txt`), and all go in the first frame after it leaves.
  No frame of a bare word or a bare body on the desktop.
- **The plates**: blue 30 dp with one line of OSD type in the blue's own colour, amber 44 dp with two lines in black,
  centred over the anchor, 28 and 22 dp above it, the drips hanging towards it; in both palettes; at density 2 every part
  in proportion. Shown, the plate boils on its tick (5 or 6 changing frames in each 333 ms, the rest 0) and never flicks
  back. Under reduce motion it stands (120 frames, 0 pixels changed); «Чистый сигнал» changes nothing here (no Tape in
  this stage); `steppedMotion` moves nothing of the plate's, which has no movement, only the boil.
- **No press taken**: a mouse click on the button under the plate's words reached it once in every case, the plate
  hidden, held by keyboard focus, lingering after a long press, and with the mouse resting on the button first; a
  finger's tap on it while the plate lingered also reached it (`neighbour.txt`). The plate's popup is 0 × 0 (B-32,
  plakat-ui's B-72).
- **Keyboard focus**: Tab onto the anchor shows the plate; a mouse click or a finger's tap on it takes focus and shows
  nothing; Tab away and back after either shows it (`focus.txt`). The brackets a touch leaves in the test window are
  its Keyboard input mode, not a finding.
- **Hover follows the anchor**: the column scrolled 200 px under a resting mouse hides the plate (`hover-scroll.txt`); a
  sheet rising over the Library FAB or the share button takes the hover and the plate goes at once
  (`demo-lib-fab-sheet.png`, `demo-rec-share.png`).
- **The keyframe loss**: the Library screen's first loss ran with the FAB's plate shown, every frame drawn, nothing
  thrown; the plate draws whole over the screen as B-98 decided («the loss covers the screen, not what floats over
  it»). The band ran through the chips' rows, not under the plate: no frame shows the band crossing the plate itself.
- **The sample's section**: the share button's plate stands over the row's «Variants» caption and the FAB's over the
  cell above it, as a tooltip does; a long press on the FAB shows the plate, it stays at 1 s after the lift and is gone
  at 1.6 s.

## Findings

### F-tooltip-01 — a finger scrolling from the anchor shows the plate (local)

A finger that lands on the anchor and scrolls the page shows the plate once the long-press timeout runs out, though the
finger has moved 142 px and the page scrolled under it; the plate then stays 1.5 s after the lift
(before the fix: `shown while held=true`, `1 s after lift shown=true`; the before-sheet was overwritten by the after-run, the numbers are its). A user scrolling
the Components page, or a list of anchors, raises plates they never asked for. The platform's long press is cancelled
once the finger leaves the touch slop; B-32 says «after a finger's long press», the platform's timeout, and a scroll is
none.

Cause: `MutantTooltipBox`'s pointer loop starts the long-press timer on the press and cancels it only on the release; a
move never cancels it. **Local**: `components/MutantTooltip.kt`.

### F-tooltip-02 — near the window's edges the plate is cut, and above the top it is gone (reported: owner)

The plate is centred over its anchor and stands above it wherever the anchor is, and its popup does not clip
(`clippingEnabled = false`), so the window cuts it (`edges.png`, `edges.txt`, the 1024 × 768 test window):

- an anchor at the left edge: the plate's slot starts at −32 dp; at the right edge 33 dp of it lie past the window;
- a FAB 16 dp in from the bottom-right corner, where a FAB stands: 15 dp of the rich plate past the edge, its words
  ending 3 dp past it, the last letter clipped. The demo's Library FAB is placed exactly so; its plate
  overhangs the phone's frame (`demo-lib-fab-loss.png`), which on a real screen is the window's edge;
- an anchor in the window's top 58 dp (66 for the rich plate) — a top bar's action, as the demo's `BarAction` — puts the
  plate wholly above the window: the tooltip shows nothing (`edges.png`, «top», «top-left»; words at y = −53 and −43).

B-32 decided «centred over its anchor … its bottom 28 dp above it» from the page, whose anchors stand at fixed offsets;
neither it nor the design says what happens at an edge. Keeping the plate inside needs a decision: shifting it along the
edge moves it off the anchor's middle, and turning it below the anchor makes its drips hang away from what it speaks of
(«плашка стекает на то, о чём говорит») — a look the design does not draw. Also: the plate is placed past a 0 × 0 popup
by a `Layout`, which knows nothing of the window, so either answer changes how the popup is measured or positioned. In
the tooltip's own file, but a placement for the owner.

Not verified: on Android a `Popup` is a window of its own, and a 0 × 0 window may clip a plate drawn past it (the
cmp-ui-component skill's note on plakat-ui's B-72: «Android to clip a popup as its own window … none was run»). No phone
was used in this pass.

### F-tooltip-03 — a long text runs off the window: the header's hint is cut by 42 % (reported: owner, sample)

The plate is one line per text, never wrapped (the design's `white-space: nowrap`, B-32), and as wide as its longest
line, measured without a limit. The sample's header gives the stepped-motion switch a sentence as its supporting text
(B-106): its plate is 1 405 dp wide in the 1 024 dp window, centred on the switch at x = 911, so it starts at 209 and
ends at 1 614. The hint's line (1 381 dp) is cut after 803 dp — 42 % of the sentence unreadable, «Off, they …» cut
mid-word — and the plate's left end lies over the header's title (`header-steps.png`, `header.txt`). B-106's AC reads
only that the hint shows, not that it reads. A consumer's long tooltip does the same. Either a width limit and wrapping
for the supporting line (a look the design does not draw) or a shorter hint in the sample; with F-tooltip-02, the owner's.

### F-tooltip-04 — the design's own running time reshapes the plate every second (reported: face of F-icon-fab-03)

`key` defaults to `text`, and the body's seed is its key: the design's own example, `REC · 0:42:17`, kept running,
regenerates the plate's contour and drips each second — 1 552, 1 749 and 1 670 pixels of a 260 × 200 crop changed per
tick of the time, under reduce motion, where the plate otherwise stands with 0 (`text-change.png`, `text-change.txt`).
The KDoc says nothing of it. The same default as F-icon-fab-03 (a toggling icon's description); the kit-wide answer is
that item's. A consumer passes a constant `key` meanwhile.

### F-tooltip-05 — in the browser's way the plate's first frame is the stand-in (reported: face of F-buttons-03)

With the browser's seams, every appearance of the plate draws the stand-in for one frame (16 ms): a flat rounded
rectangle with the outline, no drips, then the body (`seams-rich-kopiya.png`, frame +16; `seams-plain-efir.png`). A
plate appears on every hover, so in the browser each hover shows it; on the desktop the first frame waits and the body
stands at once. The stand-in is shared code (`surface/`), F-buttons-03's.

## Fixed here

- **F-tooltip-01** (`38ae0c0`): the pointer loop remembers where a finger came down and cancels the pending long press
  once the finger moves past `viewConfiguration.touchSlop`; a long press already shown keeps its linger. The KDoc says
  so. Kept test: `TooltipBehaviourTest` «a finger scrolling the page from the anchor is no long press» — a finger on the
  anchor in a scrolling column moves 160 px over 640 ms (the page scrolls over 100 px under it, asserted), and no plate
  shows while it is held or after the lift; B-32's long-press test (no movement) still shows the plate and its linger.
  Mutant: the cancel on movement removed — killed («held, after a scroll»). After: `scroll-longpress-after.png`,
  `scroll-longpress-after.txt` (`shown while held=false`, `1 s after lift shown=false`); the neighbour and focus checks
  run again unchanged.

Reported: F-tooltip-02 (placement at the window's edges, owner), F-tooltip-03 (long text and the header's hint, owner and
sample), F-tooltip-04 (face of F-icon-fab-03), F-tooltip-05 (face of F-buttons-03).
