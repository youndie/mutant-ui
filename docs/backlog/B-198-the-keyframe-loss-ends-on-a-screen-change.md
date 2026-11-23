---
id: B-198
title: "The keyframe loss ends on a screen change and takes the dividers"
status: done
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-198 — The keyframe loss ends on a screen change and takes the dividers

The keyframe loss keeps dragging the picture it took when it started after the screen under it changed — after a tab switch, a chosen chip without its body for 1.08 s — and leaves the dividers out of its picture (F-screens-01, F-foundations-05, F-list-divider-badge-02). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 24 ([решение]; the dividers [ошибка]).

- **The decision and its reason.** On a screen change the loss ends at once, and no new one starts for 1 s after it. The dividers are in its picture.
- The kit learns of a screen change from the screen host (B-180) or a key the consumer passes; the item decides which. A scroll is not a screen change, and the answer does not settle it: it stays as it is, and says so.
- **Decided here: a key the consumer passes.** `MutantKeyframeLoss(…, screen: Any? = null, content)`: the value names the screen the content shows (a destination, a tab, an item opened over them), compared by `equals`; on a change the scene ends a running loss at once, its recovery not played, and starts none until 1 s of the clock's time past the change (`KEYFRAME_LOSS_QUIET_MS`, beside the `KEYFRAME_LOSS_*` tokens). The change is taken in a `SideEffect`, after the composition that shows the new screen applies and before its frame draws, so the new screen's bodies draw whole on that frame. Left null, the loss runs by its clock alone, as before. B-180's host, once built, passes its own destination here. The sample's Library phone passes `DemoNav.screen`: the tab, or the tape opened over it with the recording's tab (hoisted from the recording into `DemoNav.pages` so the loss hears it).
  - *Rejected: inferring the change from the content*, by the scene's bodies leaving and arriving. A row inserted, an overlay opened, a list item recycled by a `LazyColumn` on a scroll and a tab switch all look alike from there; the answer names a screen change, which only the consumer knows. *Rejected too: waiting for B-180*, which would leave the defect in every consumer without the host.
- **A scroll is not a screen change** (the answer does not name it): content scrolled under a running loss keeps the band's picture until it recovers, as before (F-screens-01's scroll case stays).
- **The dividers** become bodies of the scene: `TrackingDivider` (`surface/MutantDivider.kt`) registers a `MoshBody` and draws through `moshBehind`, so the snapshot takes their strokes and the band cuts them where it lies, as a surface's body. `MutantDivider` and the text button's line draw through it.
- **Under reduce motion** (D7, D24) the loss does not run, so there is nothing to end: a screen change shows the new screen at once, as it did.

- AC:
  - a tab switch during a loss shows the new screen whole on the next frame, and no loss begins within 1 s
  - a loss over a list shows its dividers in the dragged picture
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/KeyframeLoss.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantDivider.kt`

## Result

**Changes.** `surface/MutantKeyframeLoss.kt`: the `screen` parameter and `MoshScene.follow` (ends the loss, clears the
pieces painted ahead for the old screen, sets the quiet), the quiet in the start condition, `EVERY_MS` and `RECOVER_MS`
read from the `KEYFRAME_LOSS_*` tokens. `surface/MutantDivider.kt`: `TrackingDivider`. `components/MutantDivider.kt`,
`components/MutantButton.kt` (the text button's line): drawn through it. The sample: `DemoNav.screen` and `pages`,
`DemoPage`'s Library phone passing it. The ABI dump records the new parameter (source-compatible; the JVM signature
changed).

**Tests** (desktop):

| AC | Held by |
|---|---|
| a tab switch during a loss shows the new screen whole on the next frame | `KeyframeLossScreenChangeTest` «a tab switch during a loss shows the new screen whole on the next frame»: the loss runs before, not one frame after and not at 4.4 s; 0 of 53 000 px off the same screen drawn with no loss, one frame after (bound 265); on the code before, the loss ran on (`[true, true, true]`). The sample: `DemoNavigationTest` «a tab taken during the phone's keyframe loss shows whole on the next frame»: 0 of 259 200 px off the phone with no loss one frame after Search was taken 3.8 s in |
| … and no loss begins within 1 s | `KeyframeLossScreenChangeTest` «no loss begins within 1 s of a screen change»: switched 300 ms before the window's loss, the loss starts 1 s or more after the switch (and does start within 1.4 s); on the code before, at 448 ms |
| a loss over a list shows its dividers in the dragged picture | `KeyframeLossScreenChangeTest` «a loss over a list drags its dividers», at density 1 and 2: 740 and 2 906 px of dragged strokes off the dividers' rows (bounds 150 and 600), and 131 of 858, 510 of 3 376 px of the dividers at rest still showing in the band (bound half); on the code before, 0 px dragged |

**Mutants**, each after the commit, the aimed test named among the failures in the JUnit XML, the file restored with
`git checkout`:

| Mutant | Killed by |
|---|---|
| a change does not end the loss (`if (plan != null) end()` dropped) | «a tab switch …»: `[true, true, true]` |
| no quiet (`&& t >= quietUntil` dropped) | «no loss begins …»: a loss at 448 ms after the switch |
| the divider not a body of the scene (`rememberMoshBody(null)`) | «a loss over a list …»: 0 px dragged |
| the divider not cut by the band (`moshBehind(null, body)`) | «a loss over a list …»: 858 of 858 px standing whole over the band |
| the sample passes no key (`screen = null`) | `DemoNavigationTest` «a tab taken …»: 18 252 px off |

**Goldens and parity cells:** none moved. At rest the divider draws the same strokes (a `Spacer` with the same drawing
in place of a `Canvas`); `KeyframeLossParityTest` has no divider; the change is in live behaviour only.

**Frames looked at** (throwaway contact sheets, deleted): a tab switch 400 ms into a loss at density 1 and 2 in Эфир and
Копия, before (the old picture dragged over the new chip until 4.85 s) and after (the new screen whole from the first
frame); a switch 300 ms before the loss (the loss starts 1.07 s after it); a list with dividers frozen at 0, 30, 60 and
90 % of its loss in both palettes and densities (the strokes stand whole over the band before, are dragged after); the
sample's Library phone with Search taken during its loss.

**Gates on this Mac:** `make check`, `./gradlew check` (desktop, Android, the iOS simulator's tests, wasm),
`./gradlew :mutant-core:compileKotlinIosArm64`.

**Findings, not fixed here.**

- A body that changes inside a running band without a screen change (F-foundations-05) still shows its old picture
  there until the band recovers; the answer covers a screen change only.
- The scroll case of F-screens-01 stays, by the decision above.

**Not verified.** No phone and no real browser: the browser's way (`BandPixels`, the pieces painted ahead, which a change
now clears) ran only as `./gradlew check` runs wasm; no test reads a change on that path.
