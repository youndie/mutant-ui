---
id: B-192
title: "The text button and the snackbar action show a press"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-192 — The text button and the snackbar action show a press

The text button and the snackbar's action have no pressed look (F-buttons-05, F-snackbar-04). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 15 ([решение]).

- **The decision and its reason.** Pressed, the label drops 1 dp and a solid 2 dp line in the label's colour stands under it; on the text button it replaces the tracking divider. Release is one frame.

- AC:
  - pressed: the label's ink 1 dp lower and a 2 dp line in its colour under it, no tracking divider; released: the rest picture on the next frame
  - under reduce motion the same, at once
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantButton.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSnackbar.kt`

## Result

### 2026-11-24

- **Looked at first**: throwaway contact sheets (`/private/tmp/b192/before/`, `/private/tmp/b192/after1/`, not in the
  repository) of a text button and a snackbar with its action, in «Эфир» and «Копия», at density 1 and 2, with the clock
  running and under reduce motion: rest, the frame after the press, 160 ms into it, the frame after the release, 160 ms
  after. Before, no frame of either changed on the press but for the focus brackets a finger's press draws on the action
  in a test window. After, both words stand 1 dp lower on the pressed frames, with the line under them, and the frame
  after the release is the rest picture.
- **The change** (`components/MutantButton.kt`, `components/MutantSnackbar.kt`), read from each control's own
  `collectIsPressedAsState()` where it is placed and drawn, so a press recomposes nothing:
  - the word drops `PRESS_DROP` (1 dp, a whole pixel by `offset`, no layer) inside its clean zone: the zone stays where
    it is at rest, so a body's mosaic under the word does not move with the press;
  - the text button: its tracking divider's slot draws a solid line in the label's ink (`inkOnBody`, as the label's)
    instead of the divider while pressed;
  - the snackbar's action: a solid 2 dp line in the word's colour, as wide as the word's box, 5 dp under the word's line
    box, in whole pixels (designed here, D15: the answer says «под ней»; 5 dp is the text button's gap between its label
    and its divider). It stands at its rest place; only the word drops. It is drawn over the bar's body, outside the clean
    zone, so the body does not change on the press.
  - No transition: the look follows the press in its frame, under reduce motion too, as D24's finished frame.
- **B-189's hold: not taken.** Answer 11 holds a *body's heat* 100 ms inside a scroll («Для тел внутри прокручиваемого
  контейнера»); these two have no body and answer 15 names no hold. Their look follows Compose's press interaction,
  which Android's `clickable` holds inside a scroll for the tap timeout and the desktop, iOS and the browser show on the
  down (F-192-01).
- **Tests** (`TextButtonsPressedTest`, desktop, both themes; every reading over an area: the ink's mean row over the
  word's box, the share of the line's 2 dp band in the word's ink, the count of pixels that differ from the rest frame):
  - `the text button pressed drops its label over a solid line, and is at rest the frame after` (AC 1), at density 1 and
    2, pressed through its own source: the ink's mean row 1.0 and 2.0 px lower on the pressed frame and 300 ms in (bound
    ±0.3 around 1 dp); the band 100 % in the label's ink (bound 90 %), none of it in the divider's blue in «Эфир» (bound
    2 %), against 0 % («Эфир») and 70–72 % («Копия», where the divider's blue is the label's and its gaps show) at rest
    (bound 80 %); released, 0 pixels differ from the rest frame against 537–2 121 pressed;
  - `the text button under reduce motion shows the same at once` (AC 2), density 2;
  - `the snackbar's action pressed drops its word over a solid line, and is at rest the frame after` (AC 1), by a finger
    (`performTouchInput` down and up on the word), at density 1 and 2, the bar's body running: the same readings, the
    band 0 % at rest and 100 % pressed, 0 pixels differing after the release against 456–1 498 pressed;
  - `the snackbar's action under reduce motion shows the same at once` (AC 2), density 2.
- **Before the change** (`main`'s `MutantButton.kt` and `MutantSnackbar.kt` in place), all four fail: `it dropped 0.0`
  on the first pressed frame of each.
- **Mutants**, after `b184a49b`, each against its aimed test, read from the JUnit XML, the file copied back with
  `git checkout` and `git status` clean of code after each:
  - the text button's label not dropped (`inner = Modifier`): killed, `the text button pressed …` (`it dropped 0.0`);
  - the text button's divider drawn while pressed (no solid line): killed, `the text button pressed …` (`0.00` of the
    band in the ink);
  - the snackbar's line never drawn: killed, `the snackbar's action pressed …` (`0.00`);
  - the snackbar's word not dropped: killed, `the snackbar's action under reduce motion …` (`it dropped 0.0`);
  - the snackbar's line drawn at rest too: killed, `the snackbar's action under reduce motion …` (`at rest there is no
    solid line; 1.00`);
  - a drop of 2 dp: killed, `the text button under reduce motion …` (`it dropped 4.0`).
- **Goldens and parity cells**: none moved. No golden or parity cell presses either control, and at rest nothing is
  drawn or placed differently (the drop is an offset of 0, the line is not drawn). `ButtonParityTest`,
  `ButtonBehaviourTest`, `ButtonWidestStateTest`, `SnackbarParityTest`, `SnackbarBehaviourTest`, `SnackbarFocusBackTest`
  and the QA sweeps pass unchanged.
- **Gates on this Mac**: `./gradlew check` green (642 tests in `mutant-core`'s desktop suite, 50 in the sample's, 30 each
  on wasm and the iOS simulator, none failed, read from the JUnit XML; `checkKotlinAbi` unchanged, as everything added is
  internal); `./gradlew :mutant-core:compileKotlinIosArm64` green; `make check` green; `make docs-against
  BASE=origin/main` clean.
- **Not verified**: a phone, a real window and a real browser (none used); Android's own hold of the press inside a
  scroll, which is Compose's and only compiled here.

## Findings

- **F-192-01, a scroll started on a text button shows its press off Android.** The look follows Compose's press, which
  the desktop, iOS and the browser show on a finger's down inside a scroll too, so a finger that starts a scroll on a
  text button in a scrolling list shows the drop and the line until the scroll takes the pointer past the slop. B-189's
  hold is a body's heat (answer 11), and these have no body; if the word's press should wait too, that is a question for
  the designer. Read from the code and Compose's behaviour, not seen in a test.
- **F-192-02, a keyframe loss over a pressed text button.** The text button's tracking divider is a body of the loss's
  scene (B-198), and the scene takes its drawing from the divider's own draw; the pressed line is drawn instead of it,
  so a loss that starts while the button is held shows the divider in its band. Read from the code, not seen.
