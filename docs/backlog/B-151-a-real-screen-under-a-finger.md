---
id: B-151
title: "A real screen under a finger: tabs and scroll on the phone"
status: wip
priority: P1
size: L
stage: stage-6-later
blocked_by: []
---

# B-151 — A real screen under a finger: tabs and scroll on the phone

The owner, 2026-11-21: take a screen of the Demo, run its wasm on the Pixel 6a, and reach **tabs that switch at once,
with no delay, and a scroll with no drop**. Neither the UI may be simplified nor the Demo changed: the real screen is
the subject, and the kit is what is debugged on it, on the worst platform and conditions there are. Heat is not a
condition (the owner: «на нагрев не обращай внимания»). Means the owner named, beside any found here: forms built into
the components that animate into the target ones as a screen opens (today's static stand-ins may go), a pre-warm, a
diffusion (pixelation) as a body appears, a global cache.

This item is the protocol, the bench and the baseline. Each change made against it is an item of its own, measured by
this protocol and recorded here as well.

## The subject

`?scene=DemoTouch`: the Demo's Library screen as `DemoLibrary` opens it (B-73) — the whole demo app in its 360 × 720
phone, its keyframe loss included — which also tells the bench where its phone stands. The production wasm build, in
Chrome on the Pixel 6a, served through `adb reverse`, nothing emulated or throttled.

## The protocol, pre-registered 2026-11-21 before any change is measured

[`scripts/demo-touch-bench.mjs`](../../scripts/demo-touch-bench.mjs), 4 rounds of 30 s loads, round 1 not counted,
loads rotating:

- **tabs**: a tap (touch down, 40 ms, up) on the navigation bar every 1.5 s, round the four tabs from On air: about 20 a
  load, 60 counted;
- **scroll**: on the On air tab, a 300 px swipe in 160 ms (10 moves), let go moving so the content flings, up then down,
  one every 1.5 s: about 20 a load, 60 counted;
- **blank**, the floor: B-110's ground and Tape with no body (`?scene=Blank`) under the same taps.

Per gesture, from the page's own clock (B-73's recorder plus the gesture's `pointerdown` and `pointerup`): the frame
intervals from the gesture (a tap's `pointerup`, a swipe's `pointerdown`) to 1000 ms after its `pointerup`. Read: the
**longest** interval, and the share of intervals **missed** (over 25 ms: a frame lost at 60 Hz), pooled over the counted
gestures. `latency` (the end of the first frame after `pointerup`) is printed and not read: the tab is composed two or
three frames after the `pointerup` (the baseline below), so it measures the browser, not the screen.

- **Held**, for each of tabs and scroll: the longest interval at most 33.4 ms (no two frames lost in a row) in 95 % of
  the counted gestures and at most 50 ms in all of them, and at most 2 % of the intervals missed.
- **Not held**: anything else, said by which condition.
- **Valid** only if the floor holds all three conditions; otherwise the phone itself cannot show the bar and the run is
  void.

A change's arm is measured beside `main` (`DIST_B`), interleaved, in the same run.

## Baseline, 2026-11-21

[The run](../../logs/b-151/2026-11-21-baseline.log), by the protocol: `main` (`1a31c00`, the sample with the
`DemoTouch` scene), phone at 37.6–38.4 °C. Counted rounds pooled, 60 gestures each:

| load | longest, p50; p95; max | windows over 50 ms | missed | verdict |
|---|---|---|---|---|
| tabs | 116; 333; 483 ms | 53 of 60 | 13.0 % | not held (all three) |
| scroll | 33; 831; 1164 ms | 13 of 60 | 2.7 % | not held (all three) |
| blank | 17; 17; 17 ms | 0 | 0 % | held: **valid** |

The floor holds: the phone draws the Tape at 60 frames a second under the same taps. The screen does not, on either
gesture. A scroll is smooth between the keyframe losses and stops at each: once in 9 s a single frame of 750–1160 ms,
the loss's start.

**Where a tab switch goes** ([the profile](../../logs/b-151/2026-11-21-profile-tabs.txt): the CDP profiler every 200 µs over a 30 s tabs load of a build whose Kotlin module was
rebuilt by the same `wasm-opt` passes plus `-g`; the profile lined up with the page's clock by a marker; 16 taps after
the first four, the 700 ms after each): 543 ms of main-thread work per switch.

- Composition and layout: under 20 ms a switch. Compose is not where it goes.
- The generator's `frame`: 211 ms a switch — a first frame of every new body built ahead of its first draw, then built
  again in the draw that takes it, then the next tick's built ahead.
- `sin`: 84 ms a switch, most of it Kotlin's own `fdlibm` reducing the lattice hash's large arguments
  (`__ieee754_rem_pio2`); the design's generator calls JavaScript's `Math.sin`.
- `shaderOf`: 66 ms a switch — every new body's brush and its still layers' image.
- The keyframe loss's draw over the new content: 51 ms a switch.

A tab's first frame comes two or three frames after the tap and takes 100–190 ms; three to six heavy frames follow it
as the bodies arrive.

- Anchors: `scripts/demo-touch-bench.mjs`, `sample/src/wasmJsMain/kotlin/io/github/youndie/mutant/sample/BenchScene.kt`

## Findings

### Round 1, 2026-11-21: the levers

Each lever is an item, measured against the arm before it; the phone's runs are [arms 1](../../logs/b-151/2026-11-21-arms-1.log)
(a comparison: no floor, the phone throttled to 0.5–1.6 GHz) and [arms 2](../../logs/b-151/2026-11-21-arms-2.log) (by the
protocol, valid); the rest are the phone proxy (`PHONE=1 CPU=2`, 2 counted rounds of 20 s), which read `main` as the
phone does (tab switches longest p50 117 ms and 12 % missed, a scroll stopped up to 1.2 s at the loss).

| item | what | where it was measured | tab switches, longest p50 / p95 | scroll, longest p95 |
|---|---|---|---|---|
| [B-152](B-152-the-browsers-own-sine.md) | the browser's `Math.sin` | arms 1 | 150 / – (main 216) | – |
| [B-153](B-153-a-first-picture-between-frames.md) | a first picture between frames, whole | arms 1 | 83 / – | – |
| [B-155](B-155-no-tick-built-in-a-frame.md) | no tick built in a frame | arms 2, valid | 50 / 150 (main 133 / 283) | 449 (main 1196) |
| [B-156](B-156-a-pool-of-bodies.md) | a pool of bodies left behind | arms 2 | 67 / 183 | 449 |
| [B-154](B-154-the-loss-snapshots-the-band-only.md) | the snapshot: the band's bodies, each frame built once | arms 2 | 67 / 150 | 200 |
| [B-157](B-157-textures-in-one-block.md) | textures in one block | proxy, at rest | busy −14 points | – |
| [B-158](B-158-the-loss-band-in-memory.md) | the loss's band in memory | proxy | – | 350, missed 0.2 % |
| [B-159](B-159-the-loss-snapshot-painted-ahead.md) | the snapshot painted ahead | proxy | 33 / 133 | **33, held** |
| [B-160](B-160-a-frame-kept-between-ticks.md) | a frame kept between ticks | proxy | no change | held |
| [B-161](B-161-builds-in-steps.md) | builds in steps | proxy | no change | held |
| [B-162](B-162-a-surface-keeps-its-recent-instances.md) | recent instances, a new one between frames | proxy | **17 / 67**, missed 0.7 % | held |

Where it stands, in the proxy: **a scroll holds** (no window over 33 ms, 0.1 % missed); **a tab switch holds on a return
to a tab** (one frame lost, at most) and not on a tab's first visit in a page: the frame that composes it takes 65–100
ms in the proxy, of which Compose's own draw of the new nodes 17, text 10, composition 5, layout 3, and Skia's work that
the profile cannot name (its module carries no names), with GPU programs compiled for it. The bodies are not in that
frame any more: they come as stand-ins and are built after it, a few milliseconds at a time.

Owed: the run by the protocol on the phone with all of it, against `main` (the phone was disconnected the evening of
2026-11-21, between the last proxy run and it); and a first visit's own frame.
