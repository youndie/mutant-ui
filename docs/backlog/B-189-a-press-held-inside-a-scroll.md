---
id: B-189
title: "A press inside a scroll heats after 100 ms"
status: done
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-189 — A press inside a scroll heats after 100 ms

A finger that starts a scroll on a body boils it: off Android, Compose's `clickable` presses on the finger's down (F-card-cocoon-01). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 11 ([решение]).

- **The decision and its reason.** A body inside a scrollable container starts its heat 100 ms after the down, on every platform. If the gesture becomes a scroll, the heat never starts; heat already started is removed by the ordinary 480 ms cooling. Outside a scroll there is no delay.
- Inside a scroll is read as Compose reads it for Android's press delay (`isInScrollableContainer`); the item checks that this works in common code. *It does not (2026-11-24): in CMP 1.12.1 `isInScrollableContainer` is gone, and what replaced it is `internal` to foundation. The surface reads a lazy layout's item or a layout showing less than its content instead (Result).*

- AC:
  - a finger held still on a card in a list heats it from 100 ms; one that moves past the slop within 100 ms never heats it
  - a body outside any scroll heats on the down, as now
  - on desktop, Android, iOS and wasm alike (desktop and wasm by test, the others by compile)
  - each new test of what changes fails on the code before; the tests that guard what stays (outside a scroll, a mouse) hold before and after, as the code before already did both; each kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at *(rewritten 2026-11-24: a test of behaviour the code already had cannot fail before it)*
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyHeat.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`

## Result

### 2026-11-24

- **Looked at first**: throwaway contact sheets (`/private/tmp/b189/sheets/`, not in the repository) of eight cards in a
  `verticalScroll` column and in a `LazyColumn`, and one card outside any scroll: a finger held still, and a finger moved
  10 dp a frame for six frames; «Эфир» and «Копия» at density 1 and 2, the scroll cases also under reduce motion. Before:
  held still, the card warmed from the first frame; scrolled, it was amber from 48 to 144 ms while the list moved, then
  cooled; under reduce motion a scroll's first frames showed the card full amber (B-190's still press). After: held
  still in a scroll, the first change at 112 ms (the first frame past 100); scrolled, no frame warms, under reduce motion
  too; outside a scroll, warm from the first frame as before.
- **Whether `isInScrollableContainer` works in common code: no.** Checked at its source, the CMP 1.12.1 jar
  `org.jetbrains.compose.foundation:foundation-desktop:1.12.1` (`javap` of `AbstractClickableNode`, `ClickableKt`,
  `GestureNodeKt`, `Clickable_skikoKt`, `Clickable_desktopKt`) and the AndroidX `foundation-android:1.12.1` AAR
  (`Clickable_androidKt`). `isInScrollableContainer` no longer exists. The clickable holds its press when
  `delayPressInteraction()` is true: `hasWaitingParent()` (an ancestor `GestureConnection`, a drag gesture node such as a
  scrollable's, in the `Waiting` state, found by `traverseAncestorGestures`) or `isComposeRootInScrollableContainer()`
  (`false` on Skiko); it waits `TapIndicationDelay`, which is `ViewConfiguration.getTapTimeout()` on Android and 0 on
  the desktop (the Skiko targets share it). All of these are `internal`: a common-code compile against
  `GestureConnection`, `GestureState` and `traverseAncestorGestures` failed with `INVISIBLE_REFERENCE`, as did one against
  `requireSemanticsInfo`. Scrollables take no mouse (`AbstractScrollableNodeKt.CanDragCalculation` refuses
  `PointerType.Mouse`), and a key's press is never delayed (`onKeyEvent` does not ask `delayPressInteraction`).
- **The change** (`surface/BodyHeat.kt`, `surface/MutantSurface.kt`, an `actual` per platform): the relay that turns a
  surface's interactions into heat holds a press for `HEAT_HOLD_IN_SCROLL_MS` (100 ms, a named constant beside the heat,
  D24) when `MutantPressHold.holds()`: a finger is down on the body (any pointer but a mouse, read in the pointer's first
  pass by a node that consumes nothing and shares the pointer with siblings), the body stands inside a scroll, and
  Compose does not already hold it (`composeHoldsPressInScroll`, `true` on Android only, where a second hold would make
  200 ms). A cancel before 100 ms (the scroll took the pointer) drops the held press: the heat never starts. A release
  before 100 ms drops it too, as Compose shows a quick tap on Android (press and release together, a ramp of 0 ms): a
  tap shorter than 100 ms in a scroll shows no heat (this repository's reading; the answer names only the hold and the
  scroll). The hold is a `delay` on the coroutine's clock, as Compose's own; the heat then starts on the screen's clock
  (D6). Under reduce motion the held press shows B-190's still frame from 100 ms. Outside a scroll, for a mouse and for
  a key, nothing changed.
- **Inside a scroll**, read from what is public (research D24): an item of a lazy layout (`LocalPinnableContainer`, which
  every lazy list, grid and pager item provides), or an ancestor layout showing less than its content along an axis
  (`verticalScroll`, `horizontalScroll`, the sheet's scrolling content, the dialog's and the menu's scrolling bodies,
  the tabs' row, once their content overflows; field-ui B-93's reading). A container whose content fits cannot scroll,
  and its press is not held. Rejected: the internal API through a suppressed `INVISIBLE_REFERENCE` (unspecified under
  K2, silent on a stack bump); a marker the consumer puts on each scroll.
- **Platforms**: one common relay. The desktop by `PressHeldInScrollTest` and `PressHoldTest`; the browser and the iOS
  simulator by `PressHoldTest` in `./gradlew check` (`wasmJsBrowserTest` through Karma headless, `iosSimulatorArm64Test`,
  30 tests each, none failed); Android and iOS by compile (`./gradlew check` builds Android; `compileKotlinIosArm64`).
- **Tests**:
  - `PressHeldInScrollTest` (desktop, real path: `performTouchInput` and `performMouseInput` on a `MutantCard` in a
    300 × 240 dp stage, the heat read as the number of stage pixels warmed from the card's fill towards the amber — rest
    67 to 235, unheated held 155 to 615, a boil 17 600 to 76 000, against bounds of 1 500 and 6 000):
    - a finger held still on a card in a `verticalScroll` column and in a `LazyColumn`, at density 1 and 2: cold in each
      of the first four frames, boiling at 260 ms (AC 1);
    - a finger moved 60 px up over five frames in the column and in the lazy list: the list scrolls, and no frame of the
      next 600 ms warms the card (AC 1);
    - a tap lifted at 64 ms in the column: no frame of the next 400 ms warms it (the reading above);
    - a card outside any scroll warms within 80 ms of the down (AC 2), and a mouse's press in the column too (Compose's
      reading of the mouse);
  - `PressHoldTest` (common: desktop, wasm, iOS simulator): the hold is 100 ms and Compose holds nothing itself off
    Android; a layout taller or wider than its viewport is a scroll, one that fits is not; a finger is held in a scroll
    or a lazy item, a mouse or a key and a finger outside are not (AC 3).
- **Before the change** (`main`'s `BodyHeat.kt` and `MutantSurface.kt`, the `actual`s and `PressHoldTest` moved aside),
  five of `PressHeldInScrollTest`'s seven fail: both held-still tests (`[17616, 17616, 17616, 17616]` in the first 64 ms),
  both scrolls (`max 17616`), the early lift (`max 18363`). The outside and the mouse tests pass there, as the code
  before already heated both on the down (AC 4 rewritten for it). `PressHoldTest` does not compile there.
- **Mutants**, after `e61cd06f`, each against its test, read from the JUnit XML, the file copied back and `git status`
  clean after each:
  - no layout reading (`inLazyItem` only): killed, the column held still (`[17616, …]`);
  - no lazy reading: killed, the lazy list held still;
  - a cancel that keeps the held press: killed, the column's scroll (`max 12049`), the lazy one's too;
  - a release that keeps it: killed, the early lift (`max 19414`);
  - a hold whatever the scroll: killed, outside a scroll (`188`);
  - a mouse taken for a finger: killed, the mouse (`62`);
  - Compose holding on the desktop: killed, `PressHoldTest`'s first;
  - a layout that fits read as a scroll (`>=`): killed, `everything fits`;
  - a press held with no finger down: killed, `no finger down: a mouse or a key`.
- **Goldens and parity cells**: none moved; they press nothing. B-182's, B-183's and B-184's tests (`SliderGlideTest`,
  `PanelDragTest`, `DrawerBehaviourTest`, `SheetScrollTest` and the rest) pass unchanged.
- **Gates on this Mac**: `./gradlew check` green (638 tests in `mutant-core`'s desktop suite, 50 in the sample's, 30 each
  on wasm and the iOS simulator, none failed, read from the JUnit XML; `checkKotlinAbi` unchanged, as everything added
  is internal); `./gradlew :mutant-core:compileKotlinIosArm64` green; `make check` green; `make docs-against
  BASE=origin/main` clean.
- **Not verified**: a phone, a real window and a real browser (none used); Android's own hold, which is Compose's and only
  compiled here.

## Findings

- **F-189-01, a popup inside a lazy item.** `LocalPinnableContainer` is a composition local, so a popup composed from
  inside a lazy list's item (a menu opened from a row) reads as inside a scroll, and a finger's press on its rows waits
  100 ms. Not seen in a test; Compose's own reading would not cross into the popup's window.
- **F-189-02, a squeezed control.** A layout that reports past its slot on purpose (the icon button under 48 dp, B-89's
  squeezed forms) reads as a layout showing less than its content, so a finger's press on it waits 100 ms although no
  scroll is there.
- **F-189-03, a tap shorter than 100 ms in a scroll shows no heat**, on every platform now as on Android before: the
  answer names only the hold. If a quick tap should still warm the body, that is a question for the designer.
