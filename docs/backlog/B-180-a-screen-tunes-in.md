---
id: B-180
title: "A new screen tunes in as the switch starts"
status: done
priority: P2
size: M
stage: stage-6-later
blocked_by: [B-179]
---

# B-180 — A new screen tunes in as the switch starts

The screen's content swaps in one frame while the navigation pill, the tabs' bar or a chip moves for 330 ms (F-screens-08, F-app-bar-tabs-04, F-app-bar-tabs-07). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 2 ([решение]).

- **The decision and its reason.** The old content goes in one frame; the new tunes in (B-179, 320 ms) starting on the same frame as the pill, the bar or the chip. No slide and no cross-dissolve.
- The kit offers the host (a content slot keyed by the screen), so a consumer gets the same swap as the demo; the sample's screens use it. Rejected: building it in the sample only, which leaves every consumer the one-frame cut.
- Rejected: the brief's `switch` (a 200 ms vertical tear and a noise band), which the answer replaces.

- AC:
  - in the demo, switching a tab, a navigation destination or a filter chip: the old screen's last frame is the one before the switch, and the new screen's tune-in begins on the frame the pill, bar or chip starts to move
  - nothing slides and no frame mixes the two screens
  - under reduce motion the finished frame shows at once (research D24)
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyTransitions.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationBar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTabs.kt`, `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/DemoApp.kt`

## Result, 2026-11-24

**`MutantScreen(screen, modifier, content)`**, public (`surface/MutantScreen.kt`): a content slot keyed by the screen. A change of `screen` takes the old content out of the composition at once and composes the new one through B-179's host (`MutantAppear`), so the new screen's steps count from the composition that sees the pick — the one in which the navigation bar's pill, the tabs' bar or the chip's pour starts for it. Composed for the first time it tunes in too, as any body appearing does: a tab composed only while shown tunes in on every visit. The control that picks the screen stays outside it, and the KDoc says so: everything inside tunes in. Under reduce motion and on a standing clock the new screen stands finished at once (B-179's `tuneStands`). Research D24 records the decision.

- **Nested hosts** (`BodyTransitions.kt`, `MutantAppear.kt`, `MutantSurface.kt`): a body registers with every tuning host around it (`MutantTuneBodies(parent)`), not only the nearest. A host inside another that tunes in with it draws into the outer one's empty clip, so without this the demo's cards (a `MutantAppear` inside the filter's `MutantScreen`, inside the tab's) showed nothing for 213 ms while every other body showed its mosaic. A body in a popup is skipped by a host in another window (`findRootCoordinates`), so a host never measures across windows at draw time.
- **The demo** (`sample/…/Demo*.kt`): each tab (Library, Queue, Search, Settings) and the opened recording put their content in `MutantScreen`; the navigation bar stays outside, so its pill runs while the screen tunes in. The Library's FAB is the screen's own and tunes in with it. The recording's page under its tabs is `MutantScreen(tab)`, and the Library's filtered cards and shelf are `MutantScreen(chosen)`. Content without a body (the notes' text, the linear progress, the cocoon's own picture) shows nothing until the third step, as B-179 says of content without a body. The catalogue names `MutantScreen` beside the navigation bar; the README lists it.
- Rejected: one host around the demo's whole tab with an exemption for the navigation bar. Each tab draws the bar over its content and under its own sheets and dialogs (B-96), so the bar cannot leave the tab; an exemption would be a second public API for one demo's layering, while a consumer's bar normally stands outside its content slot.
- The AC's "the frame the chip starts to move": a chip's pour starts in the pick's composition, but its front shows in the chip some frames later. The kit's test holds it by the chip's frames being identical, frame by frame, with and without a screen to swap; the pill and the bar are held where they stand (at their start in the pick's frame, moved the frame after).

**Frames looked at** (throwaway sheets, deleted): the demo's navigation (On air → Queue), the recording's tabs (Tracks → Notes) and the Library's chip (Recordings → Noise), every frame from the one before the click to 384 ms, in «Эфир» at density 1, «Копия» at density 2 and «Эфир» under reduce motion. Before: the new screen whole in the click's frame, while the pill stood at its old cell in that frame and moved from the next. After: the old screen gone in the click's frame, the 16 dp mosaic for 7 frames, the 8 dp for 7, the words from 224 ms, the pill and the bar running from the same frame; under reduce motion the new screen finished in the click's frame. At density 2 the test window squeezes the phone to half its height; the screen above the bar was read there.

**Tests**

| AC | Test |
|---|---|
| switching a destination, a tab or a filter chip: the old screen's last frame is the one before; the tune-in begins on the frame the pill, bar or chip starts to move | kit, `mutant-core/src/desktopTest/…/surface/ScreenTunesInTest.kt`: «a new screen tunes in as the navigation bar's pill starts for it» («Эфир» 1, «Копия» 2), «… as the tabs' bar starts for it» («Копия» 1, «Эфир» 2), «… as the chip starts to pour, the bodies appearing inside it with it» (both): the old card whole before, gone from the pick's frame; 16 dp blocks (> 95 % of cells one colour) at 0–96 ms, 8 dp at 112–208, no glyph and no outline before 224 ms, the new words from 224; the pill's or bar's middle within 1.5 px of its start in the pick's frame and moved 12–38 px the frame after; the chip's frames identical to its frames with no screen. Demo, `sample/src/desktopTest/…/DemoScreenTunesInTest.kt`: the same three switches in the phone by real clicks (both palettes for the tabs and the chip): the old screen's words gone from the click's frame, the new words under a tenth of their count before 224 ms and drawn from it, the pill and the tabs' bar at their start in the click's frame and moved the frame after |
| nothing slides and no frame mixes the two screens | `ScreenTunesInTest`: in every frame to 208 ms > 97 % of what is drawn is the mosaic's paints (no word or outline of either screen), and each step's picture stands still frame after frame (under 50 px × d² changed) |
| under reduce motion the finished frame shows at once | «under reduce motion the new screen stands finished at once» (all three pickers) and the demo's «under reduce motion each switch shows the new screen finished at once» |

On the code before, the kit's tests do not compile (`MutantScreen` did not exist); the demo's three switch tests fail on the new words in the click's frame (606, 169 and 735 px). Both reduce-motion tests pass on the code before by construction, as the swap was already a cut; they are held by mutant M4.

**Mutants**, each after the commit, the aimed test named among the failures in the JUnit XML, the file restored by copy and `git status` clean after each:

| Mutant | Killed by |
|---|---|
| M1 `MutantScreen` without `key(screen)` | «… navigation bar's pill …»: 1 090 glyph px at 0 ms |
| M2 a nested host not registering with the outer (`MutantTuneBodies()`) | «… as the chip starts to pour …»: no mosaic at 0 ms (0 px) |
| M3 the tune counting from a frame late (`now + 16`) | «… as the tabs' bar starts for it»: still the 16 dp mosaic at 112 ms |
| M4 the tune not standing under reduce motion (`tuneStands` without the theme's switch and the pause; without the pause alone the mutant is equivalent, as reduce motion pauses the clock) | «under reduce motion …»: no words in the pick's frame |
| M5 the Queue tab outside `MutantScreen` (`run`) | demo «a destination on the navigation bar …»: 606 px at 0 ms |
| M6 the recording's page on one key (`MutantScreen(0)`) | demo «a tab of the recording …»: 169 px at 0 ms |
| M7 the Library's filter on one key | demo «a filter chip …»: 735 px at 0 ms |

**Goldens and parity cells:** none moved; the fixtures draw on a frozen clock, where a tune shows its end at once. No departure: a change in live behaviour only.

**The ABI:** `MutantScreen` is the one addition to the three dumps; reviewed and recorded with `updateKotlinAbi`.

**Gates on this Mac** (2026-11-24, the head of the code): `./gradlew check --continue` green: the desktop suite 604 tests in 164 classes (every parity class among them), the sample's 50 in 17, the iOS simulator's 27, wasm's 27, ktlint and the ABI check; `./gradlew :mutant-core:compileKotlinIosArm64` green; `make check` and `make docs-against BASE=origin/main` clean.

**Not verified:** a phone, the browser and iOS in a real window (no device and no real browser in this pass); the demo at density 2, where the test window squeezes the phone (the kit's test covers density 2).

**Findings, not changed here:** the Components page's group chips (F-screens-08's second case) still swap in one frame; the Demo page's screen chip now tunes the phone's screen in, as its `key(screen)` composes each tab anew, but its navigation bar appears whole. B-198's keyframe loss still takes its screen key from the consumer: the loss stands around the whole screen, outside `MutantScreen`, so the host cannot hand it its key (research D24).
