---
id: B-206
title: "A body's default key is its place in the composition"
status: done
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-206 — A body's default key is its place in the composition

Default keys are a constant or the label: unkeyed cards, rows, dividers, radios and dialogs are clones that boil in step, the dialog's window key cannot be set at all, and a play/pause icon button or a running `REC · 0:42:17` tooltip reshapes its body on every change (F-card-cocoon-04, F-icon-fab-03, F-tooltip-04, F-dialog-10). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 38 ([решение]).

- **The decision and its reason.** By default a body's key is its place in the composition (`currentCompositeKeyHash`) and its body type. In lists an explicit key from the item's id is required; the KDoc and the sample say so, as the kit cannot enforce it.
- Fixtures that relied on the label as a key pass it explicitly, so the goldens hold; any that move are listed and looked at.

- AC:
  - two unkeyed cards side by side boil differently; the same card recomposed keeps its shape
  - an icon button whose icon changes and a tooltip whose text runs keep their bodies
  - the dialog's window takes a key
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDialog.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCard.kt`

## Result, 2026-11-25

**Decided** (the designer's answer 38, recorded in research D24). A body's key, when the consumer gives none, is its
body type and its place in the composition: `mutantPlaceKey(type)` (`surface/MutantPlaceKey.kt`, internal) returns
`type@<currentCompositeKeyHashCode in base 36>`, read in the component's default argument, so the place is that of the
component's own call. The item names `currentCompositeKeyHash`; Compose 1.12 has it as the `Int` predecessor of the `Long`
`currentCompositeKeyHashCode`, which this uses.

- **Every public component's `key`** defaults to it, by its body type: `MutantButton` `btn`, `MutantOutlinedButton`
  `obtn`, `MutantTextButton` `tbtn`, `MutantIconButton` `icon`, `MutantFab` `fab`, `MutantExtendedFab` `efab`,
  `MutantChip` `chip`, `MutantCheckbox` `check`, `MutantRadioButton` `radio`, `MutantSwitch` `switch`, `MutantSlider`
  `slider`, `MutantTextField` `field`, `MutantTooltipBox` `tip`, `MutantCard` `card`, `MutantCocoon` `cocoon`,
  `MutantDivider` `div`, `MutantListItem` `tile`, `MutantDataTable` `trow`, `MutantSearchBar` `search`,
  `MutantSearchResult` `sr`, `MutantSearchResultLoading` `srl`, `MutantCarousel` `car`, `MutantMenu`/`MutantDropdown`
  `menu`, `MutantNavigationDrawer` `drawer`, `MutantNavigationBar` `navbar`, `MutantSegmentedButton` `segmented`,
  `MutantPullToRefresh` `ptr`, `MutantSnackbar` `snack`, `MutantBottomSheet` `sheet`, `MutantDatePicker` `cal`,
  `MutantTimePicker` `tp`, `MutantTopAppBar` `topbar`, `MutantDialogAction` `dialog-ok`. Each KDoc says so, and that in a
  list the key comes from the item's id.
- **`MutantDialog(key = …)`**, new, defaulting to `mutantPlaceKey("dialog")`; the window's body was the private constant
  `dialog`. The internal `DialogFrame` keeps `dialog` for the parity fixture.
- **The kit's own calls keep their keys**: the sheet's tear line now passes the `0` it took by default (the page's first
  divider of a stage), and the internal dividers and cocoons already passed theirs.
- **The sample** keys the Settings screen's three radios by their labels (`speed-SP`…), where all three were `radio` and
  boiled as clones, with a line saying a list's item takes its key from what it is (B-168's screens face of
  F-card-cocoon-04). Its other lists were keyed already. The demo's phones pass the keys their bodies had (the top bars'
  `0`, the navigation bar's `navbar`, the tooltips' and the volume slider's words, the snackbar's `snack`, the erase
  dialogs' `dialog` and `dialog-ok`), so a phone draws the same on the Demo page as alone: `DemoTapeOnceTest` compares the
  two and went red by 402 px at one instant, the two being two places. The Components page keeps the defaults, so its
  «open» dialog and its «Try it» stage are now two windows (F-dialog-10).
- Rejected: a KDoc line alone (the clones stay); a key relative to the theme's place, so that a body would be the same
  on every platform: it needs the runtime's private hash formula.

**Where the place holds, and where it was verified.**

| Question | Answer | Where |
|---|---|---|
| recomposition | holds: the same place, the same body | desktop: `DefaultKeysByPlaceTest` (a card recomposed under a new click handler and a branch appearing before it, 0 px); a scratch probe of the hash through a changed argument and a toggled `if` sibling |
| B-180's `MutantScreen` (`key(screen)`) | a screen shown again gives its bodies the places they had (0 px); another screen, other bodies (3 335 px). The place hashes the screen's `hashCode`: a string or a number is the same every run; a value hashed by identity is not (said in `MutantScreen`'s KDoc) | desktop, `DefaultKeysByPlaceTest` |
| `MutantAppear` | a card leaving and appearing again (composed anew at the same place) has its body back, 0 px | desktop, `DefaultKeysByPlaceTest` |
| `movableContentOf` | holds through the move and a recomposition after it (0 px): the runtime hashes moved content by itself, not by where it stands | desktop, `DefaultKeysByPlaceTest` and the scratch probe |
| from run to run | the same numbers in two JVMs | desktop, the scratch probe run twice |
| a plain loop, a lazy list | two rows are two places; a loop's row has its index for a place (a removal above hands its body on), a lazy item its key | the scratch probe (loop, `LazyColumn`, `Popup`, `BoxWithConstraints`: all distinct) |
| across platforms | the runtime hashes one tree alike: a runtime-only composition (no UI host) gave the same eight numbers on the JVM, the iOS simulator and wasm (`iosSimulatorArm64Test`, `wasmJsBrowserTest`, headless). But the place counts the host above the content: on desktop the same call under `runComposeUiTest` and under `ImageComposeScene` had two places. So an unkeyed body is not expected to be the same in a desktop window, a browser and on iOS; a body that must be takes a key of its own (`mutantPlaceKey`'s KDoc) | scratch tests, deleted; no real host of wasm or iOS was run |

**Goldens and parity cells.** None moved. Eight tests went red with the new default, every one a fixture that had taken
the old default, and each now passes it explicitly: `CardParityTest` (`card`), `DialogParityTest` (the primary action's
`dialog-ok`), `BarTabsParityTest` (the top bar's `0`), `SearchBarParityTest` (`MutantSearchResultLoading(key = "")`),
`SheetParityTest` (the screen's divider `0`; the sheet's own tear line, in the kit), `SheetBehaviourTest`'s keyframe
loss case (`sheet`), `CalendarNumbersOnDropTest` (`cal`), and `RightToLeftSweepTest`, whose subjects are composed under
`key(i, dir)`, so a default place differed between the two directions (`QaKit`'s icon button and FAB take their
description, the time picker `tp`, the empty card `card`). In the sample, `DemoTapeOnceTest` (above). No departure, so
nothing in the D20 registry.

**ABI.** `MutantDialog` gains `key: String` before `actions` (the three dumps, recorded with `updateKotlinAbi`): binary
incompatible for that one function, source compatible for every call that passes `actions` as the trailing lambda or by
name. A changed default is not in the dump. The look of every unkeyed body in a consumer's app changes once.

**Tests** (`mutant-core/src/desktopTest/.../components/DefaultKeysByPlaceTest.kt`, density 1, «Эфир», under reduce
motion where the clock stands and a body changes only with its key or size; a body against another counts in thousands
of pixels, against itself in none, so every threshold stands far from both):

| AC | Test |
|---|---|
| two unkeyed cards side by side boil differently; the same card recomposed keeps its shape | «two unkeyed cards side by side are two bodies, and each keeps its own as it recomposes» (3 592 px apart, > 1 000; recomposed 0, < 100) and «a card is its place's body, through a move by movable content, a screen shown again and an appearance» (two places 3 000+ px apart; screen `b` 3 335; `a` again, moved, appeared again 0) |
| an icon button whose icon changes keeps its body | «an icon button whose icon and description change keeps its body»: Play → Close, 214 px in the icon's square, 0 outside it (< 100; 981 with the description as the key) |
| a tooltip whose text runs keeps its body | «a tooltip whose text runs keeps its plate»: `REC · 0:42:17` → `:20`, 62, 10 and 64 px a second (< 400; 1 975…2 293 with the text as the key) |
| the dialog's window takes a key | «the dialog's window takes a key, and two unkeyed dialogs are two windows»: read on the tune-in's first step, where the window's mosaic is drawn from its key: one key 3 px, two keys 19 004, two unkeyed dialogs 23 636 (> 1 000) |
| each new test fails before; goldens and cells unchanged | run on `main`'s code (the changed files from `origin/main`, the tests from the branch, the dialog's `key` argument dropped as `main` has none): all five failed — cards 0 px apart, the screen's card and the movable one 0 px, the icon button's body 981 px, the tooltip 1 975 px, two keys 3 px. `./gradlew check` green with every golden and parity cell |

**Mutants** (after the commit, one line each, the aimed test named in the JUnit XML's `<failure>`, restored with a copy
and `git status` clean):

| Mutant | Killed by |
|---|---|
| `MutantCard`'s default back to `"card"` | «two unkeyed cards…»: 0 px apart |
| `mutantPlaceKey` from `currentRecomposeScope.hashCode()` instead of the place | «a card is its place's body…»: the first screen shown again moved its card 3 351 px |
| `MutantIconButton`'s default back to `contentDescription ?: icon.name` | «an icon button…»: the body moved 981 px |
| `MutantTooltipBox`'s default back to `text` | «a tooltip…»: 0:42:18 changed 1 975 px |
| the dialog's window drawn from `WINDOW_KEY`, not `windowKey` | «the dialog's window…»: two keys 3 px apart |

**Frames looked at** (throwaway contact sheets at density 1 and 2, both palettes, under reduce motion, on this branch and on
`main`): two cards, one body on `main`, two here; the play/stop button's drips jump on `main` and stand here; the
tooltip's plate regenerates every second on `main` and keeps its drips here, only the digits changing; two dialogs' Erase
buttons are one body on `main` and two here, and at rest the clean window barely differs (its key shows in the tune-in's
mosaic, which the test reads).

**Not verified.** No real host of wasm or iOS was run, so that an unkeyed body differs between platforms is inferred from
two desktop hosts, not seen; no phone; B-198's keyframe loss tests ran green in `./gradlew check` (`KeyframeLossScreenChangeTest`).

