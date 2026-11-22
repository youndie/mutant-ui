# B-168 — buttons: `MutantButton`, `MutantOutlinedButton`, `MutantTextButton`

Group 1 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-buttons` from `c42af01`, 2026-11-22,
on this Mac (M-series, the desktop JVM, `runComposeUiTest` with the clock driven by hand).

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-buttons/` (not in the repository).

- **The sample's section** (`sample/`): `MutantUiSample(SampleSettings(group = Actions))`, the page scrolled to the
  Button section, which was captured every frame for 2 s (a sheet every 48 ms) and then while its rest cell was pressed
  by the mouse, held 384 ms and released: «Эфир», «Копия», reduce motion, «Чистый сигнал», `steppedMotion` and Russian.
  Density 2 could not be captured whole (the section is taller than the test window at 2), so density 2 was looked at
  in the component stage below. Sheets `sample/<look>-N.png`, per-frame pixel diffs `sample/<look>.diff.txt`.
- **The components as the sample and the demo screens compose them** (`core/`), on the page colour, each in eight looks:
  «Эфир», «Копия», reduce motion, «Чистый сигнал», `steppedMotion`, density 2, and the browser's way of drawing
  (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true, `LocalMutantBodyAsImage`
  false, as `FillChangeOnGpuTest`) in both palettes:
  - press, hold, release and leave of each kind by the mouse (`press-<kind>-<look>`);
  - Idle → Loading → Success → Idle, with no width and at the page's 200 dp (`state-free-`, `state-fixed-`);
  - enabled → disabled → enabled for all three (`enable-`);
  - the demo's label changes: Listen/Pause at a weighted width, «Erase n» with its count and `enabled`, the sort
    button's label (`labels-`);
  - leaving the page and coming back disabled, then Success, then Idle — B-166's face (`back-`);
  - a press and the mouse on a Loading, a Success and a disabled button (`busy-`);
  - keyboard focus moved through Loading and Success, then by Tab to the outlined and the text button (`focus-`).

A touch injected by the test leaves the window in keyboard input mode, so the button the touch focuses draws its focus
brackets and calms its body for the rest of the test. The mouse switches the window to touch mode, as a real window
does (`before=Keyboard after=Touch`), so every press here is the mouse's.

## Findings

### F-buttons-01 — a Loading button answers a press and the mouse it ignores — local

- **What.** Since B-62 a Loading button keeps its `clickable` enabled, so that keyboard focus survives the work. Its
  presses and hovers reach the body with it:
  - pressed, the body boils to amber, as a live button does;
  - under the mouse it simmers (B-63's hover heat);
  - it shows the hand cursor.
  It looks pressable and takes no click. A user who clicks a button and keeps the mouse over it sees it simmer through
  the whole of its work.
- **Frames.** `core/busy-loading-efir-0.png`, `core/busy-loading-kopiya-0.png`: amber from 1120 to 1600 ms under a
  press, a simmer to 2096 ms with the mouse still over it.
- **The design.** The page's Loading slot (`data-state="loading"`) carries no `data-press`: the engine never heats it.
  B-62 decided only that it keeps focus and takes no click.

### F-buttons-02 — a text button turned disabled keeps its accent label — reported (`MutantText.kt`)

- **What.** `MutantTextButton(enabled = false)` after being enabled draws its label in the accent colour still, and
  enabled again after disabled keeps the grey. The divider and everything else change; the word does not, until
  something else redraws its layer.
- **Frames.** `core/enable-efir-0.png`, `core/enable-kopiya-0.png`: «Reset» stays acid in «Эфир» and blue in «Копия»
  through every "off" frame, while the filled and the outlined buttons beside it lose their signal. A pixel count of
  the label alone: 307 accent pixels and 0 grey, enabled and disabled alike.
- **Cause.** `MutantText` draws its kept layout in a `drawBehind` lambda that captures only the layout holder, the
  colour producer and the overflow. Compose memoises that lambda, so a new style whose only change is its colour
  remeasures the text to the same size and never invalidates the draw. Confirmed by a temporary patch (reverted): the
  lambda reading `style.color` turns the count into 0 accent and 307 grey.
- **Reach.** Any `MutantText` whose colour alone changes where nothing else redraws its layer. The text button is the
  bare case: no body under it. The filled and the outlined buttons' words follow because their body redraws.
- **The item that decided it.** B-164 (kept layouts); the disabled text button's grey is D15's (B-15).

### F-buttons-03 — in the browser, a disabled button back on the page shows its live plate for a frame — reported (`surface/StandIn.kt`)

- **What.** In the browser's way of drawing, a filled button that left the page and came back disabled drew its first
  frame as an acid plate. The outlined one drew a plain dark plate with no accent outline. The stand-in draws the
  body's fill and the theme's ink whatever the state (no signal) and the stroke.
- **Frames.** `core/back-gpu-0.png` at 1008 ms, against `core/back-efir-0.png` (the desktop's way, no stand-in).
- **The item that decided it.** B-149 (the stand-in), B-166's class (a stale look for a frame).

### F-buttons-04 — a button given no width grows 32 dp when it turns Success — reported (a look the design does not settle)

- **What.** Success adds the check and its 8 dp gap before the label. A button sized by its label is 32 dp wider in
  Success and narrower again in Idle, and its body is generated anew at each size (D17).
- **Frames.** `core/state-free-efir-0.png` from 1936 ms, and back at 2256 ms. At the page's fixed 200 dp
  (`core/state-fixed-*.png`) nothing moves.
- **The design.** It draws every button at a fixed width (200 and 160 px), so it never meets this. B-15 decided the
  label's 24 dp padding for the kit's own width (D15), and nothing about a width across states.
- **For the owner, the options:**
  - leave it, and say in the KDoc that a button changing state should be given a width;
  - reserve the check's room in every state, which widens every button without a width by 32 dp and moves its goldens;
  - never let the button shrink below the widest width it has shown.
  A consumer's Loading words («Перемотка…») change the width anyway.

### F-buttons-05 — the text button has no press look — reported (a look the design does not settle)

- **What.** Pressing a text button changes no pixel (`core/press-text-*.diff.txt`: 0 on every frame). Only the hand
  cursor says it is a control; the keyboard has its brackets (B-57).
- **The design.** The page's text button has no body and no `data-press`. A press look for it is D15's to design.

### F-buttons-06 — under reduce motion a press shows nothing — reported (shared: `surface/BodyHeat.kt`, the clock)

- **What.** With the clock stopped, the heat never rises: a press on a filled or outlined button changes no pixel
  (`core/press-*-reduce.diff.txt`, `sample/reduce.diff.txt`: at most 1 pixel). The action still fires on release.
- **The design.** This is the engine's own paused mode: `frame` stops `this.time`, from which the heat is computed. The
  handoff says toggles switch instantly under reduce motion. Whether a press should show at once (heat to 1 without its
  320 ms) is a look the design does not settle.

### F-buttons-07 — drops fall onto the sample's captions — reported (the sample)

- **What.** A drop torn from a filled button's drip falls past the caption gap onto the caption under the cell:
  - «filled» at 2208, 3168 and 3360 ms;
  - beside «loading» at 3168 and 3360 ms.
- **Frames.** `sample/kopiya-0.png`.
- **Cause.** The sample's `Cell` keeps its caption clear of the hanging drips ("the caption stands clear of the drips
  the body hangs below its slot"). Drops fall 9–11 dp past the overhang (research §1.15, B-25). Not the button's: its
  drops are the generator's.

## Looked at and found right

- **Press and release.** Of the filled and the outlined button in both palettes, at density 2, `steppedMotion` and in
  the browser's way: the heat rises and cools on the design's times, and the drips stand while hot. The outlined
  button's word turns dark on amber and back as the fill passes (B-91), with no frame of a light word on amber. The
  hover simmer cools when the mouse leaves.
- **States.** Loading's seek bands, Success's clean frame and check, Disabled's no signal, in both palettes. Keyboard
  focus is kept through Loading and Success with its brackets (B-57, B-62), and Tab reaches the outlined and the text
  button.
- **The demo's label changes** at a fixed key: «Listen»/«Pause» at a weighted width keeps its body. «Erase n» and the
  sort button follow their words; a new width regenerates the body (D17).
- **No body back from the pool with a stale look** in the desktop's way. In the browser's way only the stand-in of
  F-buttons-03.
- **«Чистый сигнал»** changes nothing on a button: the bodies are unchanged and only the Tape goes. This is as built:
  the switch reaches the Tape, the keyframe loss and the cocoon.

## Fixes

*(filled in as they land)*
