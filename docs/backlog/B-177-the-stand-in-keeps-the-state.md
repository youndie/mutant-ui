---
id: B-177
title: "The browser's stand-in draws the body's state and stroke"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-177 — The browser's stand-in draws the body's state and stroke

[B-168](B-168-look-and-motion-pass.md)'s pass found one defect three times, each reported against `surface/StandIn.kt`:

- F-buttons-03 ([report](../../logs/b-168/buttons.md)): in the browser's way of drawing, a filled button that came back
  disabled drew its first frame as the live acid plate; the outlined one drew a plain dark plate;
- F-tooltip-05 ([report](../../logs/b-168/tooltip.md)): every appearance of a tooltip draws a flat rounded rectangle with
  no drips for one frame;
- F-screens-11 ([report](../../logs/b-168/screens.md)): a Demo screen chip draws the whole new phone as stand-ins for one
  frame, 25–53 k px.

**Why**: [B-149](B-149-a-body-in-its-first-frame.md)'s stand-in, drawn where the frame cannot wait for a body's first
build (the browser, [B-153](B-153-a-first-picture-between-frames.md)), drew the slot's rounded shape in `look.fill` with
`look.ink` whatever the body's state, heat, shadow and slip (the foundations agent's shared-code class 7,
[report](../../logs/b-168/foundations.md)). A body taken back from the pool ([B-156](B-156-a-pool-of-bodies.md)) draws
no stand-in, but a signature the pool does not hold does: a button disabled while it was away comes back with its first
frame live. It is [B-166](B-166-a-chip-flicks-back-to-its-old-fill.md)'s class — a look that is not the body's for a
frame — in the frame before any build. The desktop, Android and iOS wait for the build (B-149) and never draw it.

- **The decision**: the stand-in is given what the body's state is drawn by when no instance is needed — the state, the
  focus, the heat and its time, the instance's seed (`"$cassette/$key"`, which the instance only carries), the theme —
  and draws what it can of it (`StandInState`, `drawStandIn`):
  - **Disabled** and **Success** are drawn on the slot's rounded rectangle anyway (`drawDisabledBody`,
    `drawCleanBody`), so the stand-in draws them as the body does: the disabled grey with its scanlines, snow, dropouts and
    shifted band; the clean frame with its highlight and, focused, its brackets. Of Disabled only the edge mosaic is
    missing: its tiles are generated with the instance (`edgeMosaic`), and generating them in the frame is work the
    stand-in is not to add.
  - A **live** body keeps B-149's plate (its shape, its outline inside the slot) and draws on it what its look and
    state say: its fill boiled by its heat (a press's or a fixture's, `boilFill`), slipped from its outline as the body's
    fill is, over its misregistered shadow, the stroke's ink, the error's offset outline and corruption blocks
    (`errorBlocks` from the slot and the seed), the loading bands and the focus brackets.
  - The drawings read the slot and seed alone: `drawDisabledBody`, `drawCleanBody`, `drawBrackets`, `loadingBands` and
    `errorBlocks` gain an overload taking those, and the instance's own versions delegate to it, so the body's drawing
    is the same code.
- **What it costs**: the stand-in's own draw, in the frame that draws it, and nothing else: no instance, no field, no
  variant built ahead (B-151's phone budget). A disabled stand-in draws the disabled body's few hundred small rects
  instead of two round rects, once per stand-in frame.
- **Rejected**:
  - building a still body's instance in the frame (it is cheap, `gen.instance` without a field) to draw it exactly,
    edge mosaic included — work added to the frame, which B-153 took out of it;
  - building the next likely variants ahead (class 8 below) — work for the phone, out of this item;
  - a stand-in with the body's shape (drips) — that needs the instance's field, the first build itself (B-149's
    question).
- AC:
  - through the seams of the browser's way (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true,
    `LocalMutantBodyOnGpu` true, `LocalMutantBodyAsImage` false), the first frame of a body, read from that frame's own
    recording, shows its state's fill and its stroke: a disabled body grey with no pixel of its live fill, the disabled
    body but for its edge mosaic; a successful body its clean frame exactly; a hot body its boiled fill and none of its
    cold one; an outlined body its stroke and its shadow; an error its red marks; a loading body its bands; a focused
    body its brackets (`StandInStateTest`, seven tests, every one failing on `main`);
  - the desktop's way unchanged: no stand-in where the frame waits (`FirstFrameOffTheFrameTest`);
  - every golden, parity and numeric golden unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/StandIn.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/surface/StandInStateTest.kt`

## Class 8, for the owner: the old body kept for frames on a new signature

Out of this item (F-text-field-06; F-icon-fab-04 and F-text-field-08 are its desktop cut). In the browser a signature
the surface never held — focus calms the wear, disabled and success make the body still — is built between frames while
the old instance goes on drawing ([B-162](B-162-a-surface-keeps-its-recent-instances.md)), so the old state shows for
the frames its build takes. What closing it would cost, not measured here (no phone in this pass):

- **(a) the focus and still variants built ahead** once a body is shown, so B-162's recent instances hold them from the
  first time: a focusable body's first builds double in the browser. The focus variant is a whole first build (the
  instance, its static field, the first frame and its brush: about 10 ms of CPU for a 328 × 80 card on this M1 by
  B-149's measure, several times that on a phone), the still variant the instance alone (about 4 % of it). Between
  frames, in B-161's steps, so no frame takes it whole, but a screen of twenty controls appearing builds about twice as
  long before every body is real, and each surface keeps up to two more instances alive.
- **(b) the cut itself**, an owner's decision: a crossfade or melt of about 160 ms between the two instances draws the
  body twice for those frames.

## Findings

### 2026-11-23

- **Looked at first** through the seams, every face in «Эфир» and «Копия» at density 2, first frame against the body 40
  frames on (`/private/tmp/b177/before`, `after`, not in the repository). The first frame is read from the stage's own
  recording in that frame: a capture renders a frame of its own, which takes a still body's quick build and shows the
  body instead (so B-168's sheets caught the disabled stand-in only where the build was slow). Pixels differing by more
  than 40 levels, stand-in against body, «Эфир» / «Копия», `main` → this:

  | Face | `main` | this |
  |---|---|---|
  | filled button, disabled | 34 908 / 37 774 | 3 100 / 2 978 (the edge mosaic) |
  | outlined button, disabled | 7 979 / 16 860 | 490 / 618 |
  | filled button, Success | 3 380 / 6 252 | 0 / 0 |
  | outlined button, live | 16 890 / 21 646 | 18 485 / 20 654 (the shadow now drawn; the drips are not) |
  | filled button, Loading | 18 395 / 25 885 | 20 143 / 27 314 (the bands drawn) |
  | live surface, heat 0.8 | 44 212 / 51 757 | 44 199 / 51 776 (now amber, at another tick's flicker) |
  | live surface, Error | 14 201 / 20 187 | 13 462 / 18 665 |
  | live surface, focused | 13 306 / 20 558 | 12 652 / 19 296 |
  | tooltip plate | 5 541 / 10 196 | 5 527 / 9 890 (a live plate: only the slip in «Копия») |

  The live faces' counts are the plate against the drips, which B-149's shape keeps; what changed in them is what
  `StandInStateTest` counts. **F-tooltip-05 is not closed**: the tooltip's plate is a live body whose fill and stroke
  were already right; its drips need the instance. **F-screens-11 is not closed** either: the three Demo screens
  (`DemoScreenFrame`, no dialog, «Эфир», density 1), first frame against the bodies 30 frames on: Library 48 679 →
  48 559, Recording 47 247 → 47 337, Settings 31 833 → 31 609 px — the phone's bodies are live plates; Settings' error
  field now shows its red marks.
- `StandInStateTest` (density 1, the first frame read from its recording, each asserting that the frame was the
  stand-in's), `main` → this: the disabled body's live-fill pixels 8 041 → 0 and its difference from the disabled body
  9 075 → 952 (held under 1 400); Success 1 250 → 0 differing; the outlined body's shadow 0 → 495 px (held over 300), its
  stroke 1 358 → 1 387; a hot body's cold fill 8 041 → 0 and its hot fill 0 → 8 042; the error's red 0 → 254; the
  loading bands 0 → 582; the focus brackets 0 → 160.
