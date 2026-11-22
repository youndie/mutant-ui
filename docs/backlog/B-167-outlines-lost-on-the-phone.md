---
id: B-167
title: "On the phone, the bodies the shader draws have no outline"
status: wip
priority: P0
size: S
stage: stage-6-later
blocked_by: []
---

# B-167 — On the phone, the bodies the shader draws have no outline

The owner, 2026-11-22: «на телефоне на wasm у switch нет окантовки у кружка в selected» — on the Pixel 6a in Chrome
the chosen switch's thumb has no outline; and asked why the goldens did not see it.

**What it is**: no body the shader draws has its outline on the phone — the switch's thumb and track, the cards, the
chips, the button, the bar ([the screenshots](../../logs/b-167/)); a body drawn otherwise (disabled) has it. The
desktop, the goldens and headless Chrome on the Mac draw it.

**Why**: the shader reads its field from a texture whose texel is a 16-bit word: the value in its top 14 bits, and in
its low two whether the node is near the contour (the outline's search) and near the highlight. A texture's channel is
a `half`, and the word was summed straight from the channels (`floor(float(t.r) * 31.0 + 0.5) * 2048.0 + …`): an
expression of halves, which the phone's GPU works out at half precision — where a number past 2048 keeps 11 bits, and
the word's low ones, its flags, are lost. The fill reads the high bits and is right; the outline, searched only where
the flag is set, is never drawn. Three variants on the phone told the causes apart: the same texture in `RGBA_8888`, and
the outline's search in one cell instead of two, both still drew no outline, as the 5-6-5 texture did.

**Why the goldens did not see it**: the goldens and every test draw on the desktop, at full precision, by paths (the
shader is the browser's since B-126); headless Chrome on the Mac computes at full precision too, and none of its
precision emulations (`--emulate-shader-precision` over Metal or SwiftShader) shows it.

- **The decision**: a channel becomes a number only in `c5`, `c6` and `c8`, which return it as a float under 256; the
  value, the flags and the words (the flow lines' tables, the bins' 16-bit numbers) are summed from those. At full
  precision every result is what it was.
- AC:
  - `BodyShaderPrecisionTest`: no half is made a number outside the helpers, and the field, the flags and the words are
    read through them — both failing on `main`'s shader;
  - the shader's parity and every other test and golden unchanged on the desktop;
  - the phone draws the outlines (a screenshot).
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyShader.kt`

## Findings
