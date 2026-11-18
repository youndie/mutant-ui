---
id: B-135
title: "A body ticks in its own layer"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-135 — A body ticks in its own layer

Part of [B-126](B-126-bodies-on-the-gpu.md), after [B-134](B-134-the-shader-hands-over-half-the-bytes.md): the components page
scrolls jerkily in the shader build as in the paths' (20–33 frames a second hot, [B-133](B-133-the-keyframe-loss-freezes-a-shader-page.md)).

## What the profile says

The shader build of B-134, its wasm with names kept, in the phone proxy (Chrome, CPU ×2): **recording graphics layers is
33 % of the busy time at rest and 47 % scrolling.** The page's content is one layer (`MutantKeyframeLoss` puts it in one, so
a loss's step redraws its band only), and Compose records every child of a scrolling column into it, seen or not. A body's
tick invalidates its drawing, so every tick of any body records the whole page again: every paragraph of text, every
pattern swatch of the foundations (6–7 % of the busy time, hundreds of rectangles each, nowhere near the screen), every
other body's cached drawing.

Two things found on the way:

- **The frame probe lied for a body the shader draws**: it reported every tick as built in the frame. Fixed;
  `ScrolledShaderTicksAheadTest` holds it, a mutant restoring the lie fails it (0 ticks ahead, 8 in the frame).
- **Refuted: scrolling cancels the builds ahead.** A build ahead is given a window band and rebuilt when the window leaves
  it (B-86); a body the shader draws has no image for the band to shape, so that looked like waste. The same test scrolls a
  body back and forth 30 px a frame on the desktop: every tick is still taken ahead. In the browser, ticks scrolled are built
  in the frame (14 % of the busy time) because the one thread is full and builds ahead run as tasks of lower priority
  ([B-111](B-111-what-the-decorations-cost-on-a-phone.md)'s finding): the load is the cause, not the band.

## The decision

A body's drawing and its content in a layer of their own (`graphicsLayer()` before its `drawWithCache`), so a tick records
that layer and nothing beside it. Rejected: a layer for the pattern alone (it fixes one sibling of many).

## Set before the first render

- The code gate as it stands: every golden and parity test unchanged.
- A test: a body's ticks do not redraw what is drawn beside it.
- **On the phone, hot**, one rotation, B-134's build then this one: `ComponentsRest`, six rounds of ten seconds, round 1 not
  counted; then the touch scroll of `scripts/phone-touch-scroll.mjs`, each build twice, alternating. **It holds** if this
  build's pooled frames a second at rest are higher and its work p50 lower in every counted round, and its frames a second
  scrolled are higher in both pairs.

## Results, 2026-11-18

- **The code.** `TickInOwnLayerTest`: a box beside a ticking body is drawn once, where it was drawn again at each of the
  body's 8 ticks before; a mutant taking the layer away fails it. The code gate is green as it stood, every golden and
  parity test unchanged.
- **The phone at rest** ([run](../../logs/b-135/2026-11-18-pixel6a.log)), 31.6–34.5 °C: B-134's build 51.4 frames a second
  pooled, **this one 53.5**, the main thread 5.9 points less busy on average (0.4 to 10.2 a round); but work p50 lower in four
  counted rounds of five, 0.1 ms higher in the other. **Not held**: the rule asked every round.
- **The phone scrolled by touch** ([run](../../logs/b-135/2026-11-18-pixel6a-touch-scroll.log)), 34.5–36.0 °C, B-134 then
  this build then this build then B-134: 47.6 and 45.5, then 40.6 and 35.0 frames a second. One pair each way; the phone
  warmed through the run and the order moves more than the build does. **Not held.** Scrolled, both builds now stand at
  35–48 frames a second where B-133's stood at 20–33, a degree or two hotter; not one run, so not a comparison.

## The question, answered

The layer does what it was for — a tick no longer records the page, and the phone's main thread is 6 points less busy — but
by the rule set before it, it is not shown in frames a second on the phone: grey. Keep it, or take it out? The protocol
for scrolling needs more pairs than two to read a difference of a few frames a second through the phone's warming.

**The owner, 2026-11-18: keep the layer.** The item is done on that decision, its rule not met on the phone (grey), the
numbers above as they are.

