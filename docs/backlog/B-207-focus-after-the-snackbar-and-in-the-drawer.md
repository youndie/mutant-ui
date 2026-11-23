---
id: B-207
title: "Focus returns after the snackbar and cycles in the drawer"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-207 — Focus returns after the snackbar and cycles in the drawer

After the snackbar's action focus lands nowhere (F-snackbar-05); the drawer's Tab stops at its last row, unlike the dialog's (F-drawer-10). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 40 ([ошибка]; the clock's accent is B-196).

- **The decision and its reason.** After the snackbar's action focus returns to where it was before. In an open drawer Tab cycles through its rows, and Escape closes it.

- AC:
  - focus a button, open a snackbar, take its action: the button is focused again (the button named by the snackbar's new `returnFocusTo`: Compose cannot read where focus stood, see the Result)
  - Tab from the drawer's last row reaches its first; Shift+Tab from the first reaches the last; Escape closes the drawer
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSnackbar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationDrawer.kt`

## Result

### 2026-11-23

- **Looked at first**: a throwaway sheet (`/private/tmp/b207/`, not in the repository) of a button over a snackbar and of
  an open three-row drawer, in «Эфир» and «Копия» at densities 1 and 2, on a running clock and under reduce motion, the
  focused node named under each frame. Before: Tab reaches the snackbar's action and draws its brackets, Enter takes it,
  and from the next frame no node is focused, through the ride and after it (F-snackbar-05). In the drawer Tab steps the
  rows and the fourth Tab stays on the last (F-drawer-10); Shift+Tab from the first row stops on the drawer itself, which
  shows nothing. Escape already closed the drawer (B-34, `DrawerBehaviourTest`), through its `onDismissRequest`.
- **The snackbar** (answer 40): Compose has no public way to read where focus stood before the action took it, so
  `MutantSnackbar` takes `returnFocusTo: FocusRequester? = null`, as `MutantDialog` and `MutantNavigationDrawer` do (the
  answer settles where focus goes; the requester is how a consumer names that place). The bar never takes focus itself.
  If its action holds focus as the bar starts to leave, focus goes to `returnFocusTo` in that same frame — the action is
  disabled from then on, which is what cleared focus before. Whether the action held it is read in composition
  (`SnackbarFocusBack`), where the change of `visible` is seen before the disabled action drops its focus. So:
  - the action taken from the keyboard: back to the requester's node at once, and there after the ride;
  - the bar hidden by its holder while its action holds focus (a timeout): the same, as focus would otherwise go nowhere
    the same way. The answer speaks of the action only; this is its case of "where it was before";
  - the action never held focus: nothing moves;
  - no `returnFocusTo`: focus left to the window, which clears it, as before; the KDoc says so;
  - the requester's node left the composition: Compose's `requestFocus` on a requester attached to nothing prints its
    `FocusRelatedWarning` to stdout and does nothing, so no node is focused, as without one, and nothing throws;
  - a bar removed from the composition instead of hidden returns nothing; the KDoc's way out is `visible`.
  The sample's «Try it» names its «Показать уведомление» button as the requester (`ComponentsCommunication.kt`).
- **The drawer** (answer 40): its focus group wraps as the dialog's does (B-57): `focusProperties { onExit }` sends Tab
  past the last row to the first and Shift+Tab out of the drawer itself (as it opens) to the last, and Shift+Tab on the
  first row, which would stop on the drawer itself, goes to the last row from the row's own key handler, where the
  dialog's checks `atFirst()`. Escape is unchanged: the drawer's `onDismissRequest`.
- **Tests**:
  - `SnackbarFocusBackTest`, `focus a button, open the snackbar, take its action from the keyboard - the button is
    focused again` (AC 1): the button focused in the frame after Enter and 400 ms later, one focused node;
  - `SnackbarFocusBackTest`, `a snackbar whose action never held focus moves nothing as it leaves`: another button keeps
    focus through the leaving;
  - `SnackbarFocusBackTest`, `a requester on nothing composed any more leaves no node focused, and nothing throws`;
  - `DrawerBehaviourTest`, `B-207 Tab steps the rows and comes round from the last to the first` (AC 2): six Tabs from
    the open drawer focus rows 0, 1, 2, 3, 0, 1;
  - `DrawerBehaviourTest`, `B-207 Shift+Tab from the first row reaches the last, and Escape closes the drawer` (AC 2):
    Shift+Tab from the drawer itself and from the first row reach the last row, the next reaches the one above, Escape
    calls the close callback once.
- **Before the change**: with `main`'s `MutantNavigationDrawer.kt` both drawer tests fail. The snackbar tests do not
  compile on `main` (`returnFocusTo` is new); the first is held by mutant m1, which is `main`'s behaviour with the new
  parameter. The other two hold what must not change and pass on `main`'s behaviour too, by design; m2 and m3 hold them.
- **Mutants**, after `8ebae2a`, each read from the JUnit XML of the aimed class, restored with `git checkout`, `git
  status` clean after each:
  - m1 the effect never calls `returnFocusTo` (`main`'s behaviour): killed, `… the button is focused again`;
  - m2 armed on every leaving (`armed = true`): killed, `… never held focus moves nothing …`;
  - m3 a fallback to the next focusable when the requester answers nothing: killed, `a requester on nothing composed …`;
  - m4 `FocusDirection.Next -> null` in the drawer's `onExit`: killed, `B-207 Tab steps the rows …`;
  - m5 the first row's Shift+Tab clause dead (`i == -1`): killed, `B-207 Shift+Tab …`;
  - m6 `FocusDirection.Previous -> null` in the drawer's `onExit`: killed, `B-207 Shift+Tab …`.
- **Goldens and parity cells**: none moved; the change is in focus and keys only, and draws nothing new. The public API
  grows by `MutantSnackbar(returnFocusTo)` (ABI dumps updated).
- **Not verified**: a real window, a phone and a browser were not used; on Android and iOS the keyboard path was not run.
  A mouse click on the action that focuses it (desktop focuses on press) also returns focus to `returnFocusTo`, harmless
  as the brackets show only under keyboard input; not tested.

## Findings

- 2026-11-23: in the first full `./gradlew check` on this branch, the sample's `AccessibilityTest` (`active words hold 4_5
  to 1 after the Tape …`) failed with `UncompletedCoroutinesError: After waiting for 1m`. It ran alongside the
  mutant-core suite. Alone it passed in 55.4 s, so it sits 5 s under `runTest`'s one-minute timeout, and a loaded
  machine pushes it over. It does not open the snackbar's «Try it» this item touches. Not fixed here: out of scope.
