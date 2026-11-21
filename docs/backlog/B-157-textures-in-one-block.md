---
id: B-157
title: "In the browser, a body's textures reach Skia in one block"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: [B-152]
---

# B-157 — In the browser, a body's textures reach Skia in one block

From [B-151](B-151-a-real-screen-under-a-finger.md)'s profile of the Library screen at rest in the phone proxy: of the
brush built at every tick of every body (`shaderOf`, 2.6 s of 10), 1.5 s was Skiko moving the field's and the flow lines'
bytes into its memory one byte a call across JavaScript (`Image.makeRaster` → `toInterop` → `skia_memSetByte`), and the
still layers' shader was made anew at every tick, from a mutable bitmap Skia copied each time. B-134 halved the bytes
and rejected a copy in one piece because neither glue exposed its memory; field-ui has since done it (`skiko.mjs`
exports its emscripten module, `loadedWasm`, and a `@JsFun` sees this module's `wasmExports`).

- **The decision**: `rasterImage` writes a texture's bytes into this module's linear memory and copies them into a fresh
  bitmap's pixels in Skiko's memory with one `Uint8Array.set`; the bitmap is immutable, so the image shares it. The still
  layers' shader is made once for their image (`StillImage.platformForm`), the image's bitmap marked immutable.
- AC:
  - the Library screen draws as before (a screenshot in the proxy, every body, flow line and still layer in place);
  - every test unchanged (the shader's parity is a desktop test; this is the browser's actual alone);
  - measured against the arm before it.
- Anchors: `mutant-core/src/wasmJsMain/kotlin/io/github/youndie/mutant/surface/RasterImage.wasmJs.kt`,
  `mutant-core/src/wasmJsMain/kotlin/io/github/youndie/mutant/surface/BodyShader.wasmJs.kt`

## Findings

### 2026-11-21

- Phone proxy (`PHONE=1 CPU=4`, B-73's bench, the Library screen at rest, 3 counted rounds of 8 s, against the arm with
  B-152…B-156): the main thread's busy share −14.4 points (−14.66, −14.60, −14.07), work p50 −2.0 ms.
