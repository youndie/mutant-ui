---
id: B-185
title: "The dialog rises opaque and keeps a margin when tall"
status: done
priority: P1
size: M
stage: stage-6-later
blocked_by: [B-179]
---

# B-185 — The dialog rises opaque and keeps a margin when tall

The dialog's window fades with its scrim as one alpha (B-28), so for 200 ms the screen's words read through its text (F-dialog-05). A dialog taller than the screen stands flush, its outline clipped and its last line cut (F-dialog-07). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 7 ([ошибка]) and 29 ([решение]).

- **The decision and its reason.** The window is opaque from its first frame and enters by `tune-in` (B-179). The scrim keeps its 200 ms. A tall dialog stands 24 dp from the window's edges; its title and buttons are pinned and its content scrolls.

- AC:
  - no frame of the opening shows the screen through the window's pixels; the window follows B-179's steps while the scrim takes 200 ms
  - a dialog taller than the window stands 24 dp from every edge, its title and buttons in view, its content scrolling
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDialog.kt`

## Result, 2026-11-24

In `MutantDialog.kt`: the window leaves the scrim's alpha layer and enters through `MutantTuned` (B-179) on the dialog's own clock, with its own `MutantTune`; `MutantTuned` takes `deafLeaving` (default `true`, as before), which the dialog sets `false`.


- **Leaving, decided here (D15, consistent with B-179).** The window steps out as every body leaves: the 8 dp mosaic to 80 ms, then nothing; the scrim and the band take their own 200 ms each way, so for the last 40 ms only the lightening scrim stands, and the dialog leaves the composition at 200 ms as before. Its actions go disabled and its keys stay with it from the close's first frame, as since B-168; the window's semantics stay through the close (it is not deafened as `MutantAppear`'s leaving content is), so a reader still finds the dialog while its scrim covers the screen.
- **The scrim's 200 ms each way.** The answer keeps the scrim's 200 ms; until now the open took the brief's `pause`, 320 ms, for scrim, band and window together, and the close `play`'s 200. The scrim and the band now darken over 200 ms and lighten over 200; the window's own 320 ms are the `tune-in` token's. Under `steppedMotion` the scrim steps at the hot tick (D22); the window's steps keep to the clock either way, a picture sequence (B-179).
- **The margin.** A centred window stands in a box padded 24 dp on every side, so a window that fits stands exactly where it stood (the padding is symmetric) and a tall one keeps 24 dp from the top and the foot. A fixture's window placed by `windowTop` keeps no vertical padding, so the parity stills stand where they stood.
- **No new clip.** The text's scroll is B-59's: `weight(1f, fill = false)` and a `verticalScroll`, the title and the actions measured first. B-59 measured that a fitting dialog's capture is byte-identical with and without the scroll container, so it stays unconditional; this item adds no clip.
- Rejected: the window's own fade over the open's first part (F-dialog-05's second option): an alpha ramp, which D24 rules out; a `MutantAppear` around the window, which would clear its semantics and take its keys while it leaves, against the dialog's own closing rules.

**Tests** (`mutant-core/src/desktopTest/…/components/DialogOpaqueAndTallTest.kt`; "through" counts the window's inside pixels that differ between two renders over a red and a green screen):

| AC | Test |
|---|---|
| no frame of the opening shows the screen through the window; B-179's steps; the scrim 200 ms each way | «the window rises opaque by tune-in and steps out while the scrim takes 200 ms each way» («Эфир» density 1, «Копия» 2): through ≤ 0.1 % of the inside in every frame of the open and the close's first 80 ms; the 16 dp mosaic to 96 ms, the 8 dp to 208, the words from 224; leaving, no word from the first frame; the scrim full from 208 ms and clear from 208 ms of the close |
| the same for a dialog composed open | «composed open, the window tunes in opaque from its first frame too»: through ≤ 0.1 % in every frame, no word in the first five, the words from the fifteenth |
| a tall dialog stands 24 dp from every edge, title and actions in view, text scrolling | «a dialog taller than the screen stands 24 dp from every edge, title and actions in view, text scrolling» |
| reduce motion | «under reduce motion the window and the scrim stand finished in the first frame and leave at once» |

On `origin/main`'s implementation the first three fail (the screen through the window, 34 472 px at 0 ms; the tall window at 0 dp from the top); the reduce-motion one passes by construction, as the dialog already stood finished there. `DialogBehaviourTest`'s first test is renamed for the scrim's 200 ms; its readings are unchanged. Goldens and parity cells at rest are unchanged: `./gradlew check` green with none re-recorded.

**Mutants**, each after its commit, the aimed test named among the failures in the JUnit XML, the file restored and `git status` clean after each:

| Mutant | Killed by |
|---|---|
| M1 the window back in the scrim's alpha (the root box's `graphicsLayer`) | «the window rises opaque …»: 34 472 px through at 0 ms |
| M2 the window whole from its first frame (`play = false`) | «composed open, …»: 351 glyph px at frame 0 |
| M3 the scrim over the old 320 ms | «the window rises opaque …»: the scrim not full from 208 ms |
| M4 the host not told of the close (`leaving = false`) | «the window rises opaque …»: 351 glyph px leaving at 0 ms |
| M5 no margin above and below (`vertical = 0.dp`) | «a dialog taller than the screen …»: 0 dp from the top |
| M6 the tune never stands (`still = false`) | «under reduce motion …»: not gone in the frame after the close |

M2 survived the first three tests (a dialog opened after composition never reads `play`); the fourth was added for it.
