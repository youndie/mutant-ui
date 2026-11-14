---
id: B-09
title: "MutantTheme: «Эфир» and «Копия», the cassette seed, reduce motion as pause, «Чистый сигнал»"
status: done
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

## Result (2026-11-14)

- Public: `MutantTheme(colors = MutantPalettes.Efir, cassette = "5", reduceMotion: Boolean? = null, cleanSignal = false, content)` and the object `MutantTheme` (`colors`, `cassette`, `reduceMotion`, `cleanSignal`), recorded in the ABI dump. The theme provides the palette, the cassette, the fonts and one `MutantClock` for its subtree; `reduceMotion` pauses that clock (set at construction and by `SideEffect`, never written in composition); `null` reads the platform through `platformReduceMotion()` — the animator scale on Android, `UIAccessibilityIsReduceMotionEnabled` on iOS, the media query in a browser, off on desktop (the actuals are plakat-ui's).
- `cleanSignal` is carried in `LocalMutantCleanSignal` for the Tape ([B-13](B-13-tape.md)) and idle glitches; the engine implements no idle glitch and no generation, so today it changes nothing a body draws, and the KDoc says only what it promises.
- The print theme's 1.5 dp slip was already the surface's default for bodies that are not round (B-10).
- `MutantThemeTest` (4, desktop): reduce motion off by the platform on desktop; under reduce motion a button builds no frame after its first and draws the same picture after 3 s; without it, ≥ 20 frames are built and the picture changes; a card is filled with each palette's surface. Mutants, each fails a named test: the clock never paused; the palette ignored; the desktop default turned on.
- Not run: the Android, iOS and wasm actuals beyond their compilation in `check`; toggles landing at once under reduce motion arrive with the toggles ([B-12](B-12-transitions.md)).
