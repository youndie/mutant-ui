---
id: B-108
title: "A phone's budget for the boil"
status: question
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-108 — A phone's budget for the boil

Found by [B-107](B-107-wasm-on-a-phone.md). On a Pixel 6a, in Chrome, the sample's pages with motion run at 8–11
frames a second, the main thread working 108–125 ms a frame; the Tape is down to about 2 ms of it. A profile of the
resting Components page there puts three quarters of the main thread in drawing and a fifth in the body generator,
built for every visible body on its tick ([profile](../../logs/b-107/2026-11-17-pixel6a-profile-ComponentsRest-named.txt)).
With the Tape off the page still runs at 10–12 frames a second, so no change to the Tape reaches 60.

- **What would.** Fewer bodies boiling at once, or a slower resting tick, under a narrow window or a coarse pointer, as
  toon-ui's idle budget caps how many decorative components breathe at once. It changes what a phone shows: bodies
  past the budget stand still between ticks.
- **The alternatives.** Cheaper drawing of a body (fewer path points, caching a body's picture between ticks), which
  keeps what a phone shows and is a measured item of its own; or accepting the phone's rate as it is.

## Question for the owner

Should a phone get a budget for the boil, and if so, what may it change: how many bodies boil at once, how often they
step, or neither, and the drawing is made cheaper instead?
