# B-168 — checks-switch: `MutantCheckbox`, `MutantRadioButton`, `MutantSwitch`

Group 4 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-checks-switch` from `7a8e691`,
2026-11-22, on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand). All three live in
`mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCheckbox.kt` (checkbox and radio) and
`MutantSwitch.kt`; their numbers were decided in [B-18](../../docs/backlog/B-18-checkbox-and-radio-button.md) and
[B-19](../../docs/backlog/B-19-switch.md).

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-checks-switch/` (not in the
repository).

- **The components as the sample composes them** (`core/`): the sample's three cells of each (`Checkboxes`, `Radios`,
  `Switches` in `ComponentsSelection.kt`: two live ones with their keys and a disabled one), side by side on the page's
  colour, in eight looks: «Эфир», «Копия», reduce motion, «Чистый сигнал», `steppedMotion`, density 2, and the
  browser's way of drawing (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu`
  true, `LocalMutantBodyAsImage` false) in both palettes. Per look and component, every frame captured, sheets of each
  control zoomed 3× (2× at density 2), and a per-frame pixel diff:
  - appear (`<kind>-<look>-appear-r0..r2`);
  - a mouse click on each live control in turn — checking, unchecking; selecting the other radio
    (`…-toggle-r0`, `…-toggle-r1`);
  - a second click 96 ms into a running transition (`…-interrupt-`);
  - a held press of 480 ms, its release (which toggles) and the mouse resting and leaving (`…-press-`);
  - `enabled` false and back (`…-enable-`, with the label's pixels counted in `…-labels.txt` and whole frames
    `…-enabled-full.png`, `…-disabled-full.png`);
  - leaving the page and coming back with another state (`…-back-`, B-166's face);
  - two seconds at rest (`…-rest-`).
  And probes: the body's colour frame by frame across a press released into a toggle, and the pixels a hover changes
  under reduce motion (`core/probes.txt`).
- **The sample itself** (`sample/`): `MutantUiSample(SampleSettings(group = Selection))` in «Эфир» (English and
  Russian) and «Копия», each live control clicked (`selection-*.png`); the demo's Settings screen
  (`DemoScreenFrame(Settings)`) with the speed radios, both switches and the checkbox clicked (`settings-*.png`).

A mouse press switches the test window to touch mode, so no click leaves keyboard focus brackets behind; keyboard focus
itself is held by `KeyboardFocusSweepTest`, and was not filmed again.

## Findings

### F-checks-switch-01 — unchecked, the checkbox's mark vanishes at once instead of melting out — local

- **What.** On uncheck the tick and the two burnt blocks are gone in the first frame, while the fill pours away
  over 370 ms. The mark melts in on check, but never out: `CheckBoxControl` tints the tick transparent and stops
  drawing the blocks as soon as `checked` is false, so the melt-out runs over nothing. The radio's dot and the
  switch's echo, on the same `mutantMelt`, do melt out.
- **Frames.**
  - `core/check-Efir-toggle-r1.png`: tick and blocks whole at 2512 ms, gone at 2528 (1 113 pixels change in that one
    frame), an empty acid body until the pour reaches it.
  - The same in every look (`core/check-*-toggle-r1.png`), and on a click 96 ms into a check
    (`core/check-Efir-interrupt-r0.png`, 3280 → 3296 ms).
  - The demo's Settings screen: «Head noise» unchecked, `sample/settings-efir.png`, 1024 → 1120 ms.
  - Against `core/radio-Efir-toggle-r1.png`, 1760–2048 ms: the dot sinks and stretches down out of its body.
- **The design.** Every piece of the mark is `data-on-only` on the page; the engine's `toggle` melts each of them, on
  and off (`melt(x, on)`: «иначе стекает вниз»), and the handoff's motion table has `melt-out`, 330 ms, «индикатор
  оплывает вниз». B-18 states it: «the mark melts in and out».

### F-checks-switch-02 — a control turned disabled keeps its label's colour — reported (`MutantText.kt`, F-buttons-02's face)

- **What.** `enabled = false` on a live checkbox, radio or switch greys the body (no signal) but leaves the label in
  the text colour, and enabled again a disabled one keeps its grey. `ControlRow` gives `MutantText` the disabled
  colour, which a colour-only change does not redraw (F-buttons-02).
- **Numbers.** The label's pixels near the text colour, of the two live rows, before / disabled / enabled again,
  unchanged in every look: checkbox 167 and 220 («Эфир»), 109 and 143 («Копия»); radio 89 and 102, 63 and 72; switch
  198 and 148, 119 and 90; the pixels near the disabled colour likewise unchanged (`core/*-labels.txt`). Whole
  frames: `core/switch-Kopiya-disabled-full.png` — «Clean signal» and «Frame loss» in ink beside the grey «Auto
  tracking» composed disabled.
- **Reach.** Only a consumer flipping `enabled`: the sample composes its disabled cells disabled from the start.

### F-checks-switch-03 — a press released into a toggle drops the press's heat for a pour's first steps — reported (`surface/`: the pour draws its old fill without the heat)

- **What.** The kit heats a checkbox's and a switch's body on press (they share the row's interaction source with
  their body, as B-63's hover does). Released, the press toggles the control, the fill starts to pour, and the old
  fill under the front is drawn cold: the amber body turns to the cold surface colour in one frame, stays so until the
  front passes, and the new fill comes in hot.
- **Numbers** (`core/probes.txt`, a pixel low in the body): the checkbox after 480 ms held, `#ffb000` at the last
  pressed frame, `#2f3960` from the next for 160 ms, then `#ebc507` (hot acid) cooling to `#cfe411`; the switch the
  same from its track. A plain click (48 ms held) shows the same drop at lower heat.
- **Frames.** `core/check-Efir-press-r1.png` and `core/switch-Efir-press-r1.png`, 4528 → 4576 ms.
- **The design.** The page's toggles carry no `data-press`: the engine never heats them, so it never meets a pour
  under heat. Whether a toggle should heat on press at all is not settled by the design or by B-18/B-19 (B-63 decided
  the hover simmer only); the chip, which does press and pour, has the same path.

### F-checks-switch-04 — a click back within a pour's first half jumps the box from two thirds poured to the old fill — reported (`surface/BodyMoves.kt`, B-94's rule)

- **What.** Uncheck, then check again 128 ms later: the box, two thirds covered by the new (dark, here hover-warmed)
  fill, is the acid it started from in the next frame (1 111 pixels in one frame).
- **Frames.** `core/check-Efir-interrupt-r0.png`, 3424 → 3440 ms.
- **Cause.** B-94 pours a change back from the pour's own old fill while the pour's progress is under one half. The
  progress is the stepped time (83/370 = 0.23 here), but the 22 dp box's front at that progress already covers about
  two thirds of it, so the old fill is no longer what covers most of the body. The rule and the front are shared.

### F-checks-switch-05 — under reduce motion the mouse's arrival and departure redraw a body — reported (`surface/`, a face of F-buttons-06)

- **What.** With the clock stopped, the mouse entering a live control's row and leaving it each change the body's
  edge: 169 pixels on the checkbox, 210 on the radio, 342 on the switch, and back on leave (`core/probes.txt`). The
  hover marks the body hot, which draws it at the hot tick's time instead of the resting one; the edges move by under
  a pixel (`core/ra.png` against `core/rc.png`). A press adds nothing (F-buttons-06).

## Looked at and found right

- **Check, select, switch on and off**, in every look: the acid pours down in 4–5 steps of 83 ms, the tick melts in
  from the top, the radio's dot and echo melt in and sink out, the thumb slides 24 dp with its stretch and the echo
  ring melts in where it stood, and out again on switching off (over the thumb, as B-19 decided). Under reduce motion
  each toggle lands in one frame; under `steppedMotion` the slide and the melt step at 83 ms.
- **«Копия»**: the tick dark on acid, the slip's double outline as the page draws it; **density 2**: every mark, block,
  dot and echo where it stands at density 1, nothing off by a unit.
- **The browser's way of drawing**: per-frame diffs within a few pixels of the desktop's through every scenario; a
  control back on the page in another state draws that state from its first frame after the generic stand-in
  (B-149), with no old fill (B-166's class not seen here).
- **At rest**: the boil's steady 500–1 700 pixels per tick, no frame jumping out (`core/*-rest.diff.txt`).
- **The sample**: the three sections and the Settings screen as composed; the three speed radios share their key
  `radio`, as the design's Demo page does.
- **Hover**: the body simmers to amber and cools when the mouse leaves (B-63).
