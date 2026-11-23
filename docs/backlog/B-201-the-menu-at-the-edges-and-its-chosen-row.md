---
id: B-201
title: "The menu opens upward near the foot, scrolls when tall, and answers its chosen row"
status: done
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-201 — The menu opens upward near the foot, scrolls when tall, and answers its chosen row

Near the window's foot the menu slides up over its anchor (Compose's popup clamping); a menu taller than the window loses its first rows and does not scroll (F-menu-06, F-menu-07). A press on the chosen row passes nothing to `onSelect`, so the demo's sort menu cannot be closed by picking the current order (F-menu-08). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 28 ([решение]) and 39 ([ошибка]).

- **The decision and its reason.** Near the foot the menu opens upward from its anchor. Taller than the window, it is limited to the window less 16 dp at the top and 34 dp at the foot (16 and the 18 dp overhang), and its content scrolls. A press on the chosen row calls `onSelect`; nothing pours, and the plate gets a heat impulse of 0.8.

- AC:
  - an anchor in the window's lower part opens the menu above it, never over the anchor
  - a menu of 30 rows in a 400 dp window stands 16 dp from the top and 34 dp from the foot and scrolls to its last row
  - a press on the chosen row calls `onSelect` with it, and the plate's heat reads 0.8 with no pour
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantMenu.kt`

## Result

### 2026-11-23

- **Looked at first**: a throwaway sheet (`/private/tmp/b201/`, not in the repository) of a dropdown in a 400 dp window —
  its anchor at 330 dp, 30 rows under an anchor at 20 dp, and 4 rows with room — in «Эфир» and «Копия» at densities 1 and
  2 under reduce motion, and on a running clock. Before: the menu stood on its anchor at the foot (F-menu-06), and the
  30 rows ran off the window's top with no scroll (F-menu-07).
- **Where the menu stands** (answer 28): `MutantMenu`'s popup places itself (`MenuPlacement`). Under its anchor at
  `offset`, as before, while it fits above the window's foot margin; otherwise on the side with more room, which is above
  whenever the menu fits there: its foot 18 dp above the anchor's top, the gap its drips hang in (the overhang the answer's
  34 counts), never over the anchor. Held inside 16 dp at the top and 34 dp at the foot. Across, `offset` from the
  anchor's start (mirrored right to left, as Compose's own placement), moved in to fit the window, as Compose did.
- **A menu taller than the window** (answer 28): the popup's body is held to the window less 50 dp (`menuWithinWindow`),
  so it stands 16 dp from the top and 34 dp from the foot, and its rows scroll in a `verticalScroll` column inside the body.
  The focused row is brought into view by the focus itself: the chosen row as the menu opens, each row the arrows reach,
  the first after wrapping. Designed here (D15), as the answer does not say: the rows scroll to the body's box, under its
  wavy outline, not to its 6 dp inset; and the words' clean zone is where they stand with the column unscrolled, cut to
  the body, so the body is built once for its first screenful and not rebuilt as the rows scroll. A menu that fits draws
  exactly as before: the sheet of 4 rows with room is byte-identical to `main`'s, both palettes, densities 1 and 2.
- **The chosen row** (answer 39): a press, Enter or Space on it hands its index to `onSelect`; the index does not change,
  so nothing pours; the window gets a heat impulse of 0.8 and cools from it in 384 ms at the release's rate. The impulse is
  `HeatMachine.impulse`, new and shared (research D24): the slider's arrow step (B-182, answer 4) can take it. Under reduce
  motion it does nothing: the window as it was is the impulse's finished frame (D24). The KDoc said the opposite since
  F-menu-08; it now says this.
- **Tests**, `MenuEdgesAndChosenRowTest`:
  - an anchor at 330 dp in a 400 dp window: the menu's foot exactly 18 dp above the anchor's top, the whole 156 dp menu,
    at the anchor's start; densities 1 and 2 (AC 1). Its control: with room, the menu still stands 50 dp under the anchor's
    top;
  - 30 rows under an anchor at 20 dp in a 400 dp window: the menu from 16 dp to 366 dp at densities 1 and 2; Down through
    every row and round to the first, each focused row inside the menu's bounds (AC 2);
  - a press on the chosen row of a running menu: `onSelect` gets its index, the window's heat reads exactly 0.8 in the
    press's frame and 0 a second later, no lime outside the chosen row in 12 frames after it (0 in each), and the window
    drawn hot: 6 590 of its row's 7 344 pixels change against the frame before, against 167 with the boil not drawn (AC 3).
- **Before the change**, the placement tests fail (`the menu's foot 18 dp above the anchor … expected 312 but was 400`;
  the 30 rows `expected 16 but was 50`, mutant m4 below being the old `Popup(offset = …)` line); the chosen-row test does
  not compile there (`plate` is new) and is held by mutant m7, the old `if (index != selectedIndex)` guard.
- **Mutants**, after `c206257`/`4eddeca`, each against `MenuEdgesAndChosenRowTest`, read from its JUnit XML, restored with
  `git checkout`, `git status` clean after each:
  - m1 `val up = false` (never above): killed, the foot test (`expected 312 but was 366`);
  - m2 no gap above the anchor: killed, the foot test (`expected 312 but was 330`);
  - m3 `offset.y / 2` under the anchor: killed, the room test (`expected 90 but was 65`);
  - m4 the old `Popup(offset = dy)`: killed, the foot and 30-row tests;
  - m5 no `menuWithinWindow()`: killed, the 30-row test (`expected 16 but was 0`);
  - m6 no `verticalScroll`: killed, the 30-row test (`ПУНКТ 2` not in view);
  - m7 the old guard `if (index != selectedIndex) onPick()`: killed, the chosen-row test (`expected [1] but was []`);
  - m8 no impulse: killed, the chosen-row test (heat `0.0`);
  - m9 the surface ignoring `heatMachine`: killed, the chosen-row test (167 pixels changed);
  - m10 `impulse` not landing the heat: killed, the chosen-row test (heat `0.0`).
- **Goldens and parity cells**: none moved. `MenuParityTest` and the design goldens draw `MenuBody` without the scrolling
  column; the popup's menu that fits is byte-identical to before.
- **Found on the way** (not changed):
  - a menu taller than the window covers its anchor, as nothing else can hold it; a press outside or Escape closes it;
  - the drips' falling drops reach a little past the 18 dp overhang, beside a narrow anchor at its top's level (looked at
    in the sheet; the anchor's chip stays clear);
  - in the scratch sheet, a menu opened by a touch press and stepped by Down showed no focus brackets on the focused row
    (the test window's input mode, probably, as B-168 saw for the mouse); not followed.
- **Gate**: see the pull request. **Not verified**: a phone, iOS and a real browser; a real window's popup (Compose
  Desktop's test window only); right to left.
