---
id: B-179
title: "Bodies tune in as they appear and step out as they leave"
status: done
priority: P2
size: L
stage: stage-6-later
blocked_by: []
---

# B-179 — Bodies tune in as they appear and step out as they leave

Cards, empty states, the tooltip, the cocoon handing over to its content, the search results and the snackbar's words appear and leave in one frame (F-card-cocoon-03, F-search-05, F-empty-state-05, F-snackbar-06, F-pull-to-refresh-04 in `logs/b-168/`). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 1 ([решение]), settle it.

- **The decision and its reason.** `tune-in`: 320 ms in three steps — a 16 dp mosaic over the slot, an 8 dp mosaic, the finished body. Words and icons appear only on the third step. Leaving is two steps over 160 ms: the 8 dp mosaic, then nothing. The badge appears in one frame: it is an on-screen caption that switches on. The snackbar's words appear on the third step of its body's tune-in. It is built once in the surface, so the screen swap (B-180), switching on (B-181) and the dialog (B-185) use the same steps.
- The steps are the token's `stepped(3)` over 320 ms, whatever `steppedMotion` says: they are a picture sequence, not a movement (D22).
- **How the leaving content stays composed: a host, `MutantAppear(visible)`** (research D24). It keeps its content composed for 160 ms after `visible` turns false, deaf to pointer, keys and readers. Every `MutantSurface` inside registers its slot with the host, which draws the slots' mosaics while it tunes and the content (bodies, words, icons) from the third step. A component that keeps its own visibility tunes its own body: the tooltip's plate in its popup, the snackbar through `MutantSurface(tune = …)`. Rejected: each component tuning in on its own first composition — a lazy list's cards would tune in again on every scroll into view, every screen visit would tune every body, and leaving would still need something to hold the content. A component the consumer takes straight out of the composition cannot step out; the KDoc says so.
- **Numbers designed here (D15):** the mosaic's blocks are laid from the slot's origin and clipped to its corner; half of them take the body's fill, a quarter each the cocoon's two paints (`cocoon1`, `cocoon2`), from the stream `rng(<cassette>/<key>, tune<side>)`, fixed for the step. The steps count on the screen's clock from the composition that saw the change: the 16 dp mosaic to 106.7 ms, the 8 dp one to 213.3 ms; leaving, the 8 dp mosaic to 80 ms, nothing to 160 ms. A clock held by a dialog, paused or frozen shows the end at once, as reduce motion does. A turn back half-way goes on from the picture shown (leaving's 8 dp mosaic tunes in from the 8 dp step).
- **The snackbar** keeps B-26's ride: its body tunes in while it rides in (240 ms), its words on the third step, and steps out while it rides down; it stays composed, drawing nothing after 80 ms, until the 200 ms ride ends and `onHidden` is called. A new `message` while it stands tunes it in again, its new words on the third step (F-snackbar-06's cut): this repository's reading of «слова снекбара появляются на третьем шаге tune-in его тела».
- **Not changed:** the pull-to-refresh word (F-pull-to-refresh-04) is a word inside a standing body, not a body appearing; the answer names no step for it, and it stays as it is.
- Rejected: an alpha ramp (no smooth opacity, research D24); the brief's 16 → 8 → 4 → clean with a 40 ms cascade, which the answer replaces.
- Not covered: a whole screen's swap (B-180).

- AC:
  - for each listed component, a frame strip at 0, 107, 213 and 320 ms shows the 16 dp mosaic, the 8 dp mosaic and the finished body, and no glyph or icon pixel before the third step; leaving shows the 8 dp mosaic, then nothing, by 160 ms (read on the test clock's 16 ms frames at 0–96, 112–208 and 224–320 ms: the third step starts at 213.3 ms, so a frame at 213 still shows the 8 dp mosaic; the cocoon's hand-over and the search results take the place of a loading picture, which comes back in one frame, so they have no leaving)
  - the badge's first frame is its finished picture
  - under reduce motion the finished frame shows at once (research D24)
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at (rewritten 2026-11-24: the badge's criterion says nothing changes, so its guard passes on the code before by construction; it is held by the mutant that makes the badge tune in)
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyTransitions.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCard.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantEmptyState.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTooltip.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCocoon.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSearchBar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSnackbar.kt`

## Result, 2026-11-24

Built once in the surface (`surface/BodyTransitions.kt`, `surface/MutantAppear.kt`, `surface/MutantSurface.kt`), for B-180, B-181 and B-185 to take:

- **`MutantTune`** follows a body's visibility from composition without writing state, as `MeltFollower` does, and answers what it shows at a moment (`MutantTuneLook`: the 16 dp mosaic, the 8 dp mosaic, whole, nothing), whether a run is under way and whether the body has left. `restartFor(picture)` tunes a body that stays shown in again for a new picture. Under reduce motion, and on a frozen, paused or held clock (`tuneStands`), it shows the end at once.
- **`MutantAppear(visible, modifier, content)`**, public: the host (research D24 says why a host, and what was rejected). Inside it every `MutantSurface` registers its slot (`LocalMutantTuneBodies`, `MutantTunedBody`); while it tunes, the host draws each slot's mosaic (`drawTuneMosaics`), the larger first, and the content into an empty clip, so its bodies are built by the third step; then the content whole. Leaving, the content stays composed for 160 ms with its semantics cleared, its pointer events consumed and its keys swallowed, then leaves the composition.
- **`MutantTuned(tune, …)`**, internal: the same host for a component that keeps its own visibility. **`MutantSurface(tune = …)`**, internal: one body tuning its own slot, its content with it.
- **Applied:** the tooltip (its plate tunes in its popup, which stays composed while the plate steps out), the snackbar (its body through `MutantSurface(tune = …)`, kept minimal while #137 (B-207) changed the same file: four lines and a parameter; rebased onto it, its focus return is left as it is), and through `MutantAppear` the card, the empty state, the search result and the cocoon's hand-over, each with a KDoc line saying how, and that a component taken straight out of the composition cannot step out. The badge is untouched: it switches on in one frame, and its KDoc says so. In the sample, the Library's empty state and its cards tune in as they take each other's place, and each search result in the Find tab tunes in as it arrives (`key` by the tape, so a result that stays does not tune in again); the catalogue names `MutantAppear` in the cocoon's section.

**Frames looked at** (throwaway sheets, deleted): the card «Эфир» at density 1, «Копия» at 2, and under reduce motion; the empty state, a search result over its loading row («Копия»), the tooltip (density 2) and the snackbar, every frame from the change to 384 ms and 224 ms of leaving. Before: each whole in its first frame and gone in one. After: the 16 dp mosaic for 7 frames, the 8 dp for 7, the body with its words from 224 ms; leaving, the 8 dp mosaic for 5 frames, then nothing. One defect seen and fixed: on the riding snackbar, blocks drawn one by one at a fractional offset left hairline seams between neighbours of one paint; each paint is now one path over a base of the fill.

**Tests** (`mutant-core/src/desktopTest/…/surface/BodiesTuneInTest.kt`; every reading counts pixels over the stage or over whole 16 or 8 dp cells, 2 px in from their edges, the blended edges aside, never a few edge pixels):

| AC | Test |
|---|---|
| card: the strip 0–96 / 112–208 / 224–320 ms; leaving | «a card tunes in and steps out» («Эфир» density 1, «Копия» 2): 0 glyph px and 0 outline px before 224 ms, > 97 % of the drawn pixels the mosaic's paints, > 95 % of 16 dp cells one colour, then > 95 % of 8 dp cells and < 80 % of 16 dp ones; from 224 ms the words and the outline; leaving the 8 dp mosaic to 64 ms, nothing drawn 80–144 ms |
| the cocoon's hand-over | «a cocoon hands over to its card» («Копия»): the same strip from the cocoon's last frame |
| empty state | «an empty state tunes in and steps out» (both themes, density 1 and 2): the strip on its several bodies, without the one-grid reading |
| search results | «a search result tunes in from its loading row and steps out» (density 2) |
| tooltip | «the tooltip tunes in and steps out» (both themes, density 1 and 2), read above its anchor |
| snackbar | «the snackbar tunes in as it rides in, its words on the third step, and steps out» (both themes, density 1 and 2); it leaves the composition with its 200 ms ride; «a new message in a standing snackbar tunes it in again, the new words on the third step» |
| leaving keeps content composed 160 ms, then removes it | «a leaving content stays composed for its 160 ms, then leaves the composition» |
| the badge's first frame is its finished picture | «the badge switches on whole in its first frame»: 0 px between its first frame and 320 ms later |
| under reduce motion | «under reduce motion the finished frame shows at once and leaving removes at once»: the card, the empty state, the tooltip and the snackbar |

On the code before, every new test fails to compile (`MutantAppear` did not exist), and the tooltip's and snackbar's assertions fail on their words in the first frame; the badge's passes by construction (the rewritten AC). `SnackbarBehaviourTest`'s ride reading takes the bar's top 116 ms in from its 8 dp mosaic now, the slot's own top: its range widened from 10–24 to 5–24 (the ride is unchanged; the reading was the outline's first full row).

**Mutants**, each after the commit, the aimed test named among the failures in the JUnit XML, the file restored with
`git checkout` and `git status` clean after each:

| Mutant | Killed by |
|---|---|
| the first step's blocks 8 dp (`TUNE_COARSE_DP = 8`) | «a card tunes in …»: 16 dp cells one colour 0.0 at 0 ms |
| the body whole from the second step (`TUNE_IN_MS / TUNE_IN_STEPS`) | «a cocoon hands over …»: 352 glyph px at 112 ms |
| no body registers with the host (`bodies -= tuned` for `+=`) | «an empty state …»: no mosaic at 0 ms (0 px). The first try, an `Unit` statement, did not compile under `-Werror` and is not counted |
| the content drawn under the mosaic (no empty clip) | «a search result …»: 1 866 glyph px at 0 ms |
| the tooltip hidden at once (`if (shown)` for the tune) | «the tooltip …»: nothing drawn on leaving's first frame |
| the snackbar's body not tuned (`tune = tune` dropped) | «the snackbar tunes in …»: 227 glyph px at 0 ms |
| reduce motion not standing (`tuneStands` without the theme's switch and the pause) | «under reduce motion …»: the card's words 0 px |
| leaving content gone at once (`now - since >= 0`) | «a leaving content stays composed …»: not composed from the first frame |
| no new tune for a new message (`restartFor` dropped) | «a new message …»: the new words from the first frame |
| the badge tuning in (wrapped in `MutantAppear`) | «the badge switches on whole …»: 4 324 px between its first frame and 320 ms |

**Goldens and parity cells:** none moved. The parity tests and fixtures draw on a frozen clock, where a tune shows its end at once, and nothing at rest changed: `./gradlew check`'s desktop suite ran every parity class green. No entry in the departures registry: a change in live behaviour only.

**The ABI:** `MutantAppear` is new; the dump also renames six `ComposableSingletons` lambdas whose hashes moved with the surrounding code.

**Gates on this Mac** (2026-11-24, the head of the code): `./gradlew check --continue` green, rebased onto #136 and #137: the desktop suite 584 tests in 162 classes (every parity class among them; 574 in 160 before the rebase, green too), the sample's 40 in 16, the iOS simulator's 27, wasm's 27, ktlint and the ABI check; `./gradlew :mutant-core:compileKotlinIosArm64` green; `make check` green. A first `./gradlew check` under a load average above 200 (other agents' builds on the Mac) timed out six tests at their one-minute limit (the sample's `AccessibilityTest` and two of `ComponentsPageTest`, the core's `DisabledContrastTest`, `MenuEdgesAndChosenRowTest`, one cell of `LongContentSweepTest`), none of them touching this change, and found one real failure, fixed: the sample's catalogue did not name `MutantAppear`. The second run, the one reported, had none.

**Not verified:** a phone, the browser and iOS in a real window (no device and no real browser in this pass); the browser's first-build stand-in at the third step, where a body built between frames could draw its stand-in for a frame (the content is drawn into an empty clip during the mosaics so that it is built by then, which the desktop's waiting first frame cannot show); Android below 12 is unaffected (no effect layers).
