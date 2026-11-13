---
id: B-03
title: "Generator: a 1:1 common-code port of the resting frame, held to Node goldens on every target"
status: open
priority: P0
size: L
stage: stage-0-risks
blocked_by: [B-01]
---

# B-03 — Generator: a 1:1 common-code port of the resting frame, held to Node goldens on every target

The shape of every component is the output of `mutant-generator.js`. The vendored golden reproduces exactly except three hot flow-line entries, and it is too small to hold a port: six instances, nine frames (research §1.3, §1.4).

- **The decision and its reason.** Port `hash`, `rng`, `h21`, `vn`, `ss`, `smin`, `ucap`, `ty`, `bodyD`, `rrD`, `taper`, `mkHook`, `mkArc`, `markAt`, `marks`, `markPath`, `dyn`, `edgeMos`, `deadPx`, `genItem`, `prims`, `bubbles`, `field`, `march` and `pathBuf` into `io.github.youndie.mutant.gen`, structure and stream order kept, names spelled out (D3). The field is `FloatArray`; rounding where a string is reproduced is `floor(v * 10 + 0.5) / 10`; the field's static-half cache and the march buffer belong to the instance.
- A Node script loads the vendored generator untouched and writes the repository's own goldens: every profile of `static K`, several keys and sizes each (both grid steps, `calm` 0.3 and 1.7, `slip`, `dead`, `edge` off), rest frames at several ticks, hot frames at heat 0.5 and 1. The gate runs it with `--check`. The hot flow lines follow the engine (D4).
- Rejected: porting from the handoff's description, or holding the port only to the vendored golden. Not covered: venom, pour, error blocks ([B-04](B-04-transition-geometry.md)).

- AC: goldens regenerate byte-identically and `make check` fails when they are stale; the port matches them on JVM, Native and wasm, exactly to four decimals for hash, rng, noise and instances and within ±0.1 dp per path coordinate; the vendored golden passes except the three entries `scripts/design-golden.mjs` lists; a mutant in each of FNV-1a, mulberry32, `h21`, `smin` and the march is caught.
- Anchors: `reference/design-system/mutant-generator.js`, `reference/design-system/mutant-golden.json`, `reference/design-system/mutant-engine.js`, `scripts/design-golden.mjs`
