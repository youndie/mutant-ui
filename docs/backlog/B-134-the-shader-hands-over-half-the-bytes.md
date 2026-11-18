---
id: B-134
title: "The body shader hands Skia half the bytes"
status: wip
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
- **The segments texture as wide as its entries** when they fit one row.
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
