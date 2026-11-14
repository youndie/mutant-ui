---
id: B-09
title: "MutantTheme: «Эфир» and «Копия», the cassette seed, reduce motion as pause, «Чистый сигнал»"
status: wip
priority: P1
size: S
stage: stage-1-materials
blocked_by: [B-07, B-08]
---

# B-09 — MutantTheme: «Эфир» and «Копия», the cassette seed, reduce motion as pause, «Чистый сигнал»

Two palettes, a cassette seed every instance key is prefixed with, and two switches the brief makes mandatory: reduce motion, and the «Чистый сигнал» mode the design shows only as a label on the settings demo (research §1.7, D7).

- **The decision and its reason.** `MutantTheme(colors, seed, reduceMotion: Boolean? = null, cleanSignal = false, strings)`; `reduceMotion` defaults to the platform through `expect`, `false` on desktop, and means the handoff's pause: the clock stops, damage stays, toggles are instant. `cleanSignal` turns off the Tape and idle glitches without stopping the clock; "generation 0" has no gene behind it and is not claimed.
- «Копия» shifts fills by `slip` 1.5 dp, as the genome row says.
- Rejected: the brief's 100 ms fades under reduce motion (the handoff is the later word).

- AC: a fixture forces either switch; under reduce motion no frame changes after the first and every toggle lands in one frame; the two palettes switch every component; a test reads the platform default on each target.
- Anchors: `reference/design-system/mutant-tokens.json`, `reference/design-system/brief.md`, `reference/design-system/mutant-ui Демо.dc.html`
