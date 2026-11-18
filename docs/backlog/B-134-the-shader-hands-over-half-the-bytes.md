---
id: B-134
title: "The body shader hands Skia half the bytes"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-134 — The body shader hands Skia half the bytes

Part of [B-126](B-126-bodies-on-the-gpu.md), found while looking into the components page's jerky scrolling after
[B-133](B-133-the-keyframe-loss-freezes-a-shader-page.md).

## What the profile says

The shader build's components page, at rest in the phone proxy (Chrome, CPU ×2), its wasm optimised as the build does but
with names kept: of the busy time, building ahead 35.5 %, of which `Image.makeRaster` 17.8 % and the field 17.5 %. Skiko in
the browser copies a `ByteArray` into Skia's memory a byte a call through JavaScript (`skia_memSetByte`): **5.8 million
calls a second** on that page, about 730 kB a tick. A body's field is most of it, four bytes a node: a button's grid at
0.9 dp is about 195 × 111 nodes, 86 kB a tick; the flow lines' segments texture is 4 kB a body however few they are.

## The decision

- **The field in two bytes a node**, an `R8G8_UNORM` texture: the value in 14 bits over ±32 (0.004 dp, a tenth of a pixel
  at 2.625 is 0.038 dp) and the two near flags in the low bits. Eight bits a channel stay exact in half precision.
  *Built as `RGB_565`*: Skiko's `R8G8_UNORM` makes no image with any alpha type, `RGB_565` does; the same 16-bit word in
  5-6-5 channels, which round back exactly.
- **The segments texture as wide as its entries** when they fit one row. *Then, on the numbers below*: each segment listed
  once, in a table of 16-bit words, and the bins' entries naming it by index; a bin held a full copy of every segment it
  reached, a card's segments texture was 28 kB a tick, the field 41 kB and the bins 14 kB.
- Rejected: copying in one piece through either module's memory (neither glue exposes it to the kit's JavaScript; a patch
  of Skiko's internals); one byte a node (8 bits over ±32 is 0.25 dp, too coarse at the crossings).

## Set before the first render

- `BodyShaderParityTest` holds as it stands: its silhouette test's four known cases at their 0.6 %, the rest at 0.5 %; the
  decorated test at 0.8 %.
- **The calls, in the proxy**, the components page at rest, B-133's build against this one: **at most half** the
  `skia_memSetByte` calls a second.
- **On the phone, hot**, one rotation, B-133's shader build then this one, `ComponentsRest` six rounds of ten seconds, round
  1 not counted: **it holds** if this build's pooled frames a second are higher, and its work p50 lower in every counted
  round.

## Results, 2026-11-18

- **The picture.** `BodyShaderParityTest` as it stood: the silhouette 0.266–0.586 %, the four known cases under their 0.6 %
  (button-kopiya-1.0 moved from 0.567 to 0.575), the decorated 0.27–0.59 % under 0.8 %
  ([silhouette](../../logs/b-134/2026-11-18-silhouette-shares.txt), [decorated](../../logs/b-134/2026-11-18-decorated-shares.txt)).
  Mutants swapping the word's bytes (40–49 % of pixels apart) and reading a segment at the wrong stride (to 1.17 %) fail it.
- **The calls** ([counts](../../logs/b-134/2026-11-18-byte-copies.txt), [the counter](../../scripts/count-skia-byte-copies.mjs)
  over a copy of each build whose glue counts them): B-133's build 5.82 million a second; **the first cut, the field alone in
  two bytes, 3.03 million, 52 %: over the line**; with the segments listed once, **2.88 million, 49.4 %: under it.**
- **The phone** ([run](../../logs/b-134/2026-11-18-pixel6a.log)), 31.6–34.3 °C, on mains: B-133's build 48.3 frames a second
  pooled, **this one 51.6**, work p50 lower in every counted round (by 0.5 to 2.3 ms, 1.08 on average), the main thread 8 points
  less busy. **It holds.**
