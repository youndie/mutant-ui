---
id: B-69
title: "Measure what a tall body costs per tick"
status: open
priority: P3
size: S
stage: stage-5-product
blocked_by: [B-40]
---

# B-69 — Measure what a tall body costs per tick

Found in [B-40](B-40-qa-pass-and-real-window.md) (F20), suspected only. The distance field grows with the body's area; a 360 × 3 000 dp card cost 64–78 ms per tick in the test runtime against 16 ms for 360 × 200, and those numbers include `captureToImage`.

- **The decision.** Measure first, on the real frame clock with B-06's harness, before changing the generator. The rejected alternative is capping the grid now, which changes every tall body's drawing on a suspicion.
- AC: the cost per tick of 200, 1 000 and 3 000 dp bodies measured in a real window, with a verdict: a new item for a fix, or closed with the numbers.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`
