---
id: B-181
title: "A body loses its signal in three steps and tunes in when enabled"
status: done
priority: P2
size: M
stage: stage-6-later
blocked_by: [B-179]
---

# B-181 — A body loses its signal in three steps and tunes in when enabled

Enabled ↔ disabled is a one-frame cut on every body: the drips shorten or lengthen at once (F-icon-fab-04, F-text-field-08, F-search-06). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 3 ([решение]).

- **The decision and its reason.** Disabling takes 300 ms in three steps: a tear band runs over the body, the fill desaturates in steps, and on the third step the slime is replaced by «нет сигнала» (`BodyState.Disabled`, the handoff's README line 83). Enabling is `tune-in` (B-179). Focus in and out stay instant: catching the signal reads as a click.
- The tear band is not drawn by the design: its height and travel are chosen here from the disabled look's own band (14 % of the height, shifted 4–7 dp) and listed in this item when built (D15).
- Rejected: a cross-fade (research D24).

- AC:
  - a frame strip of a button disabled at 0 ms shows three distinct pictures at the steps and «нет сигнала» from 300 ms; enabling shows B-179's steps
  - focus in and out still change in one frame
  - under reduce motion the finished frame shows at once (research D24)
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyStates.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyTransitions.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`

## Result, 2026-11-24

Built in the surface, so every body with a disabled state takes it (the button, the icon button, the card, the chip, the checkbox, the switch, the slider, the segmented button, the list item, the text field):

- **`MutantSignal`** (`surface/BodyTransitions.kt`) follows a body's `BodyState` from composition without writing state, as `MutantTune` does. A change into `Disabled` starts the loss at that moment on the screen's clock; `step(now)` answers 0, 1 or 2 for 300 ms (`SIGNAL_LOSS_MS`, `SIGNAL_LOSS_STEPS`), then nothing. Composed disabled, a body stands disabled.
- **The loss** (`MutantSurface`): on the first two steps the body still draws what it showed before (its slime, a loading body's bands, an error's blocks; a Success frame stays a clean frame), its fill a third and then two thirds of the way to the disabled fill (`disabledFill`, factored out of `drawDisabledBody`), with the tear band over it; the body is drawn by paths while it loses its signal, never from an image built ahead or the browser's shader. On the third step it is «нет сигнала» with the band still over it (`drawDisabledBody(…, tearTop)`); from 300 ms «нет сигнала» alone. The body is redrawn on every frame while the loss runs, and reads nothing of the clock once it is over.
- **Enabling** is B-179's tune-in, not a second one: the surface keeps a `MutantTune` of its own, which `restartFor` tunes in over the body's own slot whenever `MutantSignal` counts an enabling, the words with the body on the third step. A loss started while that tune-in runs takes over from the frame it starts.
- **Focus** is unchanged: the calm and the brackets switch in one frame, in and out.
- Under reduce motion and on a frozen, paused or held clock (`tuneStands`), the finished frame shows at once both ways. The steps ignore `steppedMotion`, as B-179's do.
- Research D24 records the decision.

**Numbers chosen here (D15)**, the design draws no tear band:

| | |
|---|---|
| the steps | 100 ms each (300 ms in three, the answer's) |
| the fill | ⅓ and ⅔ of the way from the body's fill to the disabled fill (`mix`, no lift), whole from the third step |
| the band's height | the disabled look's own: 14 % of the slot's height, rounded, at least 4 dp (`disabledBandHeight`) |
| the band's shift | the disabled look's own: 4–7 dp right, the stream `vhs`'s second number for the body's seed (`disabledShiftOf`) |
| what the band shows | the body drawn again in it, shifted, under the dropout's veil at 0.22, its clip 20 dp past the slot's sides, as the disabled look's band; drawn behind the content, so no word is torn |
| the band's travel | it rolls up: its top at the slot's bottom (step 1), its middle (step 2), its top (step 3), `oy + round((h − band) · (2 − step) / 2)` |

The travel was first built rolling down. Its third step then stood on the disabled look's own band, which keeps below the label, and the third step's picture was nearly the finished one (looked at in the sheets), so the AC's "three distinct pictures … «нет сигнала» from 300 ms" did not read. Rolling up, the third step's band stands at the top, away from the look's own.

**Frames looked at** (throwaway sheets, deleted): the filled button «Записать» in «Эфир» at density 1, «Копия» at 2 and «Эфир» under reduce motion, every frame from the one before the change to 368 ms after disabling and 368 ms after enabling. Before: «нет сигнала» in the change's frame and the slime back in the enabling's frame. After: 0–96 ms the slime a third desaturated with the band at its bottom, 112–192 ms two thirds with the band across its middle (behind the label), 208–288 ms «нет сигнала» with the band at its top, from 304 ms «нет сигнала» still; enabling, the 16 dp mosaic for 7 frames, the 8 dp for 7, the body and its word from 224 ms. Under reduce motion each change finished in its own frame.

**Tests** (`mutant-core/src/desktopTest/…/surface/SignalLossTest.kt`, «Эфир» at density 1 and «Копия» at 2 for each; every reading counts pixels over the slot with bounds far from its values):

| AC | Test |
|---|---|
| a button disabled at 0 ms shows three distinct pictures at the steps and «нет сигнала» from 300 ms | «a button disabled loses its signal in three steps and stands at no signal from 300 ms»: the first step's fill over > 20 % of the slot (measured 67–68 %) and neither the rest's nor the second step's (0 px) at 0–96 ms; the second step's at 112–192 ms (70–71 %) and no disabled fill; the disabled fill from 208 ms; the third step still frame to frame and different from the finished look by > 60·d² px (784 and 631 measured), more than 80 % of it in the slot's top half (all of it measured); the finished look still from 304 ms and identical to the button composed disabled (0 px) |
| enabling shows B-179's steps | «a button enabled tunes in with the tune-in's steps»: 0–208 ms more than 90 % of the slot in the mosaic's paints (98 %), no outline or word ink, 16 dp cells one colour at 0–96 ms (all of them), 8 dp at 112–208 and the 16 dp grid broken (none); from 224 ms the outline and the fill |
| focus in and out still change in one frame | «focus in and out still change in one frame»: the brackets whole in focus's first frame (160 and 640 px) and unchanged after it, the body whole (outlined) from that frame; gone in focus out's first frame, the body whole |
| under reduce motion the finished frame shows at once | «under reduce motion the finished frame shows at once»: disabled, every frame from the change's is the button composed disabled; enabled, every frame from the change's is the finished, outlined body |

On the code before (the surface from `origin/main`, the new helpers kept so the test compiles): the loss test fails («a third desaturated at 0 ms»: «нет сигнала» in the change's frame) and the enabling test fails (the slime whole at 0 ms). The focus and reduce-motion tests pass on it by construction, as both were already one-frame changes; they are held by mutants M4 and M5.

**Mutants**, each after the commit, the aimed test named among the failures in the JUnit XML, the file restored by copy and `git status` clean after each:

| Mutant | Killed by |
|---|---|
| M1 no tear band (`tearTop = null`) | the loss test: «the third step differs from the finished look: 0 px» |
| M2 the band rolling down (`· step` for `· (2 − step)`) | the loss test: «the third step's band at the slot's top: 0 of 199» |
| M3 no tune-in on enabling (`restartFor` dropped) | the enabling test: the slime at 0 ms, 5 603 of 7 200 px in the mosaic's paints |
| M4 a tune-in on focus too (`restartFor(enablings to focused, …)`) | the focus test: no frame with the brackets (the mosaic hides them) |
| M5 the loss and the tune-in not standing (`tuneStill = false`) | the reduce-motion test: ««нет сигнала» in the change's frame» |

**Goldens and parity cells:** none moved. The fixtures draw on a frozen clock and compose their states directly, where nothing runs; `drawDisabledBody` draws what it drew without a `tearTop`. No departure: a change in live behaviour only.

**The ABI:** unchanged (`checkKotlinAbi` green; everything new is internal).

**Gates on this Mac** (2026-11-24, the head of the code): `./gradlew check --continue` green: the desktop suite 608 tests in 165 classes (every parity class among them), the sample's 50 in 17, the iOS simulator's 27, wasm's 27, ktlint and the ABI check; `./gradlew :mutant-core:compileKotlinIosArm64` green; `make check` and `make docs-against BASE=origin/main` clean.

**Not verified:** a phone, the browser and iOS in a real window (no device and no real browser in this pass); the browser's shader path, which a losing body leaves for paths (desktop draws by paths anyway); the browser's stand-in (B-177), which draws a body's state before its first build and so shows «нет сигнала» at once for a body disabled before it was ever built.

**Findings, not changed here:** what a component switches itself on `enabled` still switches in the change's frame: the outlined button's accent stroke and misregistered shadow (so its first two steps show the plain ink outline), and the labels that grey when disabled (the text button's, the field's). The answer names the body's steps only.
